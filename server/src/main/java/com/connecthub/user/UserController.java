package com.connecthub.user;

import com.connecthub.auth.AuthUser;
import com.connecthub.circle.dto.CircleSummary;
import com.connecthub.common.ApiResponse;
import com.connecthub.user.dto.ProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Profiles")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final ProfileService profiles;
    private final com.connecthub.post.PostService posts;
    private final com.connecthub.moderation.BlockService blocks;
    private final com.connecthub.moderation.ReportService reports;

    @Operation(summary = "Public profile by username")
    @GetMapping("/{username}")
    public ApiResponse<ProfileResponse> profile(@AuthenticationPrincipal AuthUser me, @PathVariable String username) {
        return ApiResponse.ok(profiles.get(username, me == null ? null : me.id()));
    }

    @Operation(summary = "Circles a user belongs to (private circles only shown to co-members)")
    @GetMapping("/{username}/circles")
    public ApiResponse<List<CircleSummary>> circles(@AuthenticationPrincipal AuthUser me, @PathVariable String username) {
        return ApiResponse.ok(profiles.circles(username, me == null ? null : me.id()));
    }

    @Operation(summary = "A user's posts in circles you can read, newest first")
    @GetMapping("/{username}/posts")
    public ApiResponse<com.connecthub.common.CursorPage<com.connecthub.post.dto.PostDto>> posts(
            @AuthenticationPrincipal AuthUser me, @PathVariable String username,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String cursor,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(posts.userPosts(username, me == null ? null : me.id(), cursor, limit));
    }

    @Operation(summary = "Block someone: hides each of you from the other everywhere, and stops messages")
    @org.springframework.web.bind.annotation.PostMapping("/{username}/block")
    public ApiResponse<Void> block(@AuthenticationPrincipal AuthUser me, @PathVariable String username) {
        blocks.block(me.id(), profiles.loadByUsername(username).getId());
        return ApiResponse.ok();
    }

    @Operation(summary = "Unblock someone")
    @org.springframework.web.bind.annotation.DeleteMapping("/{username}/block")
    public ApiResponse<Void> unblock(@AuthenticationPrincipal AuthUser me, @PathVariable String username) {
        blocks.unblock(me.id(), profiles.loadByUsername(username).getId());
        return ApiResponse.ok();
    }

    @Operation(summary = "Report a user")
    @org.springframework.web.bind.annotation.PostMapping("/{username}/report")
    public ApiResponse<Void> report(@AuthenticationPrincipal AuthUser me, @PathVariable String username,
                                    @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.connecthub.moderation.ReportRequest req) {
        var target = profiles.loadVisible(username, me.id());
        if (target.getId().equals(me.id())) {
            throw new com.connecthub.common.exception.BadRequestException("You can't report yourself");
        }
        reports.report(me.id(), com.connecthub.moderation.ReportTarget.user, target.getId(), req);
        return ApiResponse.ok();
    }
}
