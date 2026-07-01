package com.remark.platform.controller;

import com.remark.platform.dto.RegisterRequest;
import com.remark.platform.entity.RoleCode;
import com.remark.platform.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserControllerTest {

    private final UserService userService = mock(UserService.class);
    private final UserController controller = new UserController(userService);

    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID targetId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID branchId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void endpointsDelegateToService() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com",
                "Password123!",
                "User",
                "Engineer",
                branchId,
                RoleCode.APPLICANT
        );

        controller.register(request);
        controller.findPending(userId);
        controller.findManaged(userId);
        controller.approve(targetId, userId);
        controller.reject(targetId, userId);
        controller.dismiss(targetId, userId);
        controller.findMe(userId);
        controller.findById(targetId);
        controller.findByBranchAndRole(branchId, RoleCode.MASTER);

        verify(userService).register(request);
        verify(userService).findPendingApprovals(userId);
        verify(userService).findManagedEmployees(userId);
        verify(userService).approve(targetId, userId);
        verify(userService).reject(targetId, userId);
        verify(userService).dismiss(targetId, userId);
        verify(userService).findById(userId);
        verify(userService).findById(targetId);
        verify(userService).findActiveByBranchAndRole(branchId, RoleCode.MASTER);
    }
}
