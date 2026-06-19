package com.remark.platform.exceptions;

public class ApprovalNotAllowedException extends RuntimeException {

    public ApprovalNotAllowedException(String message) {
        super(message);
    }
}
