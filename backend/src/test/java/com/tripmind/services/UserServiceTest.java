package com.tripmind.services;

import com.tripmind.domains.requests.ChangePasswordRequest;
import com.tripmind.domains.requests.UpdateProfileRequest;
import com.tripmind.domains.responses.UserResponse;
import com.tripmind.entities.UserEntity;
import com.tripmind.enums.UserRole;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void testGetProfileSuccess() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("user@tripmind.ai")
                .name("Nguyen Van A")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getProfile(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("user@tripmind.ai", response.getEmail());
    }

    @Test
    void testUpdateProfileSuccess() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("user@tripmind.ai")
                .name("Old Name")
                .avatarUrl("old_url")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("New Name")
                .avatarUrl("https://example.com/avatar.png")
                .build();

        UserResponse updated = userService.updateProfile(1L, request);

        assertEquals("New Name", updated.getName());
        assertEquals("https://example.com/avatar.png", updated.getAvatarUrl());
    }

    @Test
    void testChangePasswordSuccess() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .passwordHash("old_hashed")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old_password", "old_hashed")).thenReturn(true);
        when(passwordEncoder.encode("new_password")).thenReturn("new_hashed");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old_password")
                .newPassword("new_password")
                .build();

        assertDoesNotThrow(() -> userService.changePassword(1L, request));
        assertEquals("new_hashed", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    void testChangePasswordWrongCurrentPasswordThrowsValidation() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .passwordHash("old_hashed")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_old", "old_hashed")).thenReturn(false);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrong_old")
                .newPassword("new_password")
                .build();

        AppException ex = assertThrows(AppException.class, () -> userService.changePassword(1L, request));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }
}
