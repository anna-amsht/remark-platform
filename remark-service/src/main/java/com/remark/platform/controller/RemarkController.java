package com.remark.platform.controller;

import com.remark.platform.dto.AssignMasterRequest;
import com.remark.platform.dto.CreateRemarkRequest;
import com.remark.platform.dto.RemarkCalendarEventResponse;
import com.remark.platform.dto.RemarkResponse;
import com.remark.platform.dto.ResolveRemarkRequest;
import com.remark.platform.dto.SetRemarkCalendarEventRequest;
import com.remark.platform.dto.SetEstimatedResolutionRequest;
import com.remark.platform.entity.RemarkStatus;
import com.remark.platform.security.UserContext;
import com.remark.platform.service.RemarkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/remarks")
public class RemarkController {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String BRANCH_ID_HEADER = "X-Branch-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    private final RemarkService remarkService;

    public RemarkController(RemarkService remarkService) {
        this.remarkService = remarkService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RemarkResponse create(
            @Valid @RequestBody CreateRemarkRequest request,
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(value = BRANCH_ID_HEADER, required = false) UUID branchId,
            @RequestHeader(USER_ROLES_HEADER) String roles
    ) {
        UserContext actor = UserContext.fromHeaders(userId, branchId, roles);
        return remarkService.create(request, actor);
    }

    @GetMapping
    public List<RemarkResponse> findAll(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(value = BRANCH_ID_HEADER, required = false) UUID branchId,
            @RequestHeader(USER_ROLES_HEADER) String roles,
            @RequestParam(required = false) RemarkStatus status,
            @RequestParam(required = false) UUID branchIdFilter
    ) {
        UserContext actor = UserContext.fromHeaders(userId, branchId, roles);
        return remarkService.findAll(actor, status, branchIdFilter);
    }

    @GetMapping("/calendar")
    public List<RemarkCalendarEventResponse> findCalendarEvents(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(value = BRANCH_ID_HEADER, required = false) UUID branchId,
            @RequestHeader(USER_ROLES_HEADER) String roles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        UserContext actor = UserContext.fromHeaders(userId, branchId, roles);
        return remarkService.findCalendarEvents(actor, from, to);
    }

    @PostMapping("/{id}/calendar-events")
    public RemarkCalendarEventResponse setCalendarEvent(
            @PathVariable UUID id,
            @Valid @RequestBody SetRemarkCalendarEventRequest request,
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(value = BRANCH_ID_HEADER, required = false) UUID branchId,
            @RequestHeader(USER_ROLES_HEADER) String roles
    ) {
        UserContext actor = UserContext.fromHeaders(userId, branchId, roles);
        return remarkService.setCalendarEvent(id, request, actor);
    }

    @DeleteMapping("/{id}/calendar-events/{workDate}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCalendarEvent(
            @PathVariable UUID id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(value = BRANCH_ID_HEADER, required = false) UUID branchId,
            @RequestHeader(USER_ROLES_HEADER) String roles
    ) {
        UserContext actor = UserContext.fromHeaders(userId, branchId, roles);
        remarkService.removeCalendarEvent(id, workDate, actor);
    }

    @GetMapping("/{id}")
    public RemarkResponse findById(
            @PathVariable UUID id,
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(value = BRANCH_ID_HEADER, required = false) UUID branchId,
            @RequestHeader(USER_ROLES_HEADER) String roles
    ) {
        UserContext actor = UserContext.fromHeaders(userId, branchId, roles);
        return remarkService.findById(id, actor);
    }

    @PatchMapping("/{id}/assign")
    public RemarkResponse assignMaster(
            @PathVariable UUID id,
            @Valid @RequestBody AssignMasterRequest request,
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(value = BRANCH_ID_HEADER, required = false) UUID branchId,
            @RequestHeader(USER_ROLES_HEADER) String roles
    ) {
        UserContext actor = UserContext.fromHeaders(userId, branchId, roles);
        return remarkService.assignMaster(id, request, actor);
    }

    @PatchMapping("/{id}/start")
    public RemarkResponse startWork(
            @PathVariable UUID id,
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(value = BRANCH_ID_HEADER, required = false) UUID branchId,
            @RequestHeader(USER_ROLES_HEADER) String roles
    ) {
        UserContext actor = UserContext.fromHeaders(userId, branchId, roles);
        return remarkService.startWork(id, actor);
    }

    @PatchMapping("/{id}/estimated-resolution")
    public RemarkResponse setEstimatedResolution(
            @PathVariable UUID id,
            @Valid @RequestBody SetEstimatedResolutionRequest request,
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(value = BRANCH_ID_HEADER, required = false) UUID branchId,
            @RequestHeader(USER_ROLES_HEADER) String roles
    ) {
        UserContext actor = UserContext.fromHeaders(userId, branchId, roles);
        return remarkService.setEstimatedResolution(id, request, actor);
    }

    @PatchMapping("/{id}/resolve")
    public RemarkResponse resolve(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveRemarkRequest request,
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(value = BRANCH_ID_HEADER, required = false) UUID branchId,
            @RequestHeader(USER_ROLES_HEADER) String roles
    ) {
        UserContext actor = UserContext.fromHeaders(userId, branchId, roles);
        return remarkService.resolve(id, request, actor);
    }
}
