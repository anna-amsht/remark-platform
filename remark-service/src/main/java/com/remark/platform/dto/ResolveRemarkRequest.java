package com.remark.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolveRemarkRequest(
        @NotBlank String resolutionMessage
) {
}
