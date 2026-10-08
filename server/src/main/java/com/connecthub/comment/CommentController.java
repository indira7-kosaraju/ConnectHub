package com.connecthub.comment;

import com.connecthub.auth.AuthUser;
import com.connecthub.comment.dto.CommentDto;
import com.connecthub.comment.dto.CreateCommentRequest;
import com.connecthub.common.ApiResponse;
import com.connecthub.common.CursorPage;
import com.connecthub.common.RateLimiter;
import com.connecthub.post.dto.ReactionRequest;
import com.connecthub.post.dto.ReactionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Comments")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService comments;
    private final RateLimiter rateLimiter;

    @Operation(summary = "Comments on a post, oldest first, each with its replies")
    @GetMapping("/api/v1/posts/{postId}/comments")
    public ApiResponse<CursorPage<CommentDto>> list(@AuthenticationPrincipal AuthUser me, @PathVariable UUID postId,
                                                    @RequestParam(required = false) String cursor,
                                                    @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(comments.list(postId, me == null ? null : me.id(), cursor, limit));
    }

    @Operation(summary = "Comment, or reply with parentId (one level of nesting)")
    @PostMapping("/api/v1/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentDto> create(@AuthenticationPrincipal AuthUser me, @PathVariable UUID postId,
                                          @Valid @RequestBody CreateCommentRequest req) {
        rateLimiter.consume(RateLimiter.Limit.COMMENT, me.id().toString());
        return ApiResponse.ok(comments.create(postId, me.id(), req));
    }

    @Operation(summary = "Delete a comment (its author, the post author, or a circle admin)")
    @DeleteMapping("/api/v1/comments/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        comments.delete(id, me.id());
        return ApiResponse.ok();
    }

    @Operation(summary = "Report a comment")
    @PostMapping("/api/v1/comments/{id}/report")
    public ApiResponse<Void> report(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                    @Valid @RequestBody com.connecthub.moderation.ReportRequest req) {
        comments.report(id, me.id(), req);
        return ApiResponse.ok();
    }

    @Operation(summary = "Set your vibe on a comment")
    @PutMapping("/api/v1/comments/{id}/reaction")
    public ApiResponse<ReactionResult> react(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                             @Valid @RequestBody ReactionRequest req) {
        return ApiResponse.ok(comments.react(id, me.id(), req.type()));
    }

    @Operation(summary = "Remove your vibe from a comment")
    @DeleteMapping("/api/v1/comments/{id}/reaction")
    public ApiResponse<ReactionResult> unreact(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return ApiResponse.ok(comments.unreact(id, me.id()));
    }
}
