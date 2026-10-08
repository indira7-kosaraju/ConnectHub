package com.connecthub.user;

import com.connecthub.auth.AuthUser;
import com.connecthub.auth.ClientInfo;
import com.connecthub.auth.RefreshCookies;
import com.connecthub.auth.RefreshTokenService;
import com.connecthub.auth.dto.AuthResponse;
import com.connecthub.common.ApiResponse;
import com.connecthub.common.RateLimiter;
import com.connecthub.moderation.BlockService;
import com.connecthub.user.dto.ChangeEmailRequest;
import com.connecthub.user.dto.ChangePasswordRequest;
import com.connecthub.user.dto.DeleteAccountRequest;
import com.connecthub.user.dto.MeResponse;
import com.connecthub.user.dto.SettingsRequest;
import com.connecthub.user.dto.UserSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Settings")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class SettingsController {

    private final AccountService accounts;
    private final BlockService blocks;
    private final RefreshCookies cookies;
    private final RefreshTokenService refreshTokens;
    private final RateLimiter rateLimiter;

    @Operation(summary = "Update privacy and notification preferences (partial)")
    @PatchMapping("/settings")
    public ApiResponse<MeResponse> settings(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody SettingsRequest req) {
        return ApiResponse.ok(accounts.updateSettings(me.id(), req));
    }

    @Operation(summary = "Change your email (requires your current password)")
    @PutMapping("/email")
    public ApiResponse<MeResponse> email(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ChangeEmailRequest req,
                                         HttpServletRequest http) {
        rateLimiter.consume(RateLimiter.Limit.AUTH, ClientInfo.ip(http));
        return ApiResponse.ok(accounts.changeEmail(me.id(), req));
    }

    @Operation(summary = "Change or set your password. Signs out every other session and returns a fresh one.")
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<AuthResponse>> password(@AuthenticationPrincipal AuthUser me,
                                                              @Valid @RequestBody ChangePasswordRequest req,
                                                              HttpServletRequest http) {
        rateLimiter.consume(RateLimiter.Limit.AUTH, ClientInfo.ip(http));
        var session = accounts.changePassword(me.id(), req, ClientInfo.userAgent(http));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.create(session.refreshToken(), refreshTokens.ttl()).toString())
                .body(ApiResponse.ok(session.body()));
    }

    @Operation(summary = "Permanently delete your account")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody DeleteAccountRequest req,
                                                    HttpServletRequest http) {
        rateLimiter.consume(RateLimiter.Limit.AUTH, ClientInfo.ip(http));
        accounts.delete(me.id(), req);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookies.clear().toString()).body(ApiResponse.ok());
    }

    @Operation(summary = "People you've blocked")
    @GetMapping("/blocks")
    public ApiResponse<List<UserSummary>> blocked(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(blocks.blockedBy(me.id()));
    }
}
