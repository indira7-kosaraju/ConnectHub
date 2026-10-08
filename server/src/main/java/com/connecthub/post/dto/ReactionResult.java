package com.connecthub.post.dto;

import com.connecthub.post.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReactionResult(VibeMix vibe, @Schema(nullable = true) ReactionType myReaction) {
}
