package com.connecthub.message.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Inbound STOMP frame on /app/chat.send. */
public record ChatSendFrame(@NotNull UUID conversationId, @Size(max = 2000) String content, UUID sharedPostId,
                            @Size(max = 64) String clientId) {
}
