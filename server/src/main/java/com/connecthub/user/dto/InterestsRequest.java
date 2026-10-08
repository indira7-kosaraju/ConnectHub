package com.connecthub.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record InterestsRequest(
        @NotNull
        @Size(min = 3, max = 10, message = "Pick between 3 and 10 interests")
        Set<UUID> interestIds) {
}
