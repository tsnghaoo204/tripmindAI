package com.tripmind.configurations.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.exceptions.AppException;
import com.tripmind.services.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Tầng giới hạn chung (BR-601, NFR-07): mỗi người dùng — hoặc mỗi IP khi chưa đăng nhập —
 * tối đa {@code api-per-minute} request mỗi phút. Tầng AI có giới hạn riêng, chặt hơn.
 */
@Component
@RequiredArgsConstructor
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Value("${tripmind.rate-limit.api-per-minute:120}")
    private int perMinute;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String subject = auth != null && auth.getPrincipal() instanceof UserPrincipal principal
                ? "u" + principal.getId()
                : "ip" + request.getRemoteAddr();
        try {
            rateLimiter.check("api", subject, perMinute, Duration.ofMinutes(1));
        } catch (AppException e) {
            response.setStatus(e.getErrorCode().getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getOutputStream(),
                    ApiResponse.fail(e.getErrorCode().name(), e.getMessage(), e.getDetails()));
            return;
        }
        chain.doFilter(request, response);
    }
}
