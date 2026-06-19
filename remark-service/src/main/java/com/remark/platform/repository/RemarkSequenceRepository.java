package com.remark.platform.repository;

import com.remark.platform.entity.RemarkSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RemarkSequenceRepository extends JpaRepository<RemarkSequence, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM RemarkSequence s WHERE s.year = :year")
    Optional<RemarkSequence> findByYearForUpdate(@Param("year") int year);
}
