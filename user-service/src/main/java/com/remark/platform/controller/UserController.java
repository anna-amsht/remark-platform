package com.remark.platform.controller;

import com.remark.platform.dto.RegisterRequest;
import com.remark.platform.dto.UserResponse;
import com.remark.platform.entity.RoleCode;
import com.remark.platform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @GetMapping("/pending")
    public List<UserResponse> findPending(@RequestHeader(USER_ID_HEADER) UUID approverId) {
        return userService.findPendingApprovals(approverId);
    }

    @GetMapping("/managed")
    public List<UserResponse> findManaged(@RequestHeader(USER_ID_HEADER) UUID managerId) {
        return userService.findManagedEmployees(managerId);
    }

    @PatchMapping("/{id}/approve")
    public UserResponse approve(
            @PathVariable UUID id,
            @RequestHeader(USER_ID_HEADER) UUID approverId
    ) {
        return userService.approve(id, approverId);
    }

    @PatchMapping("/{id}/reject")
    public UserResponse reject(
            @PathVariable UUID id,
            @RequestHeader(USER_ID_HEADER) UUID approverId
    ) {
        return userService.reject(id, approverId);
    }

    @PatchMapping("/{id}/dismiss")
    public UserResponse dismiss(
            @PathVariable UUID id,
            @RequestHeader(USER_ID_HEADER) UUID managerId
    ) {
        return userService.dismiss(id, managerId);
    }

    @GetMapping("/me")
    public UserResponse findMe(@RequestHeader(USER_ID_HEADER) UUID userId) {
        return userService.findById(userId);
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable UUID id) {
        return userService.findById(id);
    }

    @GetMapping
    public List<UserResponse> findByBranchAndRole(
            @RequestParam UUID branchId,
            @RequestParam RoleCode role
    ) {
        return userService.findActiveByBranchAndRole(branchId, role);
    }
}
