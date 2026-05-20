package com.openchat4u.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityConfigValidator implements CommandLineRunner {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.secret:}")
    private String jwtSecretDefault;

    @Override
    public void run(String... args) {
        validateJwtSecret();
        log.info("Security configuration validated successfully");
    }

    private void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.isEmpty() || jwtSecret.equals("your-secret-key-change-in-production")) {
            throw new IllegalStateException(
                "JWT secret is not configured! Please set JWT_SECRET environment variable. " +
                "Example: export JWT_SECRET=$(openssl rand -base64 32)"
            );
        }
        
        if (jwtSecret.length() < 32) {
            log.warn("JWT secret is too short. Recommended: at least 32 characters.");
        }
    }
}
