package com.connecthub.auth.oauth;

import com.connecthub.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * Registers Google only when credentials are configured, so the app boots (and Google sign-in is
 * simply hidden) in environments without them.
 */
@Configuration
@ConditionalOnExpression("!'${app.oauth.google.client-id:}'.isBlank() and !'${app.oauth.google.client-secret:}'.isBlank()")
public class GoogleOAuthConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(AppProperties props) {
        AppProperties.Google g = props.oauth().google();
        var registration = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(g.clientId())
                .clientSecret(g.clientSecret())
                .redirectUri(g.redirectUri())
                .scope("openid", "email", "profile")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }
}
