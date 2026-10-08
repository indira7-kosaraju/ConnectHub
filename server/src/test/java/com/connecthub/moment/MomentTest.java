package com.connecthub.moment;

import com.connecthub.AuthTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MomentTest extends AuthTestSupport {

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    MomentExpiryScheduler scheduler;

    @Test
    void createTextAndImageMomentsWithValidation() throws Exception {
        Session me = register();
        create(me, Map.of("kind", "text", "text", "Hello <b>world</b>", "background", "ocean"), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.text").value("Hello world"))
                .andExpect(jsonPath("$.data.background").value("ocean"))
                .andExpect(jsonPath("$.data.viewCount").value(0));
        create(me, Map.of("kind", "image"), photo())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imageUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.imageHeight").value(1200));
        create(me, Map.of("kind", "text"), null).andExpect(status().isBadRequest());
        create(me, Map.of("kind", "image"), null).andExpect(status().isBadRequest());
        create(me, Map.of("kind", "text", "text", "x", "background", "neon"), null).andExpect(status().isBadRequest());
    }

    @Test
    void stripShowsCircleMatesOnlyAndViewsAreCountedOncePrivately() throws Exception {
        Session author = register();
        Session mate = register();
        Session stranger = register();
        String slug = createCircle(author, "Moment Mates", false, null);
        join(mate, slug);
        String id = momentId(create(author, Map.of("kind", "text", "text", "sunrise", "background", "sunset"), null));

        JsonNode strip = strip(mate);
        JsonNode group = findGroup(strip, author.user().get("id").asText());
        assertThat(group).isNotNull();
        assertThat(group.get("allSeen").asBoolean()).isFalse();
        assertThat(group.at("/moments/0/viewCount").isNull()).isTrue(); // count hidden from viewers
        assertThat(findGroup(strip(stranger), author.user().get("id").asText())).isNull();

        mvc.perform(post("/api/v1/moments/" + id + "/view").header("Authorization", mate.bearer())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/moments/" + id + "/view").header("Authorization", mate.bearer())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/moments/" + id + "/view").header("Authorization", stranger.bearer())).andExpect(status().isNotFound());

        assertThat(findGroup(strip(mate), author.user().get("id").asText()).get("allSeen").asBoolean()).isTrue();
        JsonNode mine = strip(author).get(0);
        assertThat(mine.get("mine").asBoolean()).isTrue();
        assertThat(mine.at("/moments/0/viewCount").asInt()).isEqualTo(1);
    }

    @Test
    void expiredMomentsLeaveTheStripAndTheSchedulerMovesThemToRewind() throws Exception {
        Session me = register();
        String id = momentId(create(me, Map.of("kind", "text", "text", "yesterday", "background", "dusk"), null));
        mvc.perform(get("/api/v1/me/rewind").header("Authorization", me.bearer())).andExpect(jsonPath("$.data.items.length()").value(0));

        jdbc.update("update moments set created_at = now() - interval '25 hours', expires_at = now() - interval '1 hour' where id = ?::uuid", id);
        assertThat(strip(me).size()).isZero(); // gone from the strip immediately, even before the job runs

        scheduler.archiveExpiredMoments();
        assertThat(jdbc.queryForObject("select archived from moments where id = ?::uuid", Boolean.class, id)).isTrue();
        mvc.perform(get("/api/v1/me/rewind").header("Authorization", me.bearer()))
                .andExpect(jsonPath("$.data.items[0].id").value(id));
        // Rewind is private: another user's rewind only ever contains their own moments
        mvc.perform(get("/api/v1/me/rewind").header("Authorization", register().bearer()))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void onlyTheAuthorCanDelete() throws Exception {
        Session me = register();
        String id = momentId(create(me, Map.of("kind", "text", "text", "temp", "background", "plum"), null));
        mvc.perform(delete("/api/v1/moments/" + id).header("Authorization", register().bearer())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/moments/" + id).header("Authorization", me.bearer())).andExpect(status().isOk());
        assertThat(strip(me).size()).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions create(Session s, Map<String, String> body, MockMultipartFile image) throws Exception {
        var req = multipart("/api/v1/moments")
                .file(new MockMultipartFile("moment", "", "application/json", json.writeValueAsBytes(body)));
        if (image != null) req.file(image);
        return mvc.perform(req.header("Authorization", s.bearer()));
    }

    private String momentId(org.springframework.test.web.servlet.ResultActions res) throws Exception {
        return json.readTree(res.andReturn().getResponse().getContentAsString()).at("/data/id").asText();
    }

    private JsonNode strip(Session s) throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/moments/strip").header("Authorization", s.bearer()))
                .andReturn().getResponse().getContentAsString()).get("data");
    }

    private static JsonNode findGroup(JsonNode strip, String userId) {
        for (JsonNode g : strip) {
            if (g.at("/user/id").asText().equals(userId)) return g;
        }
        return null;
    }

    private static MockMultipartFile photo() throws Exception {
        BufferedImage img = new BufferedImage(900, 1200, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return new MockMultipartFile("image", "m.jpg", "image/jpeg", out.toByteArray());
    }
}
