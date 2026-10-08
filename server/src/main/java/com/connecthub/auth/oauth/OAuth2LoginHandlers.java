package com.connecthub.auth.oauth;

import com.connecthub.auth.AuthService;
import com.connecthub.auth.ClientInfo;
import com.connecthub.auth.RefreshCookies;
import com.connecthub.auth.RefreshTokenService;
import com.connecthub.common.exception.ApiException;
import com.connecthub.config.AppProperties;
import com.connecthub.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * After Google sign-in: create/link the user, set the refresh cookie and redirect to the SPA's
 * /auth/callback, which exchanges the cookie for an access token. Tokens never appear in URLs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginHandlers {

    private final GoogleAccountService googleAccounts;
    private final AuthService authService;
    private final RefreshCookies cookies;
    private final RefreshTokenService refreshTokens;
    private final AppProperties props;

    public AuthenticationSuccessHandler success() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            SecurityContextHolder.clearContext(); // stateless: nothing to keep server-side
            if (!(authentication.getPrincipal() instanceof OidcUser oidc)) {
                redirect(response, "unsupported_provider");
                return;
            }
            try {
                User user = googleAccounts.upsert(oidc);
                AuthService.Session session = authService.startSession(user, ClientInfo.userAgent(request));
                response.addHeader(HttpHeaders.SET_COOKIE,
                        cookies.create(session.refreshToken(), refreshTokens.ttl()).toString());
                response.sendRedirect(callbackUrl(null));
            } catch (ApiException e) {
                redirect(response, "email_unverified");
            }
        };
    }

    public AuthenticationFailureHandler failure() {
        return (request, response, exception) -> {
            log.info("Google sign-in failed: {}", exception.getMessage());
            redirect(response, "oauth_failed");
        };
    }

    private void redirect(HttpServletResponse response, String error) throws IOException {
        response.sendRedirect(callbackUrl(error));
    }

    private String callbackUrl(String error) {
        var b = UriComponentsBuilder.fromUriString(props.frontendUrl()).path("/auth/callback");
        if (error != null) {
            b.queryParam("error", error);
        }
        return b.build().toUriString();
    }
}
