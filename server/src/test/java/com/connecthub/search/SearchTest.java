package com.connecthub.search;

import com.connecthub.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchTest extends AuthTestSupport {

    @Test
    void fullTextSearchFindsPeopleCirclesAndPostsByPrefix() throws Exception {
        Session me = registerWithUsername("zephyrine");
        mvc.perform(patch("/api/v1/me/profile").header("Authorization", me.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("displayName", "Zephyrine Quill"))));
        String slug = createCircle(me, "Quokka Photographers", false, null);
        createPost(me, circleId(slug), "Baking quokka-shaped sourdough this weekend");

        Session searcher = register();
        // prefix match while typing: "zeph" → Zephyrine, "quok" → circle, "baked" stems to "bake" → post
        mvc.perform(get("/api/v1/search").param("q", "zeph").header("Authorization", searcher.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.people[0].displayName").value("Zephyrine Quill"));
        mvc.perform(get("/api/v1/search").param("q", "quok").param("type", "circles").header("Authorization", searcher.bearer()))
                .andExpect(jsonPath("$.data.circles[0].slug").value(slug))
                .andExpect(jsonPath("$.data.people.length()").value(0));
        mvc.perform(get("/api/v1/search").param("q", "baked sourdough").header("Authorization", searcher.bearer()))
                .andExpect(jsonPath("$.data.posts[0].content").value("Baking quokka-shaped sourdough this weekend"));
        // operators in user input are neutralised
        mvc.perform(get("/api/v1/search").param("q", "!:*&|()'").header("Authorization", searcher.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts.length()").value(0));
        // Discover uses the same full-text search
        mvc.perform(get("/api/v1/circles").param("q", "photographer"))
                .andExpect(jsonPath("$.data.items[0].slug").value(slug));
    }

    @Test
    void privateCirclePostsOnlySearchableByMembers() throws Exception {
        Session owner = register();
        String slug = createCircle(owner, "Hidden Vault", true, null);
        createPost(owner, circleId(slug), "xylophonic secrets inside");
        String body = mvc.perform(get("/api/v1/search").param("q", "xylophonic").header("Authorization", register().bearer()))
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(body).at("/data/posts").size()).isZero();
        mvc.perform(get("/api/v1/search").param("q", "xylophonic").header("Authorization", owner.bearer()))
                .andExpect(jsonPath("$.data.posts.length()").value(1));
    }
}
