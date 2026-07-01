package com.remark.platform.repository;

import com.remark.platform.entity.Remark;
import com.remark.platform.entity.RemarkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RemarkRepository extends JpaRepository<Remark, UUID> {

    @Query("""
            SELECT r FROM Remark r
            WHERE (:branchId IS NULL OR r.branchId = :branchId)
              AND (:status IS NULL OR r.status = :status)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(r.remarkNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(r.objectBuilding) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY r.createdAt DESC
            """)
    Page<Remark> searchRemarks(
            @Param("branchId") UUID branchId,
            @Param("status") RemarkStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT r FROM Remark r
            WHERE r.assignedMasterId = :masterId
              AND (:status IS NULL OR r.status = :status)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(r.remarkNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(r.objectBuilding) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY r.createdAt DESC
            """)
    Page<Remark> searchRemarksByMaster(
            @Param("masterId") UUID masterId,
            @Param("status") RemarkStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT r FROM Remark r
            WHERE r.assignedMasterId = :masterId
            ORDER BY r.createdAt DESC
            """)
    List<Remark> findByAssignedMasterIdOrderByCreatedAtDesc(@Param("masterId") UUID masterId);

    @Query("""
            SELECT r FROM Remark r
            WHERE r.assignedMasterId = :masterId
              AND r.estimatedResolutionAt IS NOT NULL
              AND r.status IN :statuses
              AND r.estimatedResolutionAt >= :from
              AND r.estimatedResolutionAt < :to
            ORDER BY r.estimatedResolutionAt ASC
            """)
    List<Remark> findCalendarEventsByMaster(
            @Param("masterId") UUID masterId,
            @Param("statuses") List<RemarkStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
