package com.connecthub.message.dto;

import com.connecthub.user.dto.UserSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record ConversationDto(
        UUID id,
        UserSummary other,
        @Schema(nullable = true) MessageDto lastMessage,
        int unreadCount,
        @Schema(nullable = true) Instant lastMessageAt,
        @Schema(nullable = true, description = "When the other person last read the conversation") Instant otherLastReadAt) {
}
