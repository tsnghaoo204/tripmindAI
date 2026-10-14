package com.tripmind.domains.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 254)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = ValidationLimits.PASSWORD_MIN, max = ValidationLimits.PASSWORD_MAX, message = "Password must be between 8 and 72 characters")
    private String password;

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name must not exceed 120 characters")
    private String name;
}
