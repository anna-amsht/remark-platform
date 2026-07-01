package com.remark.platform.client;

public record AuthenticateRequest(
        String email,
        String password
) {
}
