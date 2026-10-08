package com.connecthub.post.dto;

import java.util.UUID;

public record CircleRef(UUID id, String slug, String name, boolean privateCircle) {
}
