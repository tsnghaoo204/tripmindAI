package com.tripmind.domains.requests;

import jakarta.validation.constraints.Pattern;
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

    /** Chuỗi rỗng để xoá ảnh đại diện. */
    @Size(max = ValidationLimits.URL_MAX)
    @Pattern(regexp = "^$|" + ValidationLimits.HTTPS_URL, message = ValidationLimits.HTTPS_URL_MESSAGE)
    private String avatarUrl;
}
