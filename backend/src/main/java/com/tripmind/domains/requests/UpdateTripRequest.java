package com.tripmind.domains.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTripRequest {

    @Size(max = 160, message = "Trip name cannot exceed 160 characters")
    private String name;

    @Min(value = 1, message = "Travelers must be at least 1")
    private Integer travelers;

    @Min(value = 0, message = "Budget must not be negative")
    private Long budget;

    private String currency;
}
