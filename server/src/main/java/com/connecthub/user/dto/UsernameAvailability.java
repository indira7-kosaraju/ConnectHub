package com.connecthub.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
/** {@code reason} is null when available, otherwise one of INVALID, RESERVED, TAKEN. */
public record UsernameAvailability(String username, boolean available, @Schema(nullable = true) String reason) {
}
