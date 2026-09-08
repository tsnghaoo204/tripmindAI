package com.tripmind.configurations.security;

import com.tripmind.entities.UserEntity;
import com.tripmind.enums.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("TripMindAISecretKeyForJwtSigningMustBeAtLeast256BitsLong2026!");
        jwtProperties.setExpirationMs(3600000L); // 1 hour

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    void testGenerateAndValidateToken() {
        UserEntity user = UserEntity.builder()
                .id(100L)
                .email("test@tripmind.ai")
                .name("Tester")
                .role(UserRole.USER)
                .passwordHash("hashed")
                .isActive(true)
                .build();

        UserPrincipal principal = UserPrincipal.fromEntity(user);
        String token = jwtTokenProvider.generateToken(principal);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        assertEquals(100L, userId);

        Claims claims = jwtTokenProvider.getClaims(token);
        assertEquals("test@tripmind.ai", claims.get("email"));
        assertEquals("USER", claims.get("role"));
        assertEquals("Tester", claims.get("name"));
    }

    @Test
    void testInvalidTokenReturnsFalse() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }
}
