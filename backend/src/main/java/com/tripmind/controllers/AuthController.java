package com.tripmind.controllers;

import com.tripmind.configurations.security.LoginAttemptLimiter;
import com.tripmind.configurations.security.SecurityUtils;
import com.tripmind.domains.requests.LoginRequest;
import com.tripmind.domains.requests.RegisterRequest;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.AuthResponse;
import com.tripmind.domains.responses.UserResponse;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.services.AuthService;
import com.tripmind.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, and token verification")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final LoginAttemptLimiter loginAttemptLimiter;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        loginAttemptLimiter.ensureAllowed(request.getEmail(), ip);
        AuthResponse response;
        try {
            response = authService.login(request);
        } catch (AppException e) {
            if (e.getErrorCode() == ErrorCode.UNAUTHORIZED) {
                loginAttemptLimiter.recordFailure(request.getEmail(), ip);
            }
            throw e;
        }
        loginAttemptLimiter.reset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user info", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserResponse response = userService.getProfile(currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/health")
    @Operation(summary = "Check auth service health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.ok("Auth service is operating normally"));
    }
}
