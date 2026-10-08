package com.connecthub.moment.dto;

import com.connecthub.user.dto.UserSummary;

import java.util.List;

/** One person's live moments, in the order they were shared. */
public record MomentGroup(UserSummary user, boolean mine, boolean allSeen, List<MomentDto> moments) {
}
