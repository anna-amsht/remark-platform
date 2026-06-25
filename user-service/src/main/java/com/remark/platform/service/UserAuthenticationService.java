package com.remark.platform.service;

import com.remark.platform.dto.AuthenticatedUserResponse;
import com.remark.platform.entity.RoleCode;
import com.remark.platform.exceptions.InvalidCredentialsException;
import com.remark.platform.entity.User;
import com.remark.platform.entity.UserStatus;
import com.remark.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AuthenticatedUserResponse authenticate(String email, String password) {
        User user = userRepository.findByEmailWithRoles(normalizeEmail(email))
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return UserMapper.toAuthenticatedResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthenticatedUserResponse getActiveUserById(UUID userId) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }

        return UserMapper.toAuthenticatedResponse(user);
    }

    @Transactional(readOnly = true)
    public List<AuthenticatedUserResponse> findActiveUsersByBranchAndRole(UUID branchId, RoleCode roleCode) {
        return userRepository.findActiveByBranchAndRole(UserStatus.ACTIVE, branchId, roleCode)
                .stream()
                .map(UserMapper::toAuthenticatedResponse)
                .toList();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
