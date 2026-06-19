package com.remark.platform.repository;

import com.remark.platform.entity.Remark;
import com.remark.platform.entity.RemarkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RemarkRepository extends JpaRepository<Remark, UUID> {

    @Query("""
            SELECT r FROM Remark r
            WHERE r.branchId = :branchId
            ORDER BY r.createdAt DESC
            """)
    List<Remark> findByBranchIdOrderByCreatedAtDesc(@Param("branchId") UUID branchId);

    @Query("""
            SELECT r FROM Remark r
            WHERE r.assignedMasterId = :masterId
            ORDER BY r.createdAt DESC
            """)
    List<Remark> findByAssignedMasterIdOrderByCreatedAtDesc(@Param("masterId") UUID masterId);

    @Query("""
            SELECT r FROM Remark r
            WHERE r.branchId = :branchId
              AND r.status = :status
            ORDER BY r.createdAt DESC
            """)
    List<Remark> findByBranchIdAndStatusOrderByCreatedAtDesc(
            @Param("branchId") UUID branchId,
            @Param("status") RemarkStatus status
    );

    @Query("""
            SELECT r FROM Remark r
            ORDER BY r.createdAt DESC
            """)
    List<Remark> findAllOrderByCreatedAtDesc();
}
