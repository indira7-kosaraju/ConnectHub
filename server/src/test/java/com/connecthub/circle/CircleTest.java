package com.connecthub.circle;

import com.connecthub.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CircleTest extends AuthTestSupport {

    @Test
    void creatorBecomesAdminAndSlugIsUnique() throws Exception {
        Session owner = register();
        String a = createCircle(owner, "Night Owls Club", false, null);
        String b = createCircle(owner, "Night Owls Club!", false, null);
        assertThat(a).startsWith("night-owls-club");
        assertThat(b).isNotEqualTo(a).startsWith("night-owls-club");

        mvc.perform(get("/api/v1/circles/" + a).header("Authorization", owner.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.maxMembers").value(150))
                .andExpect(jsonPath("$.data.viewer.role").value("admin"))
                .andExpect(jsonPath("$.data.viewer.status").value("active"));
    }

    @Test
    void publicCircleIsReadableAnonymouslyAndJoinIsImmediate() throws Exception {
        Session owner = register();
        Session joiner = register();
        String slug = createCircle(owner, "Open Door", false, null);

        mvc.perform(get("/api/v1/circles/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewer").doesNotExist())
                .andExpect(jsonPath("$.data.canViewContent").value(true));

        mvc.perform(post("/api/v1/circles/" + slug + "/join").header("Authorization", joiner.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.membership.status").value("active"))
                .andExpect(jsonPath("$.data.memberCount").value(2));
        // Idempotent
        mvc.perform(post("/api/v1/circles/" + slug + "/join").header("Authorization", joiner.bearer()))
                .andExpect(jsonPath("$.data.memberCount").value(2));
        mvc.perform(get("/api/v1/circles/" + slug + "/members"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].role").value("admin"));
    }

    @Test
    void privateCircleRequiresApprovalAndHidesMembers() throws Exception {
        Session owner = register();
        Session joiner = register();
        String slug = createCircle(owner, "Quiet Room", true, null);
        String joinerId = joiner.user().get("id").asText();

        mvc.perform(get("/api/v1/circles/" + slug + "/members").header("Authorization", joiner.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/circles/" + slug + "/join").header("Authorization", joiner.bearer()))
                .andExpect(jsonPath("$.data.membership.status").value("pending"))
                .andExpect(jsonPath("$.data.memberCount").value(1));

        // Only admins see requests
        mvc.perform(get("/api/v1/circles/" + slug + "/members").param("status", "pending").header("Authorization", joiner.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/circles/" + slug).header("Authorization", owner.bearer()))
                .andExpect(jsonPath("$.data.pendingCount").value(1));

        mvc.perform(post("/api/v1/circles/" + slug + "/members/" + joinerId + "/approve").header("Authorization", owner.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active"));
        mvc.perform(get("/api/v1/circles/" + slug).header("Authorization", joiner.bearer()))
                .andExpect(jsonPath("$.data.memberCount").value(2))
                .andExpect(jsonPath("$.data.canViewContent").value(true));
    }

    @Test
    void fullCircleRejectsJoins() throws Exception {
        Session owner = register();
        String slug = createCircle(owner, "Tiny Table", false, 2);
        mvc.perform(post("/api/v1/circles/" + slug + "/join").header("Authorization", register().bearer()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/circles/" + slug + "/join").header("Authorization", register().bearer()))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyAdminsCanEditOrRemoveAndAdminsCantRemoveAdmins() throws Exception {
        Session owner = register();
        Session member = register();
        String slug = createCircle(owner, "Guarded Garden", false, null);
        mvc.perform(post("/api/v1/circles/" + slug + "/join").header("Authorization", member.bearer()));

        mvc.perform(patch("/api/v1/circles/" + slug).header("Authorization", member.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name", "Hijacked"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/circles/" + slug + "/members/" + owner.user().get("id").asText())
                        .header("Authorization", member.bearer()))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/v1/circles/" + slug).header("Authorization", owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name", "<b>Renamed</b> Garden"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Renamed Garden"))
                .andExpect(jsonPath("$.data.slug").value(slug));
        mvc.perform(delete("/api/v1/circles/" + slug + "/members/" + member.user().get("id").asText())
                        .header("Authorization", owner.bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/circles/" + slug)).andExpect(jsonPath("$.data.memberCount").value(1));
    }

    @Test
    void lastAdminLeavingPromotesOldestMemberOrDeletesEmptyCircle() throws Exception {
        Session owner = register();
        Session first = register();
        Session second = register();
        String slug = createCircle(owner, "Hand Over", false, null);
        mvc.perform(post("/api/v1/circles/" + slug + "/join").header("Authorization", first.bearer()));
        mvc.perform(post("/api/v1/circles/" + slug + "/join").header("Authorization", second.bearer()));

        mvc.perform(delete("/api/v1/circles/" + slug + "/membership").header("Authorization", owner.bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/circles/" + slug).header("Authorization", first.bearer()))
                .andExpect(jsonPath("$.data.viewer.role").value("admin"))
                .andExpect(jsonPath("$.data.memberCount").value(2));

        String solo = createCircle(owner, "Solo", false, null);
        mvc.perform(delete("/api/v1/circles/" + solo + "/membership").header("Authorization", owner.bearer()));
        mvc.perform(get("/api/v1/circles/" + solo)).andExpect(status().isNotFound());
    }

    @Test
    void suggestionsFollowInterestsAndExcludeJoinedCircles() throws Exception {
        // Use categories no other test creates circles in, so the result is deterministic.
        var interests = json.readTree(mvc.perform(get("/api/v1/interests")).andReturn().getResponse().getContentAsString()).get("data");
        String a = interests.get(interests.size() - 1).get("id").asText();
        String b = interests.get(interests.size() - 2).get("id").asText();
        String c = interests.get(interests.size() - 3).get("id").asText();

        Session owner = register();
        String slug = createCircle(owner, "Interest Match", false, null, a);
        Session newcomer = register();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/me/interests")
                        .header("Authorization", newcomer.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("interestIds", java.util.List.of(a, b, c)))))
                .andExpect(status().isOk());

        String body = mvc.perform(get("/api/v1/me/circle-suggestions").param("limit", "12").header("Authorization", newcomer.bearer()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).contains("\"slug\":\"" + slug + "\"");

        String ownerView = mvc.perform(get("/api/v1/me/circle-suggestions").param("limit", "12").header("Authorization", owner.bearer()))
                .andReturn().getResponse().getContentAsString();
        assertThat(ownerView).doesNotContain("\"slug\":\"" + slug + "\"");
    }

    @Test
    void browseFiltersByQuery() throws Exception {
        Session owner = register();
        String slug = createCircle(owner, "Zebra Finch Fanciers", false, null);
        mvc.perform(get("/api/v1/circles").param("q", "zebra finch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].slug").value(slug));
        mvc.perform(get("/api/v1/circles").param("q", "100%_nothing"))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }
}
