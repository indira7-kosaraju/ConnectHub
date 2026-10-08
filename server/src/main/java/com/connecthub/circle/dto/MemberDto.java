package com.connecthub.circle.dto;

import com.connecthub.circle.CircleRole;
import com.connecthub.circle.MemberStatus;
import com.connecthub.user.dto.UserSummary;

import java.time.Instant;

public record MemberDto(UserSummary user, CircleRole role, MemberStatus status, Instant since) {
}
