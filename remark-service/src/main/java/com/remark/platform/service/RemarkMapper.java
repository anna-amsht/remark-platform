package com.remark.platform.service;

import com.remark.platform.dto.InternalRemarkResponse;
import com.remark.platform.dto.RemarkResponse;
import com.remark.platform.entity.Remark;

final class RemarkMapper {

    private RemarkMapper() {
    }

    static InternalRemarkResponse toInternalResponse(Remark remark) {
        return new InternalRemarkResponse(
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
                remark.getReportSentAt()
        );
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
                remark.getEstimatedResolutionDuration(),
                remark.getEstimatedResolutionUnit(),
                remark.getEstimatedResolutionAt(),
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
