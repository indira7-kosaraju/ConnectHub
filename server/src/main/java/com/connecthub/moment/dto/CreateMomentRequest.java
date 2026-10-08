package com.connecthub.moment.dto;

import com.connecthub.moment.MomentBackground;
import com.connecthub.moment.MomentKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** JSON part of the multipart create request. Image moments send the photo as the {@code image} part. */
public record CreateMomentRequest(@NotNull MomentKind kind, @Size(max = 200) String text, MomentBackground background) {
}
