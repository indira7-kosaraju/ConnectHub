package com.connecthub.auth;

import com.connecthub.auth.dto.AuthProvidersResponse;
import com.connecthub.auth.dto.AuthResponse;
import com.connecthub.auth.dto.LoginRequest;
import com.connecthub.auth.dto.RegisterRequest;
import com.connecthub.common.ApiResponse;
import com.connecthub.common.RateLimiter;
import com.connecthub.config.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@SecurityRequirements // public endpoints
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshCookies cookies;
    private final RefreshTokenService refreshTokens;
    private final RateLimiter rateLimiter;
    private final AppProperties props;

    @Operation(summary = "Create an account with email and password")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        rateLimiter.consume(RateLimiter.Limit.AUTH, ClientInfo.ip(http));
        return withSession(HttpStatus.CREATED, authService.register(req, ClientInfo.userAgent(http)));
    }

    @Operation(summary = "Sign in with email and password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        rateLimiter.consume(RateLimiter.Limit.AUTH, ClientInfo.ip(http));
        return withSession(HttpStatus.OK, authService.login(req, ClientInfo.userAgent(http)));
    }

    @Operation(summary = "Exchange the refresh-token cookie for a new access token (rotates the cookie). "
            + "Without a cookie, returns data: null (signed out) rather than an error.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(HttpServletRequest http) {
        rateLimiter.consume(RateLimiter.Limit.REFRESH, ClientInfo.ip(http));
        String raw = cookies.read(http);
        if (raw == null || raw.isBlank()) {
            // Every signed-out page load probes for a session; that's not an error worth a 401.
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
        return withSession(HttpStatus.OK, authService.refresh(raw, ClientInfo.userAgent(http)));
    }

    @Operation(summary = "Sign out: revoke the refresh token and clear the cookie")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest http) {
        authService.logout(cookies.read(http));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .body(ApiResponse.ok());
    }

    @Operation(summary = "Sign-in methods available on this deployment")
    @GetMapping("/providers")
    public ApiResponse<AuthProvidersResponse> providers() {
        return ApiResponse.ok(new AuthProvidersResponse(true, props.oauth().google().isConfigured()));
    }

    private ResponseEntity<ApiResponse<AuthResponse>> withSession(HttpStatus status, AuthService.Session session) {
        var builder = ResponseEntity.status(status);
        if (session.refreshToken() != null) {
            builder.header(HttpHeaders.SET_COOKIE, cookies.create(session.refreshToken(), refreshTokens.ttl()).toString());
        }
        return builder.body(ApiResponse.ok(session.body()));
    }
}
