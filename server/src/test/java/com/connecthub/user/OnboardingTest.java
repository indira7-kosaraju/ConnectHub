package com.connecthub.user;

import com.connecthub.AuthTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OnboardingTest extends AuthTestSupport {

    @Test
    void fullOnboardingFlow() throws Exception {
        Session s = register();
        String handle = "tester_" + System.nanoTime() % 1_000_000;

        // Can't finish before choosing a username and interests.
        mvc.perform(post("/api/v1/me/onboarding/complete").header("Authorization", s.bearer()))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/v1/me/username").header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username", handle.toUpperCase()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(handle));

        List<String> ids = interestIds(3);
        mvc.perform(put("/api/v1/me/interests").header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("interestIds", ids))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interests.length()").value(3));

        mvc.perform(post("/api/v1/me/onboarding/complete").header("Authorization", s.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onboarded").value(true));
    }

    @Test
    void usernameRulesAreEnforced() throws Exception {
        Session a = register();
        Session b = register();
        String handle = "taken_" + System.nanoTime() % 1_000_000;
        mvc.perform(put("/api/v1/me/username").header("Authorization", a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username", handle))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/me/username-availability").param("username", handle).header("Authorization", b.bearer()))
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.reason").value("TAKEN"));
        mvc.perform(get("/api/v1/me/username-availability").param("username", "settings").header("Authorization", b.bearer()))
                .andExpect(jsonPath("$.data.reason").value("RESERVED"));
        mvc.perform(put("/api/v1/me/username").header("Authorization", b.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username", handle))))
                .andExpect(status().isConflict());
    }

    @Test
    void fewerThanThreeInterestsIsRejected() throws Exception {
        Session s = register();
        mvc.perform(put("/api/v1/me/interests").header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("interestIds", interestIds(2)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.interestIds").exists());
    }

    @Test
    void avatarUploadIsResizedAndServed() throws Exception {
        Session s = register();
        MockMultipartFile png = new MockMultipartFile("file", "a.png", "image/png", png(1200, 800));

        String body = mvc.perform(multipart("/api/v1/me/avatar").file(png).header("Authorization", s.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String url = json.readTree(body).at("/data/avatarUrl").asText();
        assertThat(url).startsWith("/uploads/avatars-").endsWith(".jpg");

        byte[] served = mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(served));
        assertThat(img.getWidth()).isEqualTo(400);
        assertThat(img.getHeight()).isEqualTo(400);
    }

    @Test
    void nonImageUploadIsRejected() throws Exception {
        Session s = register();
        MockMultipartFile fake = new MockMultipartFile("file", "x.png", "image/png", "<?php echo 1; ?>".getBytes());
        mvc.perform(multipart("/api/v1/me/avatar").file(fake).header("Authorization", s.bearer()))
                .andExpect(status().isBadRequest());
    }

    private List<String> interestIds(int n) throws Exception {
        String body = mvc.perform(get("/api/v1/interests")).andReturn().getResponse().getContentAsString();
        List<String> ids = new ArrayList<>();
        for (JsonNode node : json.readTree(body).get("data")) {
            if (ids.size() == n) break;
            ids.add(node.get("id").asText());
        }
        return ids;
    }

    private static byte[] png(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
