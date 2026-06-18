package com.remark.platform.service;

import com.remark.platform.entity.Branch;
import com.remark.platform.dto.BranchResponse;
import com.remark.platform.exceptions.BranchNotFoundException;
import com.remark.platform.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchService {

    private final BranchRepository branchRepository;

    public List<BranchResponse> findActiveBranches() {
        return branchRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public BranchResponse findById(UUID id) {
        return branchRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BranchNotFoundException(id));
    }

    private BranchResponse toResponse(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getCode(),
                branch.getName(),
                branch.isActive()
        );
    }
}
