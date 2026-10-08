package com.connecthub.search;

import com.connecthub.auth.AuthUser;
import com.connecthub.common.ApiResponse;
import com.connecthub.search.dto.SearchResults;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Search")
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService search;

    @Operation(summary = "Search people, circles and posts (full-text, prefix-matched, ranked). type = all|people|circles|posts")
    @GetMapping("/api/v1/search")
    public ApiResponse<SearchResults> search(@AuthenticationPrincipal AuthUser me,
                                             @RequestParam String q,
                                             @RequestParam(defaultValue = "all") SearchService.Scope type,
                                             @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok(search.search(q, type, limit, me.id()));
    }
}
