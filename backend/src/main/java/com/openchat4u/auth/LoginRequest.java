package com.openchat4u.auth;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class LoginRequest {
    private final String username;
    private final String password;
    private final String tenantCode;
}
