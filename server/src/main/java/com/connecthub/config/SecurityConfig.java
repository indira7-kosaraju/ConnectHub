package com.connecthub.config;

import com.connecthub.auth.JwtAuthFilter;
import com.connecthub.auth.oauth.NoopAuthorizedClientRepository;
import com.connecthub.auth.oauth.OAuth2LoginHandlers;
import com.connecthub.auth.oauth.StateKeyedAuthorizationRequestRepository;
import com.connecthub.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Stateless JWT security. Every /api/v1/** route requires authentication except auth endpoints
 * and public reads of profiles, circles and interests. Google sign-in is enabled only when
 * credentials are configured.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    public static final String[] PUBLIC_DOCS = {
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    private final ObjectMapper objectMapper;
    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2LoginHandlers oauthHandlers;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // no cookie-authenticated state changes except refresh/logout (SameSite + path-scoped)
                .cors(cors -> {
                })
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(PUBLIC_DOCS).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/ws/**").permitAll() // STOMP CONNECT frames are authenticated by a channel interceptor
                        .requestMatchers("/api/v1/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/users/*", "/api/v1/users/*/posts", "/api/v1/users/*/circles",
                                "/api/v1/circles", "/api/v1/circles/*", "/api/v1/circles/*/members", "/api/v1/circles/*/posts",
                                "/api/v1/circles/discover/**", "/api/v1/posts/*", "/api/v1/posts/*/comments",
                                "/api/v1/interests").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonEntryPoint())
                        .accessDeniedHandler(jsonAccessDenied()));

        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .authorizationEndpoint(a -> a.authorizationRequestRepository(new StateKeyedAuthorizationRequestRepository()))
                    .authorizedClientRepository(new NoopAuthorizedClientRepository())
                    .successHandler(oauthHandlers.success())
                    .failureHandler(oauthHandlers.failure()));
        }
        return http.build();
    }

    /** The JWT filter runs inside the security chain only — not also as a plain servlet filter. */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    private AuthenticationEntryPoint jsonEntryPoint() {
        return (request, response, ex) ->
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
    }

    private AccessDeniedHandler jsonAccessDenied() {
        return (request, response, ex) ->
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "Access denied");
    }

    private void writeJson(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(code, message));
    }
}
