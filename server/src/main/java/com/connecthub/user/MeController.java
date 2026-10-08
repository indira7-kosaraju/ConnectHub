package com.connecthub.user;

import com.connecthub.auth.AuthUser;
import com.connecthub.circle.CircleService;
import com.connecthub.circle.dto.CircleSummary;
import com.connecthub.user.dto.UpdateProfileRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import java.util.List;
import com.connecthub.common.ApiResponse;
import com.connecthub.common.RateLimiter;
import com.connecthub.user.dto.InterestsRequest;
import com.connecthub.user.dto.MeResponse;
import com.connecthub.user.dto.UsernameAvailability;
import com.connecthub.user.dto.UsernameRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Me")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final OnboardingService onboarding;
    private final ProfileService profiles;
    private final CircleService circles;
    private final com.connecthub.post.PostService posts;
    private final RateLimiter rateLimiter;

    @Operation(summary = "The signed-in user's account")
    @GetMapping
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(onboarding.me(me.id()));
    }

    @Operation(summary = "Check whether a username can be claimed")
    @GetMapping("/username-availability")
    public ApiResponse<UsernameAvailability> usernameAvailability(@AuthenticationPrincipal AuthUser me,
                                                                  @RequestParam String username) {
        return ApiResponse.ok(onboarding.checkUsername(username, me.id()));
    }

    @Operation(summary = "Claim a username")
    @PutMapping("/username")
    public ApiResponse<MeResponse> setUsername(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UsernameRequest req) {
        return ApiResponse.ok(onboarding.setUsername(me.id(), req.username()));
    }

    @Operation(summary = "Upload a new avatar (JPEG/PNG/WebP/GIF, ≤ 10 MB; cropped to 400×400)")
    @PostMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MeResponse> uploadAvatar(@AuthenticationPrincipal AuthUser me, @RequestPart("file") MultipartFile file) {
        rateLimiter.consume(RateLimiter.Limit.UPLOAD, me.id().toString());
        return ApiResponse.ok(onboarding.setAvatar(me.id(), file));
    }

    @Operation(summary = "Replace the user's interests (3–10)")
    @PutMapping("/interests")
    public ApiResponse<MeResponse> setInterests(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody InterestsRequest req) {
        return ApiResponse.ok(onboarding.setInterests(me.id(), req.interestIds()));
    }

    @Operation(summary = "Finish onboarding (requires a username and at least 3 interests)")
    @PostMapping("/onboarding/complete")
    public ApiResponse<MeResponse> completeOnboarding(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(onboarding.complete(me.id()));
    }

    @Operation(summary = "Edit profile fields (null = unchanged, empty string clears bio/location/website)")
    @PatchMapping("/profile")
    public ApiResponse<MeResponse> updateProfile(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UpdateProfileRequest req) {
        return ApiResponse.ok(profiles.update(me.id(), req));
    }

    @Operation(summary = "Upload a cover image (cropped to 1500×500)")
    @PostMapping(path = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MeResponse> uploadCover(@AuthenticationPrincipal AuthUser me, @RequestPart("file") MultipartFile file) {
        rateLimiter.consume(RateLimiter.Limit.UPLOAD, me.id().toString());
        return ApiResponse.ok(profiles.uploadCover(me.id(), file));
    }

    @Operation(summary = "Remove the cover image")
    @DeleteMapping("/cover")
    public ApiResponse<MeResponse> removeCover(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(profiles.removeCover(me.id()));
    }

    @Operation(summary = "Circles you're an active member of, most recently joined first")
    @GetMapping("/circles")
    public ApiResponse<List<CircleSummary>> myCircles(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(circles.myCircles(me.id()));
    }

    @Operation(summary = "Circles suggested from your interests (topped up with trending ones)")
    @GetMapping("/circle-suggestions")
    public ApiResponse<List<CircleSummary>> suggestions(@AuthenticationPrincipal AuthUser me,
                                                        @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok(circles.suggestions(me.id(), limit));
    }

    @Operation(summary = "People who share a circle with you (for starting conversations and sharing posts)")
    @GetMapping("/contacts")
    public ApiResponse<List<com.connecthub.user.dto.UserSummary>> contacts(@AuthenticationPrincipal AuthUser me,
                                                                         @RequestParam(defaultValue = "") String q) {
        return ApiResponse.ok(profiles.contacts(me.id(), q));
    }

    @Operation(summary = "Your saved posts, most recently saved first")
    @GetMapping("/saved")
    public ApiResponse<com.connecthub.common.CursorPage<com.connecthub.post.dto.PostDto>> saved(
            @AuthenticationPrincipal AuthUser me, @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(posts.savedPosts(me.id(), cursor, limit));
    }
}
