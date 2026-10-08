package com.connecthub.comment.event;

import java.util.UUID;

/** Consumed by notifications in Step 6 (comment → post author; reply → parent comment author). */
public record CommentCreatedEvent(UUID commentId, UUID postId, UUID postAuthorId, UUID parentAuthorId, UUID authorId) {
}
