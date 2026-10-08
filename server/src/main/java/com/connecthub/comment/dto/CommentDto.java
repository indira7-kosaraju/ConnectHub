package com.connecthub.comment.dto;

import com.connecthub.post.ReactionType;
import com.connecthub.post.dto.VibeMix;
import com.connecthub.user.dto.UserSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentDto(
        UUID id,
        UUID postId,
        @Schema(nullable = true) UUID parentId,
        UserSummary author,
        String content,
        Instant createdAt,
        VibeMix vibe,
        @Schema(nullable = true) ReactionType myReaction,
        @Schema(description = "Comment author, post author, or circle admin") boolean canDelete,
        @Schema(description = "Replies (top-level comments only; always empty for replies)") List<CommentDto> replies) {
}
