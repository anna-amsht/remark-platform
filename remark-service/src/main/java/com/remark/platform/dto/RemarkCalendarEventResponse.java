package com.remark.platform.dto;

import com.remark.platform.entity.RemarkStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record RemarkCalendarEventResponse(
        UUID remarkId,
        String remarkNumber,
        String title,
        String objectBuilding,
        RemarkStatus status,
        LocalDate workDate,
        LocalTime workStartTime,
        LocalTime workEndTime,
        LocalDateTime start,
        LocalDateTime end,
        LocalDateTime estimatedResolutionAt,
        boolean customTime
) {
}
