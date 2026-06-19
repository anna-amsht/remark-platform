package com.remark.platform.service;

import com.remark.platform.client.ReferenceClient;
import com.remark.platform.dto.RegisterRequest;
import com.remark.platform.dto.UserResponse;
import com.remark.platform.entity.Role;
import com.remark.platform.entity.RoleCode;
import com.remark.platform.entity.User;
import com.remark.platform.entity.UserStatus;
import com.remark.platform.exceptions.ApprovalNotAllowedException;
import com.remark.platform.exceptions.EmailAlreadyExistsException;
import com.remark.platform.exceptions.InvalidRegistrationException;
import com.remark.platform.exceptions.RoleNotFoundException;
import com.remark.platform.exceptions.UserNotFoundException;
import com.remark.platform.repository.RoleRepository;
import com.remark.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<RoleCode> SELF_REGISTER_ROLES = EnumSet.of(
            RoleCode.APPLICANT,
            RoleCode.MASTER,
            RoleCode.BRANCH_HEAD
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ReferenceClient referenceClient;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        validateRegistration(request);

        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        referenceClient.ensureActiveBranchExists(request.branchId());

        Role role = roleRepository.findByCode(request.requestedRole())
                .orElseThrow(() -> new RoleNotFoundException(request.requestedRole()));

        User user = new User();
        user.setEmail(normalizeEmail(request.email()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPosition(request.position().trim());
        user.setBranchId(request.branchId());
        user.setStatus(UserStatus.PENDING);
        user.getRoles().add(role);

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return userRepository.findByIdWithRoles(id)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findPendingApprovals(UUID approverId) {
        User approver = getActiveUser(approverId);
        validateApproverCanReviewPending(approver);

        UUID branchId = requireBranchId(approver);
        Set<RoleCode> pendingRoleCodes = pendingRoleCodesForApprover(approver);

        return userRepository.findPendingByBranchAndRoleCodes(
                        UserStatus.PENDING,
                        branchId,
                        pendingRoleCodes
                ).stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse approve(UUID userId, UUID approverId) {
        User target = getPendingUser(userId);
        User approver = getActiveUser(approverId);
        assertCanApprove(approver, target);

        target.setStatus(UserStatus.ACTIVE);
        return UserMapper.toResponse(userRepository.save(target));
    }

    @Transactional
    public UserResponse reject(UUID userId, UUID approverId) {
        User target = getPendingUser(userId);
        User approver = getActiveUser(approverId);
        assertCanApprove(approver, target);

        target.setStatus(UserStatus.REJECTED);
        return UserMapper.toResponse(userRepository.save(target));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findMastersByBranch(UUID branchId) {
        return findActiveByBranchAndRole(branchId, RoleCode.MASTER);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findActiveByBranchAndRole(UUID branchId, RoleCode roleCode) {
        return userRepository.findActiveByBranchAndRole(UserStatus.ACTIVE, branchId, roleCode)
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    private void validateRegistration(RegisterRequest request) {
        if (!SELF_REGISTER_ROLES.contains(request.requestedRole())) {
            throw new InvalidRegistrationException("Role cannot be registered via self-registration: "
                    + request.requestedRole());
        }
    }

    private User getPendingUser(UUID userId) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getStatus() != UserStatus.PENDING) {
            throw new ApprovalNotAllowedException("User is not pending approval: " + userId);
        }
        return user;
    }

    private User getActiveUser(UUID userId) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApprovalNotAllowedException("Approver account is not active: " + userId);
        }
        return user;
    }

    private void validateApproverCanReviewPending(User approver) {
        if (!hasAnyRole(approver, RoleCode.BRANCH_HEAD, RoleCode.ADMIN)) {
            throw new ApprovalNotAllowedException("User cannot review pending registrations: " + approver.getId());
        }
    }

    private void assertCanApprove(User approver, User target) {
        UUID approverBranchId = requireBranchId(approver);
        UUID targetBranchId = requireBranchId(target);

        if (!approverBranchId.equals(targetBranchId)) {
            throw new ApprovalNotAllowedException("Approver and applicant must belong to the same branch");
        }

        RoleCode targetRole = getSingleRoleCode(target);

        if (hasRole(approver, RoleCode.BRANCH_HEAD)
                && (targetRole == RoleCode.APPLICANT || targetRole == RoleCode.MASTER)) {
            return;
        }

        if (hasRole(approver, RoleCode.ADMIN) && targetRole == RoleCode.BRANCH_HEAD) {
            return;
        }

        throw new ApprovalNotAllowedException(
                "Approver cannot process registration for role " + targetRole);
    }

    private Set<RoleCode> pendingRoleCodesForApprover(User approver) {
        if (hasRole(approver, RoleCode.ADMIN)) {
            return EnumSet.of(RoleCode.BRANCH_HEAD);
        }
        if (hasRole(approver, RoleCode.BRANCH_HEAD)) {
            return EnumSet.of(RoleCode.APPLICANT, RoleCode.MASTER);
        }
        throw new ApprovalNotAllowedException("User cannot review pending registrations: " + approver.getId());
    }

    private RoleCode getSingleRoleCode(User user) {
        if (user.getRoles().size() != 1) {
            throw new ApprovalNotAllowedException("Pending user must have exactly one requested role");
        }
        return user.getRoles().iterator().next().getCode();
    }

    private UUID requireBranchId(User user) {
        if (user.getBranchId() == null) {
            throw new ApprovalNotAllowedException("User must belong to a branch: " + user.getId());
        }
        return user.getBranchId();
    }

    private boolean hasRole(User user, RoleCode roleCode) {
        return user.getRoles().stream().anyMatch(role -> role.getCode() == roleCode);
    }

    private boolean hasAnyRole(User user, RoleCode... roleCodes) {
        for (RoleCode roleCode : roleCodes) {
            if (hasRole(user, roleCode)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
