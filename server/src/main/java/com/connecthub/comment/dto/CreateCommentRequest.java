package com.connecthub.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** {@code parentId} replies to a comment; replying to a reply attaches to its top-level parent. */
public record CreateCommentRequest(@NotBlank @Size(max = 500) String content, UUID parentId) {
}
