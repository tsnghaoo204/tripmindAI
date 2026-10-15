package com.tripmind.domains.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripRecapResponse {

    private String title;
    private String destinationName;
    private String content;
    private Long tripId;
    private List<String> photoUrls;
    private Long totalSpent;
    private String currency;
    private int placesVisited;
}
