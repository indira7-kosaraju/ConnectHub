package com.connecthub.message.dto;

import java.time.Instant;
import java.util.UUID;

/** Pushed to the other participant when someone reads a conversation. */
public record ReadReceipt(UUID conversationId, UUID readerId, Instant readAt) {
}
