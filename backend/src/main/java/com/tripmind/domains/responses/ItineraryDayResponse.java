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
public class ItineraryDayResponse {

    private Long id;
    private Long tripId;
    private Short dayNumber;
    private LocalDate date;
    private String note;

    @Builder.Default
    private List<ActivityResponse> activities = new ArrayList<>();

    private Long totalDayDistanceMeters;
    private String formattedTotalDayDistance;
    private Integer totalDayTravelMinutes;
    private String formattedTotalDayTravelTime;
}
