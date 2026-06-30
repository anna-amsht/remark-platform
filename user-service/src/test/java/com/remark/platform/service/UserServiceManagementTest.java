package com.remark.platform.service;

import com.remark.platform.client.ReferenceClient;
import com.remark.platform.entity.Role;
import com.remark.platform.entity.RoleCode;
import com.remark.platform.entity.User;
import com.remark.platform.entity.UserStatus;
import com.remark.platform.exceptions.ApprovalNotAllowedException;
import com.remark.platform.repository.RoleRepository;
import com.remark.platform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceManagementTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ReferenceClient referenceClient;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    private final UUID branchId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID managerId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID targetId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void branchHeadApprovesApplicantInSameBranch() {
        User approver = user(managerId, UserStatus.ACTIVE, branchId, RoleCode.BRANCH_HEAD);
        User target = user(targetId, UserStatus.PENDING, branchId, RoleCode.APPLICANT);
        when(userRepository.findByIdWithRoles(targetId)).thenReturn(Optional.of(target));
        when(userRepository.findByIdWithRoles(managerId)).thenReturn(Optional.of(approver));
        when(userRepository.save(target)).thenReturn(target);

        var response = userService.approve(targetId, managerId);

        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(UserApprovedApplicationEvent.class));
    }

    @Test
    void approveRejectsDifferentBranch() {
        User approver = user(managerId, UserStatus.ACTIVE, branchId, RoleCode.BRANCH_HEAD);
        User target = user(targetId, UserStatus.PENDING, UUID.fromString("44444444-4444-4444-4444-444444444444"), RoleCode.APPLICANT);
        when(userRepository.findByIdWithRoles(targetId)).thenReturn(Optional.of(target));
        when(userRepository.findByIdWithRoles(managerId)).thenReturn(Optional.of(approver));

        assertThatThrownBy(() -> userService.approve(targetId, managerId))
                .isInstanceOf(ApprovalNotAllowedException.class)
                .hasMessageContaining("same branch");
    }

    @Test
    void rejectChangesPendingUserStatus() {
        User approver = user(managerId, UserStatus.ACTIVE, branchId, RoleCode.BRANCH_HEAD);
        User target = user(targetId, UserStatus.PENDING, branchId, RoleCode.MASTER);
        when(userRepository.findByIdWithRoles(targetId)).thenReturn(Optional.of(target));
        when(userRepository.findByIdWithRoles(managerId)).thenReturn(Optional.of(approver));
        when(userRepository.save(target)).thenReturn(target);

        var response = userService.reject(targetId, managerId);

        assertThat(response.status()).isEqualTo(UserStatus.REJECTED);
    }

    @Test
    void adminSeesOnlyPendingBranchHeadsForApproval() {
        User admin = user(managerId, UserStatus.ACTIVE, branchId, RoleCode.ADMIN);
        User pendingBranchHead = user(targetId, UserStatus.PENDING, branchId, RoleCode.BRANCH_HEAD);
        when(userRepository.findByIdWithRoles(managerId)).thenReturn(Optional.of(admin));
        when(userRepository.findPendingByBranchAndRoleCodes(
                UserStatus.PENDING,
                branchId,
                Set.of(RoleCode.BRANCH_HEAD)
        )).thenReturn(List.of(pendingBranchHead));

        var responses = userService.findPendingApprovals(managerId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().roles()).containsExactly(RoleCode.BRANCH_HEAD);
    }

    @Test
    void branchHeadFindsManagedMastersExceptSelf() {
        User branchHead = user(managerId, UserStatus.ACTIVE, branchId, RoleCode.BRANCH_HEAD);
        User master = user(targetId, UserStatus.ACTIVE, branchId, RoleCode.MASTER);
        when(userRepository.findByIdWithRoles(managerId)).thenReturn(Optional.of(branchHead));
        when(userRepository.findActiveByBranchAndRoleCodes(UserStatus.ACTIVE, branchId, Set.of(RoleCode.MASTER)))
                .thenReturn(List.of(branchHead, master));

        var responses = userService.findManagedEmployees(managerId);

        assertThat(responses).extracting("id").containsExactly(targetId);
    }

    @Test
    void branchHeadDismissesMasterInSameBranch() {
        User branchHead = user(managerId, UserStatus.ACTIVE, branchId, RoleCode.BRANCH_HEAD);
        User master = user(targetId, UserStatus.ACTIVE, branchId, RoleCode.MASTER);
        when(userRepository.findByIdWithRoles(managerId)).thenReturn(Optional.of(branchHead));
        when(userRepository.findByIdWithRoles(targetId)).thenReturn(Optional.of(master));
        when(userRepository.save(master)).thenReturn(master);

        var response = userService.dismiss(targetId, managerId);

        assertThat(response.status()).isEqualTo(UserStatus.DISMISSED);
    }

    @Test
    void branchHeadCannotDismissApplicant() {
        User branchHead = user(managerId, UserStatus.ACTIVE, branchId, RoleCode.BRANCH_HEAD);
        User applicant = user(targetId, UserStatus.ACTIVE, branchId, RoleCode.APPLICANT);
        when(userRepository.findByIdWithRoles(managerId)).thenReturn(Optional.of(branchHead));
        when(userRepository.findByIdWithRoles(targetId)).thenReturn(Optional.of(applicant));

        assertThatThrownBy(() -> userService.dismiss(targetId, managerId))
                .isInstanceOf(ApprovalNotAllowedException.class)
                .hasMessageContaining("cannot dismiss");
    }

    @Test
    void findActiveByBranchAndRoleMapsRepositoryUsers() {
        User master = user(targetId, UserStatus.ACTIVE, branchId, RoleCode.MASTER);
        when(userRepository.findActiveByBranchAndRole(UserStatus.ACTIVE, branchId, RoleCode.MASTER))
                .thenReturn(List.of(master));

        var responses = userService.findMastersByBranch(branchId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(targetId);
    }

    private static User user(UUID id, UserStatus status, UUID branchId, RoleCode roleCode) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setCode(roleCode);
        role.setName(roleCode.name());

        User user = new User();
        user.setId(id);
        user.setEmail(id + "@example.com");
        user.setPasswordHash("hash");
        user.setFullName("Test User");
        user.setPosition("Engineer");
        user.setBranchId(branchId);
        user.setStatus(status);
        user.setRoles(new HashSet<>(Set.of(role)));
        return user;
    }
}
