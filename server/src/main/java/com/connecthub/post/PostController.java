package com.connecthub.post;

import com.connecthub.auth.AuthUser;
import com.connecthub.common.ApiResponse;
import com.connecthub.common.RateLimiter;
import com.connecthub.moderation.ReportRequest;
import com.connecthub.post.dto.CreatePostRequest;
import com.connecthub.post.dto.PostDto;
import com.connecthub.post.dto.ReactionRequest;
import com.connecthub.post.dto.ReactionResult;
import com.connecthub.post.dto.SavedResult;
import com.connecthub.post.dto.UpdatePostRequest;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Posts")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService posts;
    private final RateLimiter rateLimiter;

    @Operation(summary = "Create a post (multipart: `post` JSON part + up to 4 `images`). 10 posts / 15 min.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostDto> create(@AuthenticationPrincipal AuthUser me,
                                       @Valid @RequestPart("post") CreatePostRequest req,
                                       @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        rateLimiter.consume(RateLimiter.Limit.POST, me.id().toString());
        return ApiResponse.ok(posts.create(me.id(), req, images));
    }

    @Operation(summary = "A single post (public for public circles)")
    @GetMapping("/{id}")
    public ApiResponse<PostDto> get(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return ApiResponse.ok(posts.get(id, me == null ? null : me.id()));
    }

    @Operation(summary = "Edit your post's text (within 15 minutes of posting)")
    @PatchMapping("/{id}")
    public ApiResponse<PostDto> update(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                       @Valid @RequestBody UpdatePostRequest req) {
        return ApiResponse.ok(posts.update(id, me.id(), req));
    }

    @Operation(summary = "Delete a post (author or circle admin)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        posts.delete(id, me.id());
        return ApiResponse.ok();
    }

    @Operation(summary = "Set your vibe on a post (one per user; replaces any previous one)")
    @PutMapping("/{id}/reaction")
    public ApiResponse<ReactionResult> react(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                             @Valid @RequestBody ReactionRequest req) {
        return ApiResponse.ok(posts.react(id, me.id(), req.type()));
    }

    @Operation(summary = "Remove your vibe")
    @DeleteMapping("/{id}/reaction")
    public ApiResponse<ReactionResult> unreact(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return ApiResponse.ok(posts.unreact(id, me.id()));
    }

    @Operation(summary = "Save (bookmark) a post")
    @PutMapping("/{id}/save")
    public ApiResponse<SavedResult> save(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return ApiResponse.ok(posts.setSaved(id, me.id(), true));
    }

    @Operation(summary = "Remove a post from your saved list")
    @DeleteMapping("/{id}/save")
    public ApiResponse<SavedResult> unsave(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return ApiResponse.ok(posts.setSaved(id, me.id(), false));
    }

    @Operation(summary = "Report a post")
    @PostMapping("/{id}/report")
    public ApiResponse<Void> report(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                    @Valid @RequestBody ReportRequest req) {
        posts.report(id, me.id(), req);
        return ApiResponse.ok();
    }
}
