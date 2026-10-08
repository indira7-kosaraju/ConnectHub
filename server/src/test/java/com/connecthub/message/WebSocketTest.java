package com.connecthub.message;

import com.connecthub.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** End-to-end realtime: real HTTP server, SockJS + STOMP client, JWT on CONNECT. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class WebSocketTest {

    static {
        // Reuse the shared container started by AbstractIntegrationTest.
        AbstractIntegrationTest.POSTGRES.getClass();
    }

    @org.springframework.test.context.DynamicPropertySource
    static void datasource(org.springframework.test.context.DynamicPropertyRegistry r) {
        if (!AbstractIntegrationTest.POSTGRES.isRunning()) AbstractIntegrationTest.POSTGRES.start();
        r.add("spring.datasource.url", AbstractIntegrationTest.POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", AbstractIntegrationTest.POSTGRES::getUsername);
        r.add("spring.datasource.password", AbstractIntegrationTest.POSTGRES::getPassword);
    }

    @LocalServerPort
    int port;
    @Autowired
    TestRestTemplate rest;
    @Autowired
    ObjectMapper json;

    WebSocketStompClient stomp;

    @BeforeEach
    void setUp() {
        stomp = new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(json);
        stomp.setMessageConverter(converter);
    }

    @AfterEach
    void tearDown() {
        stomp.stop();
    }

    @Test
    void connectWithoutTokenIsRejected() {
        var future = stomp.connectAsync("http://localhost:" + port + "/ws", new WebSocketHttpHeaders(), new StompHeaders(),
                new StompSessionHandlerAdapter() {
                });
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
    }

    @Test
    void messagesTypingAndNotificationsArriveInRealtime() throws Exception {
        String tokenA = register();
        String tokenB = register();
        String slug = rest(tokenA, HttpMethod.POST, "/api/v1/circles", Map.of("name", "Live Wire " + UUID.randomUUID().toString().substring(0, 4),
                "categoryId", rest(null, HttpMethod.GET, "/api/v1/interests", null).at("/data/0/id").asText(), "privateCircle", false))
                .at("/data/slug").asText();
        rest(tokenB, HttpMethod.POST, "/api/v1/circles/" + slug + "/join", null);
        String bId = rest(tokenB, HttpMethod.GET, "/api/v1/me", null).at("/data/id").asText();
        String cid = rest(tokenA, HttpMethod.POST, "/api/v1/conversations", Map.of("userId", bId)).at("/data/id").asText();

        StompSession b = connect(tokenB);
        BlockingQueue<JsonNode> messages = subscribe(b, "/user/queue/messages");
        BlockingQueue<JsonNode> typing = subscribe(b, "/user/queue/typing");
        BlockingQueue<JsonNode> notifications = subscribe(b, "/user/queue/notifications");
        Thread.sleep(300); // let subscriptions register

        StompSession a = connect(tokenA);
        a.send("/app/chat.typing", Map.of("conversationId", cid, "typing", true));
        JsonNode t = typing.poll(5, TimeUnit.SECONDS);
        assertThat(t).isNotNull();
        assertThat(t.get("typing").asBoolean()).isTrue();

        a.send("/app/chat.send", Map.of("conversationId", cid, "content", "hello over STOMP", "clientId", "c-1"));
        JsonNode m = messages.poll(5, TimeUnit.SECONDS);
        assertThat(m).isNotNull();
        assertThat(m.get("content").asText()).isEqualTo("hello over STOMP");
        assertThat(m.get("clientId").isNull()).isTrue(); // only echoed to the sender

        JsonNode n = notifications.poll(5, TimeUnit.SECONDS);
        assertThat(n).isNotNull();
        assertThat(n.get("type").asText()).isEqualTo("message");
    }

    private StompSession connect(String token) throws Exception {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + token);
        return stomp.connectAsync("http://localhost:" + port + "/ws", new WebSocketHttpHeaders(), headers,
                new StompSessionHandlerAdapter() {
                }).get(5, TimeUnit.SECONDS);
    }

    private BlockingQueue<JsonNode> subscribe(StompSession session, String destination) {
        BlockingQueue<JsonNode> queue = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return JsonNode.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((JsonNode) payload);
            }
        });
        return queue;
    }

    private String register() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String token = rest(null, HttpMethod.POST, "/api/v1/auth/register",
                Map.of("displayName", "WS", "email", "ws" + suffix + "@test.dev", "password", "Password123")).at("/data/accessToken").asText();
        rest(token, HttpMethod.PUT, "/api/v1/me/username", Map.of("username", "ws_" + suffix));
        return token;
    }

    private JsonNode rest(String token, HttpMethod method, String path, Object body) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.setBearerAuth(token);
        var res = rest.exchange(path, method, new HttpEntity<>(body == null ? null : json.writeValueAsString(body), h), String.class);
        return json.readTree(res.getBody());
    }
}
