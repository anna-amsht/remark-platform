package com.remark.platform.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SetEstimatedResolutionRequest(
        @NotNull LocalDateTime estimatedResolutionAt
) {
}
