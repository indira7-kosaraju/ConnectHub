package com.connecthub.user;

import com.connecthub.common.ApiResponse;
import com.connecthub.user.dto.InterestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Interests")
@SecurityRequirements
@RestController
@RequiredArgsConstructor
public class InterestController {

    private final InterestRepository interests;
    private final UserMapper mapper;

    @Operation(summary = "All interests, alphabetical")
    @GetMapping("/api/v1/interests")
    public ApiResponse<List<InterestDto>> list() {
        return ApiResponse.ok(interests.findAllByOrderByNameAsc().stream().map(mapper::toDto).toList());
    }
}
