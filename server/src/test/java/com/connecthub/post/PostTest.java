package com.connecthub.post;

import com.connecthub.AuthTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostTest extends AuthTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void membersPostWithImagesNonMembersCannot() throws Exception {
        Session owner = register();
        Session outsider = register();
        String slug = createCircle(owner, "Photo Walks", false, null);
        String cid = circleId(slug);

        var meta = new MockMultipartFile("post", "", "application/json",
                json.writeValueAsBytes(Map.of("circleId", cid, "content", "Two frames from today <script>x</script>")));
        mvc.perform(multipart("/api/v1/posts").file(meta).file(image("images")).file(image("images"))
                        .header("Authorization", owner.bearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("Two frames from today"))
                .andExpect(jsonPath("$.data.images.length()").value(2))
                .andExpect(jsonPath("$.data.images[0].width").value(800))
                .andExpect(jsonPath("$.data.editableUntil").isNotEmpty())
                .andExpect(jsonPath("$.data.vibe.spark").value(0.0));

        var outsiderMeta = new MockMultipartFile("post", "", "application/json",
                json.writeValueAsBytes(Map.of("circleId", cid, "content", "let me in")));
        mvc.perform(multipart("/api/v1/posts").file(outsiderMeta).header("Authorization", outsider.bearer()))
                .andExpect(status().isForbidden());

        var tooMany = new MockMultipartFile("post", "", "application/json", json.writeValueAsBytes(Map.of("circleId", cid)));
        mvc.perform(multipart("/api/v1/posts").file(tooMany).file(image("images")).file(image("images"))
                        .file(image("images")).file(image("images")).file(image("images"))
                        .header("Authorization", owner.bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oneReactionPerUserToggleableAndShownAsProportions() throws Exception {
        Session owner = register();
        Session a = register();
        Session b = register();
        String slug = createCircle(owner, "Vibe Check", false, null);
        join(a, slug);
        join(b, slug);
        String postId = createPost(owner, circleId(slug), "How's everyone doing?").get("id").asText();

        react(a, postId, "spark").andExpect(jsonPath("$.data.vibe.spark").value(1.0));
        react(b, postId, "deep").andExpect(jsonPath("$.data.vibe.spark").value(0.5)).andExpect(jsonPath("$.data.vibe.deep").value(0.5));
        // Changing a reaction replaces it (still one per user)
        react(a, postId, "deep").andExpect(jsonPath("$.data.vibe.deep").value(1.0)).andExpect(jsonPath("$.data.myReaction").value("deep"));
        assertThat(jdbc.queryForObject("select reaction_count from posts where id = ?::uuid", Integer.class, postId)).isEqualTo(2);

        mvc.perform(delete("/api/v1/posts/" + postId + "/reaction").header("Authorization", a.bearer()))
                .andExpect(jsonPath("$.data.myReaction").doesNotExist());
        assertThat(jdbc.queryForObject("select reaction_count from posts where id = ?::uuid", Integer.class, postId)).isEqualTo(1);

        // No raw counts anywhere in the post payload
        String body = mvc.perform(get("/api/v1/posts/" + postId).header("Authorization", b.bearer()))
                .andExpect(jsonPath("$.data.myReaction").value("deep"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("reactionCount");
    }

    @Test
    void privateCirclePostsAreHiddenFromOutsiders() throws Exception {
        Session owner = register();
        String slug = createCircle(owner, "Inner Room", true, null);
        String postId = createPost(owner, circleId(slug), "members only").get("id").asText();
        mvc.perform(get("/api/v1/posts/" + postId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/posts/" + postId).header("Authorization", register().bearer())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/circles/" + slug + "/posts")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/posts/" + postId).header("Authorization", owner.bearer())).andExpect(status().isOk());
    }

    @Test
    void editOnlyWithinFifteenMinutesAndAdminsCanDelete() throws Exception {
        Session owner = register();
        Session author = register();
        String slug = createCircle(owner, "Edit Window", false, null);
        join(author, slug);
        String postId = createPost(author, circleId(slug), "frist").get("id").asText();

        mvc.perform(patch("/api/v1/posts/" + postId).header("Authorization", author.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("content", "first"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("first"))
                .andExpect(jsonPath("$.data.editedAt").isNotEmpty());
        mvc.perform(patch("/api/v1/posts/" + postId).header("Authorization", owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("content", "hijack"))))
                .andExpect(status().isForbidden());

        jdbc.update("update posts set created_at = now() - interval '16 minutes' where id = ?::uuid", postId);
        mvc.perform(patch("/api/v1/posts/" + postId).header("Authorization", author.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("content", "too late"))))
                .andExpect(status().isForbidden());

        // Circle admin removes it
        mvc.perform(delete("/api/v1/posts/" + postId).header("Authorization", owner.bearer())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/posts/" + postId)).andExpect(status().isNotFound());
    }

    @Test
    void saveReportAndPin() throws Exception {
        Session owner = register();
        Session reader = register();
        String slug = createCircle(owner, "Keepers", false, null);
        join(reader, slug);
        String postId = createPost(owner, circleId(slug), "worth keeping").get("id").asText();

        mvc.perform(put("/api/v1/posts/" + postId + "/save").header("Authorization", reader.bearer()))
                .andExpect(jsonPath("$.data.saved").value(true));
        mvc.perform(get("/api/v1/me/saved").header("Authorization", reader.bearer()))
                .andExpect(jsonPath("$.data.items[0].id").value(postId))
                .andExpect(jsonPath("$.data.items[0].saved").value(true));

        for (int i = 0; i < 2; i++) { // second report is a silent no-op
            mvc.perform(post("/api/v1/posts/" + postId + "/report").header("Authorization", reader.bearer())
                            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("reason", "spam"))))
                    .andExpect(status().isOk());
        }
        assertThat(jdbc.queryForObject("select count(*) from reports where target_id = ?::uuid", Integer.class, postId)).isEqualTo(1);

        mvc.perform(put("/api/v1/circles/" + slug + "/pin").header("Authorization", reader.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("postId", postId))))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/circles/" + slug + "/pin").header("Authorization", owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("postId", postId))))
                .andExpect(jsonPath("$.data.pinnedPostId").value(postId));
        mvc.perform(get("/api/v1/posts/" + postId).header("Authorization", owner.bearer()))
                .andExpect(jsonPath("$.data.pinned").value(true))
                .andExpect(jsonPath("$.data.canPin").value(true));
    }

    private org.springframework.test.web.servlet.ResultActions react(Session s, String postId, String type) throws Exception {
        return mvc.perform(put("/api/v1/posts/" + postId + "/reaction").header("Authorization", s.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("type", type))));
    }

    private static MockMultipartFile image(String name) throws Exception {
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return new MockMultipartFile(name, "p.jpg", "image/jpeg", out.toByteArray());
    }
}
