package com.remark.platform.controller;

import com.remark.platform.exceptions.InvalidMasterAssignmentException;
import com.remark.platform.exceptions.InvalidRemarkTransitionException;
import com.remark.platform.exceptions.InvalidRequestContextException;
import com.remark.platform.exceptions.RemarkAccessDeniedException;
import com.remark.platform.exceptions.RemarkNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RemarkNotFoundException.class)
    public ProblemDetail handleNotFound(RemarkNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({
            RemarkAccessDeniedException.class,
            InvalidRemarkTransitionException.class,
            InvalidMasterAssignmentException.class
    })
    public ProblemDetail handleForbidden(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InvalidRequestContextException.class)
    public ProblemDetail handleBadContext(InvalidRequestContextException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ProblemDetail handleUserServiceUnavailable(ResourceAccessException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "User service is unavailable"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
