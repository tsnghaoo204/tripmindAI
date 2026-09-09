package com.tripmind.domains.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedPlaceResponse {

    private Long id;
    private PlaceResponse place;
    private Instant createdAt;
}
