package com.connecthub.moment;

import com.connecthub.auth.AuthUser;
import com.connecthub.common.ApiResponse;
import com.connecthub.common.CursorPage;
import com.connecthub.common.RateLimiter;
import com.connecthub.moment.dto.CreateMomentRequest;
import com.connecthub.moment.dto.MomentDto;
import com.connecthub.moment.dto.MomentGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Moments")
@RestController
@RequiredArgsConstructor
public class MomentController {

    private final MomentService moments;
    private final RateLimiter rateLimiter;

    @Operation(summary = "Share a moment for 24 hours (multipart: `moment` JSON + `image` for photo moments)")
    @PostMapping(path = "/api/v1/moments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MomentDto> create(@AuthenticationPrincipal AuthUser me,
                                         @Valid @RequestPart("moment") CreateMomentRequest req,
                                         @RequestPart(value = "image", required = false) MultipartFile image) {
        rateLimiter.consume(RateLimiter.Limit.MOMENT, me.id().toString());
        return ApiResponse.ok(moments.create(me.id(), req, image));
    }

    @Operation(summary = "The Today strip: live moments from you and people in your circles, grouped by person")
    @GetMapping("/api/v1/moments/strip")
    public ApiResponse<List<MomentGroup>> strip(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(moments.strip(me.id()));
    }

    @Operation(summary = "Mark a moment as seen (counted once per viewer; the count is visible only to the author)")
    @PostMapping("/api/v1/moments/{id}/view")
    public ApiResponse<Void> view(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        moments.view(id, me.id());
        return ApiResponse.ok();
    }

    @Operation(summary = "Delete one of your moments (live or in Rewind)")
    @DeleteMapping("/api/v1/moments/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        moments.delete(id, me.id());
        return ApiResponse.ok();
    }

    @Operation(summary = "Your Rewind: expired moments, newest first. Private to you.")
    @GetMapping("/api/v1/me/rewind")
    public ApiResponse<CursorPage<MomentDto>> rewind(@AuthenticationPrincipal AuthUser me,
                                                     @RequestParam(required = false) String cursor,
                                                     @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(moments.rewind(me.id(), cursor, limit));
    }
}
