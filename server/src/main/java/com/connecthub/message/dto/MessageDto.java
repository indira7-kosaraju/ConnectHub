package com.connecthub.message.dto;

import com.connecthub.post.dto.PostDto;
import com.connecthub.user.dto.UserSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID conversationId,
        UUID senderId,
        UserSummary sender,
        @Schema(nullable = true) String content,
        @Schema(nullable = true, description = "The shared post, if the viewer can read it") PostDto sharedPost,
        @Schema(description = "A post was shared but the viewer can't see it (private circle or deleted)")
        boolean sharedPostUnavailable,
        Instant createdAt,
        @Schema(nullable = true) Instant readAt,
        @Schema(nullable = true, description = "Echo of the sender's client-generated id, for optimistic UI") String clientId) {
}
