package com.connecthub.auth.oauth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.time.Duration;

/**
 * Stores pending OAuth2 authorization requests server-side, keyed by the unguessable {@code state}
 * parameter, so the API can stay sessionless. Entries expire after 10 minutes.
 * (Avoids the usual cookie-serialisation approach, which would deserialise attacker-controlled bytes.)
 */
public class StateKeyedAuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private final Cache<String, OAuth2AuthorizationRequest> pending = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(50_000)
            .build();

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        return state == null ? null : pending.getIfPresent(state);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest != null) {
            pending.put(authorizationRequest.getState(), authorizationRequest);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        return state == null ? null : pending.asMap().remove(state);
    }
}
