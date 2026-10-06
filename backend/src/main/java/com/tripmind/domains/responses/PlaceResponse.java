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
    private String primaryType;
    private String primaryTypeDisplayName;
    private java.util.List<String> types;
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

    public static PlaceResponse fromEntity(com.tripmind.entities.PlaceEntity entity) {
        if (entity == null) {
            return null;
        }
        String primaryType = null;
        String primaryTypeDisplayName = null;
        java.util.List<String> types = null;
        if (entity.getMetadata() != null) {
            JsonNode meta = entity.getMetadata();
            if (meta.hasNonNull("primaryType")) {
                primaryType = meta.get("primaryType").asText();
            }
            if (meta.hasNonNull("primaryTypeDisplayName")) {
                primaryTypeDisplayName = meta.get("primaryTypeDisplayName").asText();
            }
            if (meta.hasNonNull("types") && meta.get("types").isArray()) {
                types = new java.util.ArrayList<>();
                for (JsonNode t : meta.get("types")) {
                    types.add(t.asText());
                }
            }
        }
        if (primaryType == null) {
            primaryType = entity.getCategory();
        }
        if (types == null && entity.getCategory() != null) {
            types = java.util.List.of(entity.getCategory());
        }

        return PlaceResponse.builder()
                .id(entity.getId())
                .provider(entity.getProvider())
                .externalId(entity.getExternalId())
                .name(entity.getName())
                .category(entity.getCategory())
                .primaryType(primaryType)
                .primaryTypeDisplayName(primaryTypeDisplayName)
                .types(types)
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .rating(entity.getRating())
                .userRatingsTotal(entity.getUserRatingsTotal())
                .priceLevel(entity.getPriceLevel() != null ? entity.getPriceLevel().intValue() : null)
                .address(entity.getAddress())
                .phoneNumber(entity.getPhoneNumber())
                .websiteUrl(entity.getWebsiteUrl())
                .openingHours(entity.getOpeningHours())
                .reviews(entity.getReviews())
                .photoUrls(entity.getPhotoUrls())
                .build();
    }
}
