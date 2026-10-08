package com.connecthub.circle.dto;

/** Outcome of a join: {@code active} for public circles, {@code pending} for private ones. */
public record JoinResult(ViewerMembership membership, int memberCount) {
}
