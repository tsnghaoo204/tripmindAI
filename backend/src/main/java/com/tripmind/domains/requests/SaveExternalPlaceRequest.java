package com.tripmind.domains.requests;

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
public class SaveExternalPlaceRequest {

    @Builder.Default
    @Size(max = 16)
    private String provider = "GOOGLE";

    @NotBlank(message = "externalId is required")
    @Size(max = ValidationLimits.EXTERNAL_ID_MAX)
    private String externalId;
}
