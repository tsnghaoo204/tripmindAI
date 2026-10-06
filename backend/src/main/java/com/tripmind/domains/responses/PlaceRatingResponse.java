package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripmind.enums.PlaceVerdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/** Một địa điểm của chuyến (hoặc đã đánh giá) kèm ý kiến hiện tại của người dùng. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlaceRatingResponse {

    private PlaceResponse place;
    /** Null khi chưa đánh giá. */
    private PlaceVerdict verdict;
    private String note;
    private Long tripId;
    /** Các ngày của chuyến có ghé địa điểm này. */
    private List<Integer> dayNumbers;
    private Instant updatedAt;
}
