package com.remark.platform.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignMasterRequest(
        @NotNull UUID masterId
) {
}
