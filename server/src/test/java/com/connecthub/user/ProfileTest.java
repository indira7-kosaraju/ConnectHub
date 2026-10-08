package com.connecthub.user;

import com.connecthub.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileTest extends AuthTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void editProfileAndReadItPublicly() throws Exception {
        Session s = registerWithUsername("writer");
        String username = s.user().get("username").asText();

        mvc.perform(patch("/api/v1/me/profile").header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("bio", "Hello <script>x</script>world", "website", "example.com/me", "location", "Lisbon"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bio").value("Hello world"))
                .andExpect(jsonPath("$.data.website").value("https://example.com/me"));

        mvc.perform(get("/api/v1/users/" + username.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.location").value("Lisbon"))
                .andExpect(jsonPath("$.data.me").value(false))
                .andExpect(jsonPath("$.data.restricted").value(false));

        mvc.perform(patch("/api/v1/me/profile").header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("website", "javascript:alert(1)"))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/users/nobody_by_this_name")).andExpect(status().isNotFound());
    }

    @Test
    void privateProfileIsVisibleOnlyToCircleMates() throws Exception {
        Session owner = registerWithUsername("hermit");
        Session stranger = register();
        Session mate = register();
        String username = owner.user().get("username").asText();
        jdbc.update("update users set is_private = true, location = 'Secret' where username = ?", username);
        String slug = createCircle(owner, "Hermit Hut", false, null);
        mvc.perform(post("/api/v1/circles/" + slug + "/join").header("Authorization", mate.bearer()));

        mvc.perform(get("/api/v1/users/" + username).header("Authorization", stranger.bearer()))
                .andExpect(jsonPath("$.data.restricted").value(true))
                .andExpect(jsonPath("$.data.location").doesNotExist());
        mvc.perform(get("/api/v1/users/" + username + "/circles").header("Authorization", stranger.bearer()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/users/" + username).header("Authorization", mate.bearer()))
                .andExpect(jsonPath("$.data.restricted").value(false))
                .andExpect(jsonPath("$.data.location").value("Secret"));
        mvc.perform(get("/api/v1/users/" + username + "/circles").header("Authorization", mate.bearer()))
                .andExpect(jsonPath("$.data[0].slug").value(slug));
    }
}
