package com.connecthub.circle.dto;

import com.connecthub.user.dto.InterestDto;
import com.connecthub.user.dto.UserSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CircleDetail(
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
        @Schema(nullable = true) UUID pinnedPostId,
        Instant createdAt,
        List<UserSummary> admins,
        @Schema(nullable = true, description = "Null when the viewer is anonymous or not a member")
        ViewerMembership viewer,
        @Schema(description = "Whether the viewer may see the feed and member list")
        boolean canViewContent,
        @Schema(description = "Pending join requests (admins only, else 0)")
        long pendingCount) {
}
