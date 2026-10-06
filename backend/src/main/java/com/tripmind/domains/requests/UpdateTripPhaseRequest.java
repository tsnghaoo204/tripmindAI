package com.tripmind.domains.requests;

import com.tripmind.enums.TripPhase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Đặt tay giai đoạn chuyến đi (FR-1002). {@code phase = null} để quay về tự suy từ ngày. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTripPhaseRequest {

    private TripPhase phase;
}
