package com.connecthub.circle.event;

import java.util.UUID;

/** A user asked to join a private circle. Consumed by notifications (Step 6). */
public record CircleJoinRequestedEvent(UUID circleId, UUID requesterId) {
}
