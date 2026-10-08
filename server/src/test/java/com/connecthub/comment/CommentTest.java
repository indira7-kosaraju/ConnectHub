package com.connecthub.comment;

import com.connecthub.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentTest extends AuthTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void commentsNestOneLevelAndCountsStayInSync() throws Exception {
        Session owner = register();
        Session member = register();
        String slug = createCircle(owner, "Talkers", false, null);
        join(member, slug);
        String postId = createPost(owner, circleId(slug), "discuss").get("id").asText();

        String top = comment(member, postId, "first!", null);
        String reply = comment(owner, postId, "welcome", top);
        // replying to a reply attaches to the top-level comment
        comment(member, postId, "thanks", reply);

        mvc.perform(get("/api/v1/posts/" + postId + "/comments"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].replies.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].replies[1].parentId").value(top));
        mvc.perform(get("/api/v1/posts/" + postId)).andExpect(jsonPath("$.data.commentCount").value(3));

        mvc.perform(put("/api/v1/comments/" + top + "/reaction").header("Authorization", owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("type", "support"))))
                .andExpect(jsonPath("$.data.vibe.support").value(1.0));

        // Non-member can't comment; a random user can't delete
        mvc.perform(post("/api/v1/posts/" + postId + "/comments").header("Authorization", register().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("content", "hi"))))
                .andExpect(status().isForbidden());

        // Post author deletes the thread: the top comment and both replies go
        mvc.perform(delete("/api/v1/comments/" + top).header("Authorization", owner.bearer())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select comment_count from posts where id = ?::uuid", Integer.class, postId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from comments where post_id = ?::uuid", Integer.class, postId)).isZero();
    }

    private String comment(Session s, String postId, String content, String parentId) throws Exception {
        var body = new java.util.HashMap<String, Object>(Map.of("content", content));
        if (parentId != null) body.put("parentId", parentId);
        String res = mvc.perform(post("/api/v1/posts/" + postId + "/comments").header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).at("/data/id").asText();
    }
}
