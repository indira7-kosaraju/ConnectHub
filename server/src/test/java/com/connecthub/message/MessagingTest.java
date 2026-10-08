package com.connecthub.message;

import com.connecthub.AuthTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessagingTest extends AuthTestSupport {

    @Test
    void conversationsOnlyBetweenCircleMatesAndAreUniquePerPair() throws Exception {
        Session a = registerWithUsername("ana");
        Session b = registerWithUsername("ben");
        Session stranger = registerWithUsername("stray");
        String slug = createCircle(a, "Chatters", false, null);
        join(b, slug);

        String c1 = start(a, b).get("id").asText();
        String c2 = start(b, a).get("id").asText();
        org.assertj.core.api.Assertions.assertThat(c2).isEqualTo(c1);

        mvc.perform(post("/api/v1/conversations").header("Authorization", stranger.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("userId", a.user().get("id").asText()))))
                .andExpect(status().isForbidden());
        // Non-participants can't read or write
        mvc.perform(get("/api/v1/conversations/" + c1 + "/messages").header("Authorization", stranger.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void sendListUnreadAndReadReceipts() throws Exception {
        Session a = registerWithUsername("ana");
        Session b = registerWithUsername("ben");
        join(b, createCircle(a, "Pen Pals", false, null));
        String cid = start(a, b).get("id").asText();

        send(a, cid, Map.of("content", "hey <b>there</b>", "clientId", "tmp-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("hey there"))
                .andExpect(jsonPath("$.data.clientId").value("tmp-1"));
        send(a, cid, Map.of("content", "you around?"));

        mvc.perform(get("/api/v1/conversations").header("Authorization", b.bearer()))
                .andExpect(jsonPath("$.data.items[0].id").value(cid))
                .andExpect(jsonPath("$.data.items[0].unreadCount").value(2))
                .andExpect(jsonPath("$.data.items[0].lastMessage.content").value("you around?"));
        mvc.perform(get("/api/v1/conversations/unread-count").header("Authorization", b.bearer()))
                .andExpect(jsonPath("$.data.count").value(1));

        mvc.perform(post("/api/v1/conversations/" + cid + "/read").header("Authorization", b.bearer())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/conversations/unread-count").header("Authorization", b.bearer()))
                .andExpect(jsonPath("$.data.count").value(0));
        mvc.perform(get("/api/v1/conversations/" + cid + "/messages").header("Authorization", a.bearer()))
                .andExpect(jsonPath("$.data.items[0].readAt").isNotEmpty())
                .andExpect(jsonPath("$.data.items.length()").value(2));
        // the sender's own messages never count as unread for them
        mvc.perform(get("/api/v1/conversations/unread-count").header("Authorization", a.bearer()))
                .andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    void sharedPostIsHiddenFromRecipientsWhoCannotReadIt() throws Exception {
        Session a = registerWithUsername("ana");
        Session b = registerWithUsername("ben");
        join(b, createCircle(a, "Open Chat", false, null));
        String secretSlug = createCircle(a, "Secret Garden", true, null);
        String postId = createPost(a, circleId(secretSlug), "for members only").get("id").asText();
        String cid = start(a, b).get("id").asText();

        send(a, cid, Map.of("content", "look at this", "sharedPostId", postId))
                .andExpect(jsonPath("$.data.sharedPost.id").value(postId));
        mvc.perform(get("/api/v1/conversations/" + cid + "/messages").header("Authorization", b.bearer()))
                .andExpect(jsonPath("$.data.items[0].sharedPost").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].sharedPostUnavailable").value(true));
    }

    private JsonNode start(Session from, Session to) throws Exception {
        String body = mvc.perform(post("/api/v1/conversations").header("Authorization", from.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("userId", to.user().get("id").asText()))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("data");
    }

    private org.springframework.test.web.servlet.ResultActions send(Session s, String cid, Map<String, String> body) throws Exception {
        return mvc.perform(post("/api/v1/conversations/" + cid + "/messages").header("Authorization", s.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
}
