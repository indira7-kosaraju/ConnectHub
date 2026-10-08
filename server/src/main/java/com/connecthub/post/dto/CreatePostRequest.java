package com.connecthub.post.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** JSON part of the multipart create request; images travel as separate {@code images} parts. */
public record CreatePostRequest(@NotNull UUID circleId, @Size(max = 500) String content) {
}
