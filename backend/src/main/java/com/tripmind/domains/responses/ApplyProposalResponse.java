package com.tripmind.domains.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/** ADS-30 §10.3: số thao tác đã áp dụng, thao tác bị bỏ qua kèm lý do, lịch trình mới. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyProposalResponse {

    private Long proposalId;
    private int applied;
    private List<Skipped> skipped;
    private Instant undoAvailableUntil;
    private ItineraryResponse itinerary;

    public record Skipped(String op, Long activityId, String title, String reason) {
    }
}
