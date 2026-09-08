package com.tripmind.controllers;

import com.tripmind.configurations.security.SecurityUtils;
import com.tripmind.domains.requests.ChangePasswordRequest;
import com.tripmind.domains.requests.UpdateProfileRequest;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.UserResponse;
import com.tripmind.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and account management APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Get user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserResponse response = userService.getProfile(currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserResponse response = userService.updateProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", response));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change account password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userService.changePassword(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }
}
