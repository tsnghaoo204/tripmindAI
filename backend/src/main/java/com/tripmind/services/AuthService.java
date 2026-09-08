package com.tripmind.services;

import com.tripmind.configurations.security.JwtProperties;
import com.tripmind.configurations.security.JwtTokenProvider;
import com.tripmind.configurations.security.UserPrincipal;
import com.tripmind.domains.requests.LoginRequest;
import com.tripmind.domains.requests.RegisterRequest;
import com.tripmind.domains.responses.AuthResponse;
import com.tripmind.domains.responses.UserResponse;
import com.tripmind.entities.UserEntity;
import com.tripmind.enums.UserRole;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException(ErrorCode.CONFLICT, "Email is already registered");
        }

        UserEntity user = UserEntity.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName().trim())
                .role(UserRole.USER)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("Successfully registered new user: ID={}, email={}", user.getId(), user.getEmail());

        UserPrincipal principal = UserPrincipal.fromEntity(user);
        String token = jwtTokenProvider.generateToken(principal);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(jwtProperties.getExpirationMs())
                .user(UserResponse.fromEntity(user))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid email or password");
        }

        if (!user.isActive()) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "User account is deactivated");
        }

        log.info("User successfully logged in: ID={}, email={}", user.getId(), user.getEmail());

        UserPrincipal principal = UserPrincipal.fromEntity(user);
        String token = jwtTokenProvider.generateToken(principal);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(jwtProperties.getExpirationMs())
                .user(UserResponse.fromEntity(user))
                .build();
    }
}
