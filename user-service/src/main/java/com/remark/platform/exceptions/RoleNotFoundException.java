package com.remark.platform.exceptions;

import com.remark.platform.entity.RoleCode;

public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(RoleCode code) {
        super("Role not found: " + code);
    }
}
