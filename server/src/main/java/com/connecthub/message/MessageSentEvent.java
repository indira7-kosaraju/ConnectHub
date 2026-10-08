package com.connecthub.message;

import java.util.UUID;

/** Consumed by notifications. */
public record MessageSentEvent(UUID conversationId, UUID senderId, UUID recipientId) {
}
