package com.remark.platform.exceptions;

import java.util.UUID;

public class BranchNotFoundException extends RuntimeException {

    public BranchNotFoundException(UUID id) {
        super("Branch not found: " + id);
    }
}
