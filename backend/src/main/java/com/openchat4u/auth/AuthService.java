package com.openchat4u.auth;

import com.openchat4u.tenant.Tenant;
import com.openchat4u.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final TenantService tenantService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(String username, String password, String tenantCode) {
        Tenant tenant = tenantService.findByCode(tenantCode)
            .orElseThrow(() -> new IllegalArgumentException("Invalid tenant code or password"));

        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new IllegalArgumentException("Tenant is not active");
        }

        String token = jwtTokenProvider.generateToken(username, tenant.getId(), tenant.getId(), tenant.getCode());
        return new AuthResponse(token, username, tenant.getId(), tenant.getCode(), tenant.getName());
    }
}
