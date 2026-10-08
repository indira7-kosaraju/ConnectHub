package com.connecthub.circle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Partial update: null fields are left unchanged. */
public record UpdateCircleRequest(
        @Size(min = 3, max = 50) String name,
        @Size(max = 300) String description,
        UUID categoryId,
        Boolean privateCircle,
        @Min(2) @Max(150) Integer maxMembers) {
}
