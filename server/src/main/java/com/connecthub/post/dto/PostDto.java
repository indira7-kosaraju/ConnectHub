package com.connecthub.post.dto;

import com.connecthub.post.ReactionType;
import com.connecthub.user.dto.UserSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostDto(
        UUID id,
        UserSummary author,
        CircleRef circle,
        @Schema(nullable = true) String content,
        List<PostImageDto> images,
        Instant createdAt,
        @Schema(nullable = true) Instant editedAt,
        @Schema(nullable = true, description = "Set for the author while the 15-minute edit window is open")
        Instant editableUntil,
        VibeMix vibe,
        @Schema(nullable = true) ReactionType myReaction,
        int commentCount,
        boolean saved,
        boolean pinned,
        @Schema(description = "Author, or an admin of the post's circle") boolean canDelete,
        @Schema(description = "Viewer is an admin of the post's circle") boolean canPin) {
}
