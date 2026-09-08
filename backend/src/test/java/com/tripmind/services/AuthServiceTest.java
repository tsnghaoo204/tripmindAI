package com.tripmind.services;

import com.tripmind.configurations.security.JwtProperties;
import com.tripmind.configurations.security.JwtTokenProvider;
import com.tripmind.configurations.security.UserPrincipal;
import com.tripmind.domains.requests.LoginRequest;
import com.tripmind.domains.requests.RegisterRequest;
import com.tripmind.domains.responses.AuthResponse;
import com.tripmind.entities.UserEntity;
import com.tripmind.enums.UserRole;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.getExpirationMs()).thenReturn(86400000L);
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .email("user@tripmind.ai")
                .password("password123")
                .name("Nguyen Van A")
                .build();

        when(userRepository.existsByEmail("user@tripmind.ai")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");

        UserEntity savedUser = UserEntity.builder()
                .id(1L)
                .email("user@tripmind.ai")
                .passwordHash("encoded_pass")
                .name("Nguyen Van A")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(any(UserPrincipal.class))).thenReturn("mock_jwt_token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock_jwt_token", response.getAccessToken());
        assertEquals("user@tripmind.ai", response.getUser().getEmail());
        assertEquals(UserRole.USER, response.getUser().getRole());
    }

    @Test
    void testRegisterDuplicateEmailThrowsConflict() {
        RegisterRequest request = RegisterRequest.builder()
                .email("duplicate@tripmind.ai")
                .password("password123")
                .name("Nguyen Van B")
                .build();

        when(userRepository.existsByEmail("duplicate@tripmind.ai")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .email("user@tripmind.ai")
                .password("password123")
                .build();

        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("user@tripmind.ai")
                .passwordHash("encoded_pass")
                .name("Nguyen Van A")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        when(userRepository.findByEmail("user@tripmind.ai")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(UserPrincipal.class))).thenReturn("mock_jwt_token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock_jwt_token", response.getAccessToken());
        assertEquals("user@tripmind.ai", response.getUser().getEmail());
    }

    @Test
    void testLoginWrongPasswordThrowsUnauthorized() {
        LoginRequest request = LoginRequest.builder()
                .email("user@tripmind.ai")
                .password("wrongpassword")
                .build();

        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("user@tripmind.ai")
                .passwordHash("encoded_pass")
                .name("Nguyen Van A")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        when(userRepository.findByEmail("user@tripmind.ai")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encoded_pass")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }
}
