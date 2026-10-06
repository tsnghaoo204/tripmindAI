package com.tripmind.domains.requests;

import com.tripmind.enums.PlaceVerdict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceRatingRequest {

    @NotNull(message = "verdict is required (LIKE or DISLIKE)")
    private PlaceVerdict verdict;

    @Size(max = 200, message = "note cannot exceed 200 characters")
    private String note;
}
