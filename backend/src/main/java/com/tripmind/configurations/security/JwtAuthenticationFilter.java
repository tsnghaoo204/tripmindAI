package com.tripmind.configurations.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Xác thực bằng header {@code Authorization: Bearer}. Không nhận phiếu qua query string
 * ({@code ?token=}): URL bị ghi vào log, lịch sử trình duyệt và header Referer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String jwt = getJwtFromRequest(request);
        if (StringUtils.hasText(jwt)) {
            try {
                authenticate(jwt, request);
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Bo qua JWT khong hop le: {}", ex.getMessage());
            } catch (Exception ex) {
                log.warn("Khong xac thuc duoc JWT: {}", ex.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String jwt, HttpServletRequest request) {
        Claims claims = tokenProvider.getClaims(jwt);
        Long userId = Long.parseLong(claims.getSubject());
        UserPrincipal user = (UserPrincipal) customUserDetailsService.loadUserById(userId);
        if (user == null || !user.isEnabled() || tokenVersion(claims) != user.getTokenVersion()) {
            return;
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** Phiếu cấp trước khi có claim {@code tv} được coi là phiên bản 0. */
    private static int tokenVersion(Claims claims) {
        Object value = claims.get(JwtTokenProvider.TOKEN_VERSION_CLAIM);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
