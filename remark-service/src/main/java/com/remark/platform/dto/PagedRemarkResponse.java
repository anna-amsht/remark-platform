package com.remark.platform.dto;

import java.util.List;

public record PagedRemarkResponse(
        List<RemarkResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
