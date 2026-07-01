package com.remark.platform.exceptions;

import java.util.UUID;

public class RemarkNotFoundException extends RuntimeException {

    public RemarkNotFoundException(UUID id) {
        super("Remark not found: " + id);
    }
}
