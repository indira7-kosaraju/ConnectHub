package com.connecthub.search.dto;

import com.connecthub.circle.dto.CircleSummary;
import com.connecthub.post.dto.PostDto;
import com.connecthub.user.dto.UserSummary;

import java.util.List;

public record SearchResults(List<UserSummary> people, List<CircleSummary> circles, List<PostDto> posts) {

    public static final SearchResults EMPTY = new SearchResults(List.of(), List.of(), List.of());
}
