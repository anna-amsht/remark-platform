package com.remark.platform.controller;

import com.remark.platform.dto.AuthenticateRequest;
import com.remark.platform.dto.AuthenticatedUserResponse;
import com.remark.platform.service.UserAuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserAuthenticationService userAuthenticationService;

    @PostMapping("/authenticate")
    public AuthenticatedUserResponse authenticate(@Valid @RequestBody AuthenticateRequest request) {
        return userAuthenticationService.authenticate(request.email(), request.password());
    }

    @GetMapping("/{userId}")
    public AuthenticatedUserResponse getActiveUser(@PathVariable UUID userId) {
        return userAuthenticationService.getActiveUserById(userId);
    }
}
