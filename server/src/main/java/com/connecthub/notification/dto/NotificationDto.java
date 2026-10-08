package com.connecthub.notification.dto;

import com.connecthub.notification.NotificationType;
import com.connecthub.post.ReactionType;
import com.connecthub.user.dto.UserSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        NotificationType type,
        @Schema(nullable = true) UserSummary actor,
        @Schema(nullable = true) UUID postId,
        @Schema(nullable = true, description = "First words of the post") String postSnippet,
        @Schema(nullable = true) UUID commentId,
        @Schema(nullable = true, description = "First words of the comment or reply") String commentSnippet,
        @Schema(nullable = true) String circleSlug,
        @Schema(nullable = true) String circleName,
        @Schema(nullable = true, description = "For reaction notifications: the vibe the actor chose") ReactionType reaction,
        boolean read,
        Instant createdAt) {
}
