package com.connecthub.post.dto;

import com.connecthub.post.ReactionType;
import jakarta.validation.constraints.NotNull;

public record ReactionRequest(@NotNull ReactionType type) {
}
