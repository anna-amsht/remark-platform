package com.remark.platform.controller;

import com.remark.platform.dto.BranchResponse;
import com.remark.platform.exceptions.BranchNotFoundException;
import com.remark.platform.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/refs/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public List<BranchResponse> listBranches() {
        return branchService.findActiveBranches();
    }

    @GetMapping("/{id}")
    public BranchResponse getBranch(@PathVariable UUID id) {
        return branchService.findById(id);
    }

    @ExceptionHandler(BranchNotFoundException.class)
    public ProblemDetail handleNotFound(BranchNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
