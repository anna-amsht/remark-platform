package com.remark.platform.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateReportRequest(
        @NotNull UUID remarkId
) {
}
