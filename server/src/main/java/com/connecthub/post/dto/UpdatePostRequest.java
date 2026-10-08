package com.connecthub.post.dto;

import jakarta.validation.constraints.Size;

public record UpdatePostRequest(@Size(max = 500) String content) {
}
