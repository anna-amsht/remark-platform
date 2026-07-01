package com.remark.platform.security;

import com.remark.platform.exceptions.InvalidRequestContextException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserContextTest {

    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID branchId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void fromHeadersParsesRolesAndTrimsEmptyValues() {
        UserContext context = UserContext.fromHeaders(userId, branchId, " APPLICANT, MASTER, ");

        assertThat(context.userId()).isEqualTo(userId);
        assertThat(context.branchId()).isEqualTo(branchId);
        assertThat(context.hasRole(RoleCode.APPLICANT)).isTrue();
        assertThat(context.hasAnyRole(RoleCode.ADMIN, RoleCode.MASTER)).isTrue();
        assertThat(context.hasRole(RoleCode.ADMIN)).isFalse();
    }

    @Test
    void requireBranchIdRejectsMissingBranch() {
        UserContext context = new UserContext(userId, null, java.util.Set.of(RoleCode.APPLICANT));

        assertThatThrownBy(context::requireBranchId)
                .isInstanceOf(InvalidRequestContextException.class)
                .hasMessageContaining("Branch id");
    }

    @Test
    void fromHeadersRejectsMissingUserId() {
        assertThatThrownBy(() -> UserContext.fromHeaders(null, branchId, "APPLICANT"))
                .isInstanceOf(InvalidRequestContextException.class)
                .hasMessageContaining("X-User-Id");
    }

    @Test
    void fromHeadersRejectsMissingRoles() {
        assertThatThrownBy(() -> UserContext.fromHeaders(userId, branchId, " "))
                .isInstanceOf(InvalidRequestContextException.class)
                .hasMessageContaining("X-User-Roles");
    }
}
