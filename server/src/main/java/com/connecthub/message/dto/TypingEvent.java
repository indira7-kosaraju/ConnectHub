package com.connecthub.message.dto;

import java.util.UUID;

/** Inbound on /app/chat.typing (userId ignored) and pushed to the other participant on /user/queue/typing. */
public record TypingEvent(UUID conversationId, UUID userId, boolean typing) {
}
