package com.connecthub.circle;

import com.connecthub.auth.AuthUser;
import com.connecthub.circle.dto.CircleDetail;
import com.connecthub.circle.dto.CircleSummary;
import com.connecthub.circle.dto.CreateCircleRequest;
import com.connecthub.circle.dto.JoinResult;
import com.connecthub.circle.dto.MemberDto;
import com.connecthub.circle.dto.UpdateCircleRequest;
import com.connecthub.common.ApiResponse;
import com.connecthub.common.PageResponse;
import com.connecthub.common.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Circles")
@RestController
@RequestMapping("/api/v1/circles")
@RequiredArgsConstructor
public class CircleController {

    private final CircleService circles;
    private final com.connecthub.post.PostService posts;
    private final RateLimiter rateLimiter;

    @Operation(summary = "Browse circles (public). Filter by category slug and/or text query; largest first.")
    @GetMapping
    public ApiResponse<PageResponse<CircleSummary>> browse(@AuthenticationPrincipal AuthUser me,
                                                          @RequestParam(required = false) String category,
                                                          @RequestParam(required = false) String q,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.ok(circles.browse(category, q, page, size, me == null ? null : me.id()));
    }

    @Operation(summary = "Trending circles (public): recent joins weighted over size. Excludes circles you're in.")
    @GetMapping("/discover/trending")
    public ApiResponse<List<CircleSummary>> trending(@AuthenticationPrincipal AuthUser me,
                                                     @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok(circles.trending(me == null ? null : me.id(), limit));
    }

    @Operation(summary = "Create a circle; you become its admin")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CircleDetail> create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateCircleRequest req) {
        rateLimiter.consume(RateLimiter.Limit.CIRCLE_CREATE, me.id().toString());
        return ApiResponse.ok(circles.create(me.id(), req));
    }

    @Operation(summary = "Circle details (public). Includes your membership when signed in.")
    @GetMapping("/{slug}")
    public ApiResponse<CircleDetail> get(@AuthenticationPrincipal AuthUser me, @PathVariable String slug) {
        return ApiResponse.ok(circles.get(slug, me == null ? null : me.id()));
    }

    @Operation(summary = "Edit a circle (admins)")
    @PatchMapping("/{slug}")
    public ApiResponse<CircleDetail> update(@AuthenticationPrincipal AuthUser me, @PathVariable String slug,
                                            @Valid @RequestBody UpdateCircleRequest req) {
        return ApiResponse.ok(circles.update(slug, me.id(), req));
    }

    @Operation(summary = "Upload a cover image (admins; cropped to 1200×400)")
    @PostMapping(path = "/{slug}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CircleDetail> cover(@AuthenticationPrincipal AuthUser me, @PathVariable String slug,
                                           @RequestPart("file") MultipartFile file) {
        rateLimiter.consume(RateLimiter.Limit.UPLOAD, me.id().toString());
        return ApiResponse.ok(circles.uploadCover(slug, me.id(), file));
    }

    @Operation(summary = "Join a public circle, or request to join a private one")
    @PostMapping("/{slug}/join")
    public ApiResponse<JoinResult> join(@AuthenticationPrincipal AuthUser me, @PathVariable String slug) {
        return ApiResponse.ok(circles.join(slug, me.id()));
    }

    @Operation(summary = "Leave a circle or cancel a pending request")
    @DeleteMapping("/{slug}/membership")
    public ApiResponse<Void> leave(@AuthenticationPrincipal AuthUser me, @PathVariable String slug) {
        circles.leave(slug, me.id());
        return ApiResponse.ok();
    }

    @Operation(summary = "Members (status=active, visible to members or anyone for public circles) or join requests (status=pending, admins)")
    @GetMapping("/{slug}/members")
    public ApiResponse<PageResponse<MemberDto>> members(@AuthenticationPrincipal AuthUser me, @PathVariable String slug,
                                                        @RequestParam(defaultValue = "active") MemberStatus status,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.ok(circles.members(slug, me == null ? null : me.id(), status, page, size));
    }

    @Operation(summary = "Approve a join request (admins)")
    @PostMapping("/{slug}/members/{userId}/approve")
    public ApiResponse<MemberDto> approve(@AuthenticationPrincipal AuthUser me, @PathVariable String slug, @PathVariable UUID userId) {
        return ApiResponse.ok(circles.approve(slug, me.id(), userId));
    }

    @Operation(summary = "Remove a member or decline a join request (admins)")
    @DeleteMapping("/{slug}/members/{userId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal AuthUser me, @PathVariable String slug, @PathVariable UUID userId) {
        circles.removeMember(slug, me.id(), userId);
        return ApiResponse.ok();
    }

    @Operation(summary = "A circle's posts, newest first (public circles: anyone; private: members). Keyset cursor.")
    @GetMapping("/{slug}/posts")
    public ApiResponse<com.connecthub.common.CursorPage<com.connecthub.post.dto.PostDto>> posts(
            @AuthenticationPrincipal AuthUser me, @PathVariable String slug,
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(posts.circlePosts(slug, me == null ? null : me.id(), cursor, limit));
    }

    @Operation(summary = "Pin a post to the top of the circle (admins)")
    @PutMapping("/{slug}/pin")
    public ApiResponse<CircleDetail> pin(@AuthenticationPrincipal AuthUser me, @PathVariable String slug,
                                         @Valid @RequestBody PinRequest req) {
        return ApiResponse.ok(circles.pin(slug, me.id(), req.postId()));
    }

    @Operation(summary = "Unpin (admins)")
    @DeleteMapping("/{slug}/pin")
    public ApiResponse<CircleDetail> unpin(@AuthenticationPrincipal AuthUser me, @PathVariable String slug) {
        return ApiResponse.ok(circles.unpin(slug, me.id()));
    }

    public record PinRequest(@jakarta.validation.constraints.NotNull UUID postId) {
    }
}
