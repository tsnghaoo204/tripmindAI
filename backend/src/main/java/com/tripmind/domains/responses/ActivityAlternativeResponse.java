package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityAlternativeResponse {

    private Long id;
    private Long placeId;
    private String placeName;
    private String title;
    /** BUDGET, BALANCED, PREMIUM */
    private String tier;
    private String tierLabel;
    private Long estimatedCost;
    private BigDecimal rating;
    private Integer userRatingsTotal;
    private String address;
    private String photoUrl;

    @JsonProperty("isSelected")
    private boolean isSelected;
}
