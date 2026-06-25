package com.remark.platform.entity;

import java.time.temporal.ChronoUnit;

public enum EstimatedResolutionUnit {
    HOURS(ChronoUnit.HOURS),
    DAYS(ChronoUnit.DAYS),
    WEEKS(ChronoUnit.WEEKS);

    private final ChronoUnit chronoUnit;

    EstimatedResolutionUnit(ChronoUnit chronoUnit) {
        this.chronoUnit = chronoUnit;
    }

    public ChronoUnit chronoUnit() {
        return chronoUnit;
    }
}
