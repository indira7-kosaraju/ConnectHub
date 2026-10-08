package com.connecthub.auth;

import com.connecthub.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.oauth.google.client-id=test-client",
        "app.oauth.google.client-secret=test-secret",
        "app.oauth.google.redirect-uri=http://localhost:5173/login/oauth2/code/google"
})
class GoogleOAuthEnabledTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void googleIsAdvertisedAndAuthorizationRedirectsToGoogleWithoutASession() throws Exception {
        mvc.perform(get("/api/v1/auth/providers")).andExpect(jsonPath("$.data.google").value(true));

        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", allOf(
                        startsWith("https://accounts.google.com/o/oauth2/v2/auth"),
                        containsString("client_id=test-client"),
                        containsString("state="))))
                .andExpect(header().doesNotExist("Set-Cookie")); // stateless: no JSESSIONID
    }

    @Test
    void callbackWithUnknownStateRedirectsToSpaWithError() throws Exception {
        mvc.perform(get("/login/oauth2/code/google").param("code", "x").param("state", "forged"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "http://localhost:5173/auth/callback?error=oauth_failed"));
    }
}
