package com.tripmind.domains.requests;

import jakarta.validation.constraints.NotBlank;
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
    private String provider = "GOOGLE";

    @NotBlank(message = "externalId is required")
    private String externalId;
}
