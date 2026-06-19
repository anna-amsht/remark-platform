package com.remark.platform.service;

import com.remark.platform.dto.AuthenticatedUserResponse;
import com.remark.platform.exceptions.InvalidCredentialsException;
import com.remark.platform.entity.User;
import com.remark.platform.entity.UserStatus;
import com.remark.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
