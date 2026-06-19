package com.remark.platform.repository;

import com.remark.platform.entity.RemarkStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RemarkStatusHistoryRepository extends JpaRepository<RemarkStatusHistory, UUID> {

    List<RemarkStatusHistory> findByRemarkIdOrderByChangedAtAsc(UUID remarkId);
}
