package com.connecthub.circle.event;

import java.util.UUID;

/** An admin approved a pending join request. Consumed by notifications (Step 6). */
public record CircleJoinApprovedEvent(UUID circleId, UUID userId, UUID approvedBy) {
}
