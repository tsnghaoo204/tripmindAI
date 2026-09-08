package com.tripmind.domains.requests;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 1, max = 120, message = "Name must not exceed 120 characters")
    private String name;

    private String avatarUrl;
}
