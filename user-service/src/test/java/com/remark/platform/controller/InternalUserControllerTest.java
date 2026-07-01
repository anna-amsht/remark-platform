package com.remark.platform.controller;

import com.remark.platform.dto.AuthenticateRequest;
import com.remark.platform.dto.AuthenticatedUserResponse;
import com.remark.platform.entity.RoleCode;
import com.remark.platform.entity.UserStatus;
import com.remark.platform.service.UserAuthenticationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalUserControllerTest {

    private final UserAuthenticationService userAuthenticationService = mock(UserAuthenticationService.class);
    private final InternalUserController controller = new InternalUserController(userAuthenticationService);

    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID branchId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void authenticateDelegatesToAuthenticationService() {
        AuthenticateRequest request = new AuthenticateRequest("user@example.com", "password");
        AuthenticatedUserResponse expected = response(userId, branchId, RoleCode.APPLICANT);
        when(userAuthenticationService.authenticate(request.email(), request.password())).thenReturn(expected);

        AuthenticatedUserResponse actual = controller.authenticate(request);

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void getActiveUserDelegatesToAuthenticationService() {
        AuthenticatedUserResponse expected = response(userId, branchId, RoleCode.MASTER);
        when(userAuthenticationService.getActiveUserById(userId)).thenReturn(expected);

        AuthenticatedUserResponse actual = controller.getActiveUser(userId);

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void findActiveUsersByBranchAndRoleDelegatesToAuthenticationService() {
        List<AuthenticatedUserResponse> expected = List.of(response(userId, branchId, RoleCode.MASTER));
        when(userAuthenticationService.findActiveUsersByBranchAndRole(branchId, RoleCode.MASTER)).thenReturn(expected);

        List<AuthenticatedUserResponse> actual = controller.findActiveUsersByBranchAndRole(branchId, RoleCode.MASTER);

        assertThat(actual).isSameAs(expected);
    }

    private static AuthenticatedUserResponse response(UUID userId, UUID branchId, RoleCode role) {
        return new AuthenticatedUserResponse(
                userId,
                "user@example.com",
                "User",
                "Engineer",
                branchId,
                UserStatus.ACTIVE,
                Set.of(role)
        );
    }
}
