package com.connecthub.auth;

import com.connecthub.AuthTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowTest extends AuthTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void registerIssuesAccessTokenAndHttpOnlyRefreshCookie() throws Exception {
        Session s = register();

        assertThat(s.accessToken()).isNotBlank();
        assertThat(s.refreshCookie()).isNotNull();
        assertThat(s.refreshCookie().isHttpOnly()).isTrue();
        assertThat(s.refreshCookie().getPath()).isEqualTo("/api/v1/auth");
        assertThat(s.user().get("onboarded").asBoolean()).isFalse();
        assertThat(s.user().get("hasPassword").asBoolean()).isTrue();
        // Only the hash is stored server-side.
        Integer rawStored = jdbc.queryForObject("select count(*) from refresh_tokens where token_hash = ?", Integer.class,
                s.refreshCookie().getValue());
        assertThat(rawStored).isZero();
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        String email = uniqueEmail();
        register(email);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("displayName", "Dup", "email", email.toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void weakPasswordFailsValidationWithFieldError() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("displayName", "Weak", "email", uniqueEmail(), "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fields.password").exists());
    }

    @Test
    void loginChecksPassword() throws Exception {
        String email = uniqueEmail();
        register(email);

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "WrongPass1"))))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "  " + email.toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value(email));
    }

    @Test
    void meRequiresAValidBearerToken() throws Exception {
        Session s = register();
        mvc.perform(get("/api/v1/me").header("Authorization", s.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(s.user().get("id").asText()));
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer not.a.jwt")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokenAndReuseRevokesEverySession() throws Exception {
        Session s = register();

        MockHttpServletResponse first = refresh(s.refreshCookie());
        assertThat(first.getStatus()).isEqualTo(200);
        Cookie rotated = first.getCookie(RefreshCookies.NAME);
        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotEqualTo(s.refreshCookie().getValue());

        // Pretend the rotation happened long ago, then replay the old cookie: that's token theft.
        jdbc.update("update refresh_tokens set revoked_at = now() - interval '5 minutes' where token_hash = ?",
                RefreshTokenService.hash(s.refreshCookie().getValue()));
        assertThat(refresh(s.refreshCookie()).getStatus()).isEqualTo(401);

        // The legitimate (rotated) token was revoked too.
        assertThat(refresh(rotated).getStatus()).isEqualTo(401);
    }

    @Test
    void concurrentRefreshWithinGraceWindowSucceedsWithoutNewCookie() throws Exception {
        Session s = register();
        assertThat(refresh(s.refreshCookie()).getStatus()).isEqualTo(200);

        MockHttpServletResponse second = refresh(s.refreshCookie());
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(second.getCookie(RefreshCookies.NAME)).isNull();
        assertThat(json.readTree(second.getContentAsString()).at("/data/accessToken").asText()).isNotBlank();
    }

    @Test
    void logoutRevokesRefreshTokenAndClearsCookie() throws Exception {
        Session s = register();
        MockHttpServletResponse res = mvc.perform(post("/api/v1/auth/logout").cookie(s.refreshCookie()))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(res.getCookie(RefreshCookies.NAME).getMaxAge()).isZero();
        assertThat(refresh(s.refreshCookie()).getStatus()).isEqualTo(401);
    }

    @Test
    void loginIsRateLimitedPerIp() throws Exception {
        String body = json.writeValueAsString(Map.of("email", uniqueEmail(), "password", "Nope12345"));
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void providersEndpointIsPublic() throws Exception {
        mvc.perform(get("/api/v1/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").value(true))
                .andExpect(jsonPath("$.data.google").value(false));
    }

    private MockHttpServletResponse refresh(Cookie cookie) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").cookie(cookie)).andReturn().getResponse();
    }
}
