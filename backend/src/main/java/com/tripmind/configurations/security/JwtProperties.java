package com.tripmind.configurations.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret = "TripMindAISecretKeyForJwtSigningMustBeAtLeast256BitsLong2026!";
    private long expirationMs = 86400000L; // 24 hours
}
