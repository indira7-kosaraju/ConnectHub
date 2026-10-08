package com.connecthub.circle.dto;

import com.connecthub.circle.MemberStatus;
import com.connecthub.user.dto.InterestDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Card-sized view of a circle (lists, suggestions, discover). {@code viewerStatus} is null for non-members and anonymous viewers. */
public record CircleSummary(
        UUID id,
        String slug,
        String name,
        @Schema(nullable = true) String description,
        @Schema(nullable = true) String coverUrl,
        InterestDto category,
        boolean privateCircle,
        int memberCount,
        int maxMembers,
        boolean full,
        @Schema(nullable = true) MemberStatus viewerStatus) {

    public CircleSummary withViewerStatus(MemberStatus status) {
        return new CircleSummary(id, slug, name, description, coverUrl, category, privateCircle, memberCount, maxMembers, full, status);
    }
}
