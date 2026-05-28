package com.openchat4u.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openchat4u.tenant.Tenant;
import com.openchat4u.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15;

    private final TenantService tenantService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(String username, String password, String tenantCode) {
        Tenant tenant = tenantService.findByCode(tenantCode)
            .orElseThrow(() -> new IllegalArgumentException("Invalid tenant code or password"));

        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new IllegalArgumentException("Tenant is not active");
        }

        User user = userRepository.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getTenantCode, tenantCode)
        );
        if (user == null) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Account is locked due to too many failed login attempts");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("User account is not active");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int attempts = (user.getLoginAttempts() == null ? 0 : user.getLoginAttempts()) + 1;
            user.setLoginAttempts(attempts);
            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                log.warn("User locked: {}@{} after {} failed attempts", username, tenantCode, attempts);
            }
            userRepository.updateById(user);
            throw new IllegalArgumentException("Invalid username or password");
        }

        user.setLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.updateById(user);

        String token = jwtTokenProvider.generateToken(username, user.getId(), tenant.getId(), tenant.getCode());
        return new AuthResponse(token, username, tenant.getId(), tenant.getCode(), tenant.getName());
    }
}
