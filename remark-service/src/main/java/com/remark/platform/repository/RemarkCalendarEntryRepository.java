package com.remark.platform.repository;

import com.remark.platform.entity.RemarkCalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RemarkCalendarEntryRepository extends JpaRepository<RemarkCalendarEntry, UUID> {

    List<RemarkCalendarEntry> findByMasterIdAndWorkDateGreaterThanEqualAndWorkDateLessThan(
            UUID masterId,
            LocalDate from,
            LocalDate to
    );

    Optional<RemarkCalendarEntry> findByRemarkIdAndMasterIdAndWorkDate(
            UUID remarkId,
            UUID masterId,
            LocalDate workDate
    );
}
