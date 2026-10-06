package com.tripmind.services.ai.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripmind.domains.responses.PlaceResponse;

import java.math.BigDecimal;

/** Một địa điểm ở dạng gọn đưa cho mô hình (BR-504): đủ để chọn và để đề xuất, không hơn. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlaceView(String provider, String externalId, String name, String category, BigDecimal rating,
                        Integer priceLevel, String address, BigDecimal lat, BigDecimal lng, Boolean previouslyLiked) {

    public static PlaceView of(PlaceResponse p) {
        return new PlaceView(p.getProvider(), p.getExternalId(), p.getName(), p.getCategory(), p.getRating(),
                p.getPriceLevel(), p.getAddress(), p.getLatitude(), p.getLongitude(), null);
    }

    public PlaceView liked() {
        return new PlaceView(provider, externalId, name, category, rating, priceLevel, address, lat, lng, true);
    }
}
