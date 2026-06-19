package com.remark.platform.service;

import com.remark.platform.dto.RemarkResponse;
import com.remark.platform.entity.Remark;

final class RemarkMapper {

    private RemarkMapper() {
    }

    static RemarkResponse toResponse(Remark remark) {
        return new RemarkResponse(
                remark.getId(),
                remark.getRemarkNumber(),
                remark.getBranchId(),
                remark.getObjectBuilding(),
                remark.getDescription(),
                remark.getDiscoveredAt(),
                remark.getCreatedByUserId(),
                remark.getAssignedMasterId(),
                remark.getStatus(),
                remark.getResolutionMessage(),
                remark.getResolvedAt(),
                remark.getClosedAt(),
                remark.getReportSentAt(),
                remark.getCreatedAt(),
                remark.getUpdatedAt()
        );
    }
}
