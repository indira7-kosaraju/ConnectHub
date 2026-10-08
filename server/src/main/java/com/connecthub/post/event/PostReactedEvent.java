package com.connecthub.post.event;

import com.connecthub.post.ReactionType;

import java.util.UUID;

/** A user newly reacted to a post (not fired for changing an existing reaction). Consumed in Step 6. */
public record PostReactedEvent(UUID postId, UUID postAuthorId, UUID actorId, ReactionType type) {
}
