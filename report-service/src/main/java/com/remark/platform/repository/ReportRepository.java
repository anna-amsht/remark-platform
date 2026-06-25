package com.remark.platform.repository;

import com.remark.platform.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findAllByOrderByCreatedAtDesc();
}
