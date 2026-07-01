package com.remark.platform.controller;

import com.remark.platform.dto.AssignMasterRequest;
import com.remark.platform.dto.CreateRemarkRequest;
import com.remark.platform.dto.ResolveRemarkRequest;
import com.remark.platform.dto.SetEstimatedResolutionRequest;
import com.remark.platform.dto.SetRemarkCalendarEventRequest;
import com.remark.platform.entity.RemarkStatus;
import com.remark.platform.security.UserContext;
import com.remark.platform.service.RemarkService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RemarkControllerTest {

    private final RemarkService remarkService = mock(RemarkService.class);
    private final RemarkController controller = new RemarkController(remarkService);

    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID branchId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID remarkId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void createDelegatesToService() {
        CreateRemarkRequest request = new CreateRemarkRequest("Building", "Description", LocalDate.now());

        controller.create(request, userId, branchId, "APPLICANT");

        verify(remarkService).create(eq(request), any(UserContext.class));
    }

    @Test
    void findAllDelegatesToService() {
        UUID branchFilter = UUID.fromString("44444444-4444-4444-4444-444444444444");

        controller.findAll(userId, branchId, "BRANCH_HEAD", RemarkStatus.NEW, branchFilter, 1, 20, "search");

        verify(remarkService).findAll(any(UserContext.class), eq(RemarkStatus.NEW), eq(branchFilter), eq(1), eq(20), eq("search"));
    }

    @Test
    void calendarEndpointsDelegateToService() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.plusDays(1);
        SetRemarkCalendarEventRequest request = new SetRemarkCalendarEventRequest(
                LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );

        controller.findCalendarEvents(userId, branchId, "MASTER", from, to);
        controller.setCalendarEvent(remarkId, request, userId, branchId, "MASTER");
        controller.removeCalendarEvent(remarkId, LocalDate.now(), userId, branchId, "MASTER");

        verify(remarkService).findCalendarEvents(any(UserContext.class), eq(from), eq(to));
        verify(remarkService).setCalendarEvent(eq(remarkId), eq(request), any(UserContext.class));
        verify(remarkService).removeCalendarEvent(eq(remarkId), any(LocalDate.class), any(UserContext.class));
    }

    @Test
    void remarkActionEndpointsDelegateToService() {
        AssignMasterRequest assignRequest = new AssignMasterRequest(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        SetEstimatedResolutionRequest estimatedRequest = new SetEstimatedResolutionRequest(LocalDateTime.now().plusDays(1));
        ResolveRemarkRequest resolveRequest = new ResolveRemarkRequest("Fixed");

        controller.findById(remarkId, userId, branchId, "APPLICANT");
        controller.assignMaster(remarkId, assignRequest, userId, branchId, "BRANCH_HEAD");
        controller.startWork(remarkId, userId, branchId, "MASTER");
        controller.setEstimatedResolution(remarkId, estimatedRequest, userId, branchId, "MASTER");
        controller.resolve(remarkId, resolveRequest, userId, branchId, "MASTER");

        verify(remarkService).findById(eq(remarkId), any(UserContext.class));
        verify(remarkService).assignMaster(eq(remarkId), eq(assignRequest), any(UserContext.class));
        verify(remarkService).startWork(eq(remarkId), any(UserContext.class));
        verify(remarkService).setEstimatedResolution(eq(remarkId), eq(estimatedRequest), any(UserContext.class));
        verify(remarkService).resolve(eq(remarkId), eq(resolveRequest), any(UserContext.class));
    }
}
