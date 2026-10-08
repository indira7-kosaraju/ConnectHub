package com.connecthub.feed;

import com.connecthub.auth.AuthUser;
import com.connecthub.common.ApiResponse;
import com.connecthub.common.CursorPage;
import com.connecthub.post.dto.PostDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Feed")
@RestController
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feed;

    @Operation(summary = "Home feed from your circles, ranked by recency + vibes + comments. cursor = <rankTime>,<id>")
    @GetMapping("/api/v1/feed")
    public ApiResponse<CursorPage<PostDto>> feed(@AuthenticationPrincipal AuthUser me,
                                                 @RequestParam(required = false) String cursor,
                                                 @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(feed.feed(me.id(), cursor, limit));
    }
}
