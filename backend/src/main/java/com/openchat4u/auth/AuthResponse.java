package com.openchat4u.auth;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String username;
    private Long tenantId;
    private String tenantCode;
    private String tenantName;

    public AuthResponse(String token, String username, Long tenantId, String tenantCode, String tenantName) {
        this.token = token;
        this.username = username;
        this.tenantId = tenantId;
        this.tenantCode = tenantCode;
        this.tenantName = tenantName;
    }
}
