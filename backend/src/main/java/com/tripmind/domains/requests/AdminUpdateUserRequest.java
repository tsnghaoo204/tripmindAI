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

    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    private String password;
}
