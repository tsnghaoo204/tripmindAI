package com.tripmind.domains.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryResponse {

    private Long tripId;
    private String tripName;
    private String destinationName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Integer totalActivities;
    private Long totalEstimatedCost;
    private String currency;

    @Builder.Default
    private List<ItineraryDayResponse> days = new ArrayList<>();

    private Long totalTripDistanceMeters;
    private String formattedTotalTripDistance;
}
