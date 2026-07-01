package com.remark.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "remarks", schema = "remark")
@Getter
@Setter
public class Remark {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "remark_number", nullable = false, unique = true, length = 32)
    private String remarkNumber;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "object_building", nullable = false, length = 500)
    private String objectBuilding;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "discovered_at", nullable = false)
    private LocalDate discoveredAt;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "assigned_master_id")
    private UUID assignedMasterId;

    @Column(name = "estimated_resolution_duration")
    private Integer estimatedResolutionDuration;

    @Enumerated(EnumType.STRING)
    @Column(name = "estimated_resolution_unit", length = 16)
    private EstimatedResolutionUnit estimatedResolutionUnit;

    @Column(name = "estimated_resolution_at")
    private LocalDateTime estimatedResolutionAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RemarkStatus status;

    @Column(name = "resolution_message", columnDefinition = "TEXT")
    private String resolutionMessage;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "report_sent_at")
    private Instant reportSentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
