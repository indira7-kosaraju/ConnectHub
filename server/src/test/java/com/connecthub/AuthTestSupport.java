package com.connecthub;

import com.connecthub.auth.RefreshCookies;
import com.connecthub.common.RateLimiter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Helpers for tests that need signed-up users. */
public abstract class AuthTestSupport extends AbstractIntegrationTest {

    public static final String PASSWORD = "Password123";

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected RateLimiter rateLimiter;

    public record Session(String accessToken, Cookie refreshCookie, JsonNode user) {
        public String bearer() {
            return "Bearer " + accessToken;
        }
    }

    @BeforeEach
    void resetRateLimits() {
        rateLimiter.reset();
    }

    protected static String uniqueEmail() {
        return "u" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
    }

    protected Session register(String email) throws Exception {
        MockHttpServletResponse res = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("displayName", "Test User", "email", email, "password", PASSWORD))))
                .andReturn().getResponse();
        if (res.getStatus() != 201) {
            throw new IllegalStateException("register failed: " + res.getStatus() + " " + res.getContentAsString());
        }
        return toSession(res);
    }

    protected Session register() throws Exception {
        return register(uniqueEmail());
    }

    protected Session toSession(MockHttpServletResponse res) throws Exception {
        JsonNode data = json.readTree(res.getContentAsString()).get("data");
        return new Session(data.get("accessToken").asText(), res.getCookie(RefreshCookies.NAME), data.get("user"));
    }

    /** Registers a user and gives them a unique username. */
    protected Session registerWithUsername(String prefix) throws Exception {
        Session s = register();
        String username = (prefix + "_" + UUID.randomUUID().toString().substring(0, 6)).toLowerCase();
        mvc.perform(put("/api/v1/me/username").header("Authorization", s.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username", username))));
        return new Session(s.accessToken(), s.refreshCookie(), json.readTree(json.writeValueAsString(Map.of(
                "id", s.user().get("id").asText(), "username", username))));
    }

    protected String firstInterestId() throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/interests")).andReturn().getResponse().getContentAsString())
                .at("/data/0/id").asText();
    }

    /** Creates a circle (in the first interest's category) and returns its slug. */
    protected String createCircle(Session owner, String name, boolean privateCircle, Integer maxMembers) throws Exception {
        return createCircle(owner, name, privateCircle, maxMembers, firstInterestId());
    }

    protected String createCircle(Session owner, String name, boolean privateCircle, Integer maxMembers, String categoryId) throws Exception {
        var body = new java.util.HashMap<String, Object>(Map.of("name", name, "description", "About " + name,
                "categoryId", categoryId, "privateCircle", privateCircle));
        if (maxMembers != null) {
            body.put("maxMembers", maxMembers);
        }
        MockHttpServletResponse res = mvc.perform(post("/api/v1/circles").header("Authorization", owner.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andReturn().getResponse();
        if (res.getStatus() != 201) {
            throw new IllegalStateException("create circle failed: " + res.getStatus() + " " + res.getContentAsString());
        }
        return json.readTree(res.getContentAsString()).at("/data/slug").asText();
    }

    protected String circleId(String slug) throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/circles/" + slug)).andReturn().getResponse().getContentAsString())
                .at("/data/id").asText();
    }

    protected void join(Session s, String slug) throws Exception {
        mvc.perform(post("/api/v1/circles/" + slug + "/join").header("Authorization", s.bearer()));
    }

    /** Creates a text post via the multipart endpoint and returns the post JSON. */
    protected com.fasterxml.jackson.databind.JsonNode createPost(Session s, String circleId, String content) throws Exception {
        var part = new org.springframework.mock.web.MockMultipartFile("post", "", "application/json",
                json.writeValueAsBytes(Map.of("circleId", circleId, "content", content)));
        MockHttpServletResponse res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .multipart("/api/v1/posts").file(part).header("Authorization", s.bearer())).andReturn().getResponse();
        if (res.getStatus() != 201) {
            throw new IllegalStateException("create post failed: " + res.getStatus() + " " + res.getContentAsString());
        }
        return json.readTree(res.getContentAsString()).get("data");
    }
}
