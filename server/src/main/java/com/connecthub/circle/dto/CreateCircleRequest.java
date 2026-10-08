package com.connecthub.circle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCircleRequest(
        @NotBlank @Size(min = 3, max = 50) String name,
        @Size(max = 300) String description,
        @NotNull UUID categoryId,
        boolean privateCircle,
        @Min(2) @Max(150) Integer maxMembers) {
}
