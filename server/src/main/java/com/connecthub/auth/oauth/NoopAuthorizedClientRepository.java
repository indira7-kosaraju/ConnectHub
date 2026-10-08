package com.connecthub.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

/**
 * We only use Google to establish identity and never call Google APIs afterwards, so the
 * provider's access token is not kept (avoids unbounded in-memory growth and holding secrets).
 */
public class NoopAuthorizedClientRepository implements OAuth2AuthorizedClientRepository {

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String registrationId, Authentication principal,
                                                                     HttpServletRequest request) {
        return null;
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient client, Authentication principal,
                                     HttpServletRequest request, HttpServletResponse response) {
    }

    @Override
    public void removeAuthorizedClient(String registrationId, Authentication principal,
                                       HttpServletRequest request, HttpServletResponse response) {
    }
}
