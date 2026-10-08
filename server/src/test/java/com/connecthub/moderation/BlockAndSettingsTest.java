package com.connecthub.moderation;

import com.connecthub.AuthTestSupport;
import com.connecthub.auth.RefreshCookies;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BlockAndSettingsTest extends AuthTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void blockingHidesBothWaysAndStopsMessages() throws Exception {
        Session a = registerWithUsername("blocker");
        Session b = registerWithUsername("blocked");
        String slug = createCircle(a, "Shared Space", false, null);
        join(b, slug);
        String bPost = createPost(b, circleId(slug), "b's post").get("id").asText();
        createPost(a, circleId(slug), "a's post");
        String cid = json.readTree(mvc.perform(post("/api/v1/conversations").header("Authorization", a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("userId", b.user().get("id").asText()))))
                .andReturn().getResponse().getContentAsString()).at("/data/id").asText();

        mvc.perform(post("/api/v1/users/" + b.user().get("username").asText() + "/block").header("Authorization", a.bearer()))
                .andExpect(status().isOk());

        // a no longer sees b's content, and b no longer sees a's
        assertThat(feedTexts(a)).doesNotContain("b's post").contains("a's post");
        assertThat(feedTexts(b)).doesNotContain("a's post").contains("b's post");
        mvc.perform(get("/api/v1/posts/" + bPost).header("Authorization", a.bearer())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/users/" + a.user().get("username").asText()).header("Authorization", b.bearer())).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/conversations/" + cid + "/messages").header("Authorization", b.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("content", "hello?"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/search").param("q", "blocker").header("Authorization", b.bearer()))
                .andExpect(jsonPath("$.data.people.length()").value(0));
        mvc.perform(get("/api/v1/me/blocks").header("Authorization", a.bearer()))
                .andExpect(jsonPath("$.data[0].id").value(b.user().get("id").asText()));

        mvc.perform(delete("/api/v1/users/" + b.user().get("username").asText() + "/block").header("Authorization", a.bearer()));
        assertThat(feedTexts(a)).contains("b's post");
    }

    @Test
    void settingsPasswordChangeSignsOutOtherSessionsAndProfanityIsMasked() throws Exception {
        Session s = registerWithUsername("settler");
        mvc.perform(patch("/api/v1/me/settings").header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("privateProfile", true, "notificationPrefs", Map.of("reactions", false)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.privateProfile").value(true))
                .andExpect(jsonPath("$.data.notificationPrefs.reactions").value(false))
                .andExpect(jsonPath("$.data.notificationPrefs.comments").value(true));
        mvc.perform(patch("/api/v1/me/settings").header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("notificationPrefs", Map.of("bogus", true)))))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/v1/me/password").header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("currentPassword", "wrong-one1", "newPassword", "NewPassw0rd"))))
                .andExpect(status().isUnauthorized());
        var res = mvc.perform(put("/api/v1/me/password").header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("currentPassword", PASSWORD, "newPassword", "NewPassw0rd"))))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(res.getCookie(RefreshCookies.NAME)).isNotNull();
        // the old refresh token (other "device") is revoked
        assertThat(mvc.perform(post("/api/v1/auth/refresh").cookie(s.refreshCookie())).andReturn().getResponse().getStatus()).isEqualTo(401);

        String slug = createCircle(s, "Polite Place", false, null);
        assertThat(createPost(s, circleId(slug), "this is shit").get("content").asText()).isEqualTo("this is s***");
    }

    @Test
    void deletingAnAccountHandsOverCirclesAndRemovesData() throws Exception {
        Session owner = register();
        Session member = register();
        String slug = createCircle(owner, "Legacy Circle", false, null);
        join(member, slug);
        createPost(owner, circleId(slug), "goodbye post");

        mvc.perform(delete("/api/v1/me").header("Authorization", owner.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("password", "wrong"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/me").header("Authorization", owner.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("password", PASSWORD))))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select count(*) from users where id = ?::uuid", Integer.class, owner.user().get("id").asText())).isZero();
        mvc.perform(get("/api/v1/circles/" + slug).header("Authorization", member.bearer()))
                .andExpect(jsonPath("$.data.viewer.role").value("admin"))
                .andExpect(jsonPath("$.data.memberCount").value(1));
    }

    private String feedTexts(Session s) throws Exception {
        return mvc.perform(get("/api/v1/feed").header("Authorization", s.bearer())).andReturn().getResponse().getContentAsString();
    }
}
