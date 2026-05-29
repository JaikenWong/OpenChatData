package com.openchat4u.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");

            if (header == null || !header.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = header.substring(7);

            if (!jwtTokenProvider.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtTokenProvider.getUsername(token);
            String tenantCode = jwtTokenProvider.getTenantCode(token);
            Long userId = jwtTokenProvider.getUserId(token);

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            TenantContext.set(tenantCode);
            request.setAttribute("jwtUserId", userId);
            request.setAttribute("jwtTenantCode", tenantCode);

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
