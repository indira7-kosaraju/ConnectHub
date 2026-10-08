package com.connecthub.message.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Text and/or a shared post. {@code clientId} is echoed back for optimistic reconciliation. */
public record SendMessageRequest(@Size(max = 2000) String content, UUID sharedPostId, @Size(max = 64) String clientId) {
}
