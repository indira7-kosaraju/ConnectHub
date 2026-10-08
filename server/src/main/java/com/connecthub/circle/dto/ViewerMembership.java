package com.connecthub.circle.dto;

import com.connecthub.circle.CircleRole;
import com.connecthub.circle.MemberStatus;

/** The current viewer's relationship to a circle. */
public record ViewerMembership(CircleRole role, MemberStatus status) {
}
