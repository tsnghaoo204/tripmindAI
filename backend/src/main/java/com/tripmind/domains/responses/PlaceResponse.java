package com.tripmind.domains.responses;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceResponse {

    private Long id;
    private String provider;
    private String externalId;
    private String name;
    private String category;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal rating;
    private Integer userRatingsTotal;
    private Integer priceLevel;
    private String address;
    private String phoneNumber;
    private String websiteUrl;
    private JsonNode openingHours;
    private JsonNode reviews;
    private JsonNode photoUrls;
}
