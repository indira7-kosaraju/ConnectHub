package com.connecthub.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record InterestDto(UUID id, String slug, String name, @Schema(nullable = true) String emoji) {
}
