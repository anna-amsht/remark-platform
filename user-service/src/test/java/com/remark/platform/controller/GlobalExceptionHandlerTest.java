package com.remark.platform.controller;

import com.remark.platform.exceptions.ApprovalNotAllowedException;
import com.remark.platform.exceptions.EmailAlreadyExistsException;
import com.remark.platform.exceptions.InvalidCredentialsException;
import com.remark.platform.exceptions.InvalidRegistrationException;
import com.remark.platform.exceptions.RoleNotFoundException;
import com.remark.platform.exceptions.UserNotFoundException;
import com.remark.platform.entity.RoleCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsDomainExceptionsToExpectedStatuses() {
        assertThat(handler.handleUserNotFound(new UserNotFoundException("missing")).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(handler.handleEmailAlreadyExists(new EmailAlreadyExistsException("duplicate")).getStatus())
                .isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(handler.handleInvalidRegistration(new InvalidRegistrationException("invalid")).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(handler.handleApprovalNotAllowed(new ApprovalNotAllowedException("forbidden")).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(handler.handleInvalidCredentials(new InvalidCredentialsException()).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(handler.handleRoleNotFound(new RoleNotFoundException(RoleCode.APPLICANT)).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    void mapsReferenceUnavailableToServiceUnavailable() {
        var problem = handler.handleReferenceUnavailable(new ResourceAccessException("down"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problem.getDetail()).isEqualTo("Reference service is unavailable");
    }
}
