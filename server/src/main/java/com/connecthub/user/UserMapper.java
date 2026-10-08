package com.connecthub.user;

import com.connecthub.user.dto.InterestDto;
import com.connecthub.user.dto.MeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Mapper
public interface UserMapper {

    @Mapping(target = "hasPassword", expression = "java(user.hasPassword())")
    @Mapping(target = "interests", expression = "java(toInterestDtos(user.getInterests()))")
    MeResponse toMe(User user);

    InterestDto toDto(Interest interest);

    default List<InterestDto> toInterestDtos(Collection<Interest> interests) {
        return interests.stream()
                .sorted(Comparator.comparing(Interest::getName))
                .map(this::toDto)
                .toList();
    }
}
