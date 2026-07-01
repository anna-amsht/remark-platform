package com.remark.platform.service;

import com.remark.platform.client.ReferenceClient;
import com.remark.platform.dto.RegisterRequest;
import com.remark.platform.entity.Role;
import com.remark.platform.entity.RoleCode;
import com.remark.platform.entity.User;
import com.remark.platform.entity.UserStatus;
import com.remark.platform.exceptions.EmailAlreadyExistsException;
import com.remark.platform.exceptions.InvalidRegistrationException;
import com.remark.platform.exceptions.RoleNotFoundException;
import com.remark.platform.repository.RoleRepository;
import com.remark.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceRegistrationTest {

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

    private UUID branchId;
    private Role applicantRole;
    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        branchId = UUID.randomUUID();
        applicantRole = new Role();
        applicantRole.setId(UUID.randomUUID());
        applicantRole.setCode(RoleCode.APPLICANT);
        applicantRole.setName("Applicant");

        request = new RegisterRequest(
                "User@Example.com",
                "Password123!",
                "Иванов Иван",
                "Инженер",
                branchId,
                RoleCode.APPLICANT
        );

        lenient().when(roleRepository.findByCode(RoleCode.APPLICANT)).thenReturn(Optional.of(applicantRole));
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        lenient().doNothing().when(referenceClient).ensureActiveBranchExists(branchId);
    }

    @Test
    void register_createsPendingUser_whenEmailIsNew() {
        when(userRepository.findByEmailWithRoles("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        var response = userService.register(request);

        assertThat(response.status()).isEqualTo(UserStatus.PENDING);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.roles()).containsExactly(RoleCode.APPLICANT);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    void register_reapplies_whenPreviousApplicationWasRejected() {
        User existing = existingUser(UserStatus.REJECTED);
        when(userRepository.findByEmailWithRoles("user@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        var response = userService.register(request);

        assertThat(response.status()).isEqualTo(UserStatus.PENDING);
        assertThat(existing.getFullName()).isEqualTo("Иванов Иван");
        assertThat(existing.getPosition()).isEqualTo("Инженер");
        assertThat(existing.getRoles()).containsExactly(applicantRole);
        verify(userRepository).save(existing);
    }

    @Test
    void register_reapplies_whenUserWasDismissed() {
        User existing = existingUser(UserStatus.DISMISSED);
        when(userRepository.findByEmailWithRoles("user@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        var response = userService.register(request);

        assertThat(response.status()).isEqualTo(UserStatus.PENDING);
        assertThat(existing.getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    void register_rejects_whenEmailAlreadyActive() {
        when(userRepository.findByEmailWithRoles("user@example.com"))
                .thenReturn(Optional.of(existingUser(UserStatus.ACTIVE)));

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void register_rejects_whenApplicationAlreadyPending() {
        when(userRepository.findByEmailWithRoles("user@example.com"))
                .thenReturn(Optional.of(existingUser(UserStatus.PENDING)));

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessageContaining("pending approval");
    }

    @Test
    void register_rejectsAdminSelfRegistration() {
        RegisterRequest adminRequest = new RegisterRequest(
                "admin@example.com",
                "Password123!",
                "Admin User",
                "Manager",
                branchId,
                RoleCode.ADMIN
        );

        assertThatThrownBy(() -> userService.register(adminRequest))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessageContaining("Role cannot be registered");
    }

    @Test
    void register_rejectsWhenRequestedRoleDoesNotExist() {
        when(userRepository.findByEmailWithRoles("user@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByCode(RoleCode.APPLICANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void register_updatesDismissedUserWithTrimmedFieldsAndNewBranch() {
        User existing = existingUser(UserStatus.DISMISSED);
        RegisterRequest trimmedRequest = new RegisterRequest(
                "User@Example.com",
                "Password123!",
                " New Name ",
                " New Position ",
                branchId,
                RoleCode.APPLICANT
        );
        when(userRepository.findByEmailWithRoles("user@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        var response = userService.register(trimmedRequest);

        assertThat(response.fullName()).isEqualTo("New Name");
        assertThat(response.position()).isEqualTo("New Position");
        assertThat(response.branchId()).isEqualTo(branchId);
    }

    private User existingUser(UserStatus status) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setPasswordHash("old-password");
        user.setFullName("Old Name");
        user.setPosition("Old Position");
        user.setBranchId(UUID.randomUUID());
        user.setStatus(status);
        user.setRoles(new HashSet<>(java.util.Set.of(applicantRole)));
        return user;
    }
}
