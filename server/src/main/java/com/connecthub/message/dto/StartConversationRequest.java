package com.connecthub.message.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartConversationRequest(@NotNull UUID userId) {
}
