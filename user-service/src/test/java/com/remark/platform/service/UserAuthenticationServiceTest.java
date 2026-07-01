package com.remark.platform.service;

import com.remark.platform.entity.Role;
import com.remark.platform.entity.RoleCode;
import com.remark.platform.entity.User;
import com.remark.platform.entity.UserStatus;
import com.remark.platform.exceptions.InvalidCredentialsException;
import com.remark.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAuthenticationService userAuthenticationService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setCode(RoleCode.APPLICANT);
        role.setName("Applicant");

        activeUser = new User();
        activeUser.setId(UUID.randomUUID());
        activeUser.setEmail("user@example.com");
        activeUser.setPasswordHash("encoded-password");
        activeUser.setFullName("User");
        activeUser.setPosition("Engineer");
        activeUser.setBranchId(UUID.randomUUID());
        activeUser.setStatus(UserStatus.ACTIVE);
        activeUser.setRoles(new java.util.HashSet<>(Set.of(role)));
    }

    @Test
    void authenticate_returnsUserWhenCredentialsValid() {
        when(userRepository.findByEmailWithRoles("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true);

        var response = userAuthenticationService.authenticate("User@Example.com", "Password123!");

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.roles()).containsExactly(RoleCode.APPLICANT);
    }

    @Test
    void authenticate_rejectsUnknownEmail() {
        when(userRepository.findByEmailWithRoles("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAuthenticationService.authenticate("missing@example.com", "Password123!"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void authenticate_rejectsInactiveUser() {
        activeUser.setStatus(UserStatus.PENDING);
        when(userRepository.findByEmailWithRoles("user@example.com")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> userAuthenticationService.authenticate("user@example.com", "Password123!"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void authenticate_rejectsWrongPassword() {
        when(userRepository.findByEmailWithRoles("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> userAuthenticationService.authenticate("user@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void getActiveUserById_returnsActiveUser() {
        when(userRepository.findByIdWithRoles(activeUser.getId())).thenReturn(Optional.of(activeUser));

        var response = userAuthenticationService.getActiveUserById(activeUser.getId());

        assertThat(response.userId()).isEqualTo(activeUser.getId());
        assertThat(response.roles()).containsExactly(RoleCode.APPLICANT);
    }

    @Test
    void getActiveUserById_rejectsMissingUser() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAuthenticationService.getActiveUserById(userId))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void getActiveUserById_rejectsInactiveUser() {
        activeUser.setStatus(UserStatus.DISMISSED);
        when(userRepository.findByIdWithRoles(activeUser.getId())).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> userAuthenticationService.getActiveUserById(activeUser.getId()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void findActiveUsersByBranchAndRole_mapsRepositoryUsers() {
        when(userRepository.findActiveByBranchAndRole(
                UserStatus.ACTIVE,
                activeUser.getBranchId(),
                RoleCode.APPLICANT
        )).thenReturn(java.util.List.of(activeUser));

        var response = userAuthenticationService.findActiveUsersByBranchAndRole(
                activeUser.getBranchId(),
                RoleCode.APPLICANT
        );

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().email()).isEqualTo("user@example.com");
    }
}
