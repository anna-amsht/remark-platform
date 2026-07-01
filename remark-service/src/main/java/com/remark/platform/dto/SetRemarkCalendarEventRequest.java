package com.remark.platform.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record SetRemarkCalendarEventRequest(
        @NotNull LocalDate workDate,
        @NotNull LocalTime workStartTime,
        @NotNull LocalTime workEndTime
) {
}
