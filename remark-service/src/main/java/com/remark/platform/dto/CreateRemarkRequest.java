package com.remark.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateRemarkRequest(
        @NotBlank @Size(max = 500) String objectBuilding,
        @NotBlank String description,
        @NotNull LocalDate discoveredAt
) {
}
