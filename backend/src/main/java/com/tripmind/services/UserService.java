package com.tripmind.services;

import com.tripmind.domains.requests.ChangePasswordRequest;
import com.tripmind.domains.requests.UpdateProfileRequest;
import com.tripmind.domains.responses.UserResponse;
import com.tripmind.entities.UserEntity;
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
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        UserEntity user = findUserById(userId);
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        UserEntity user = findUserById(userId);

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        user = userRepository.save(user);
        log.info("Updated profile for user ID={}", userId);
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserEntity user = findUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Current password does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Changed password for user ID={}", userId);
    }

    private UserEntity findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found with ID: " + userId));
    }
}
