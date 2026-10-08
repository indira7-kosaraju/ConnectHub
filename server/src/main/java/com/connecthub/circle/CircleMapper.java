package com.connecthub.circle;

import com.connecthub.circle.dto.CircleSummary;
import com.connecthub.circle.dto.MemberDto;
import com.connecthub.user.User;
import com.connecthub.user.UserMapper;
import com.connecthub.user.dto.UserSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = UserMapper.class)
public interface CircleMapper {

    @Mapping(target = "full", expression = "java(circle.isFull())")
    @Mapping(target = "viewerStatus", ignore = true)
    CircleSummary toSummary(Circle circle);

    UserSummary toUserSummary(User user);

    @Mapping(target = "since", source = "createdAt")
    MemberDto toMember(CircleMember member);
}
