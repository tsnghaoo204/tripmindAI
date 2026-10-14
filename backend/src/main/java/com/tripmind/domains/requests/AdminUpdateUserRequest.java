package com.tripmind.domains.requests;

import com.tripmind.enums.UserRole;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {
    @Size(max = 120, message = "Name cannot exceed 120 characters")
    private String name;

    private UserRole role;

    private Boolean active;

    @Size(min = ValidationLimits.PASSWORD_MIN, max = ValidationLimits.PASSWORD_MAX, message = "Password must be between 8 and 72 characters")
    private String password;
}
