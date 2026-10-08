package com.connecthub.notification;

import com.connecthub.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationTest extends AuthTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void reactionsCommentsAndRepliesNotifyTheRightPeopleOnce() throws Exception {
        Session author = register();
        Session fan = register();
        String slug = createCircle(author, "Notify Me", false, null);
        join(fan, slug);
        String postId = createPost(author, circleId(slug), "ping").get("id").asText();

        react(fan, postId, "spark");
        react(fan, postId, "deep"); // changing a vibe doesn't notify again
        String commentId = json.readTree(mvc.perform(post("/api/v1/posts/" + postId + "/comments").header("Authorization", author.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("content", "my own comment"))))
                .andReturn().getResponse().getContentAsString()).at("/data/id").asText();
        mvc.perform(post("/api/v1/posts/" + postId + "/comments").header("Authorization", fan.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("content", "replying", "parentId", commentId))));

        // author: 1 reaction + 1 reply (no separate "comment" — they're the parent author). Own comment → nothing.
        mvc.perform(get("/api/v1/notifications").header("Authorization", author.bearer()))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].type").value("reply"))
                .andExpect(jsonPath("$.data.items[0].commentSnippet").value("replying"))
                .andExpect(jsonPath("$.data.items[1].type").value("reaction"))
                .andExpect(jsonPath("$.data.items[1].actor.id").value(fan.user().get("id").asText()));
        mvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", author.bearer()))
                .andExpect(jsonPath("$.data.count").value(2));
        mvc.perform(post("/api/v1/notifications/read-all").header("Authorization", author.bearer()))
                .andExpect(jsonPath("$.data.updated").value(2));
        mvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", author.bearer()))
                .andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    void preferencesSilenceATypeAndCircleRequestsReachAdmins() throws Exception {
        Session admin = register();
        Session requester = register();
        String slug = createCircle(admin, "Quiet Club", true, null);
        mvc.perform(post("/api/v1/circles/" + slug + "/join").header("Authorization", requester.bearer()));
        mvc.perform(get("/api/v1/notifications").header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.items[0].type").value("circle_request"))
                .andExpect(jsonPath("$.data.items[0].circleSlug").value(slug));

        mvc.perform(post("/api/v1/circles/" + slug + "/members/" + requester.user().get("id").asText() + "/approve")
                .header("Authorization", admin.bearer()));
        mvc.perform(get("/api/v1/notifications").header("Authorization", requester.bearer()))
                .andExpect(jsonPath("$.data.items[0].type").value("circle_approved"));

        // Turn reactions off for the admin, then react to their post: no notification
        jdbc.update("update users set notification_prefs = jsonb_set(notification_prefs, '{reactions}', 'false') where id = ?::uuid",
                admin.user().get("id").asText());
        String postId = createPost(admin, circleId(slug), "silence").get("id").asText();
        react(requester, postId, "funny");
        mvc.perform(get("/api/v1/notifications").header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.items.length()").value(1)); // still just the circle request
    }

    private void react(Session s, String postId, String type) throws Exception {
        mvc.perform(put("/api/v1/posts/" + postId + "/reaction").header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("type", type))))
                .andExpect(status().isOk());
    }
}
