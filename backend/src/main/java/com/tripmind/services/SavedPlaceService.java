package com.tripmind.services;

import com.tripmind.domains.responses.SavedPlaceResponse;

import java.util.List;

public interface SavedPlaceService {

    SavedPlaceResponse savePlace(Long userId, Long placeId);

    /** Lưu một kết quả tìm kiếm chưa có trong CSDL; địa điểm được nạp với {@code adopted_via = SAVED}. */
    SavedPlaceResponse saveExternalPlace(Long userId, String provider, String externalId);

    void unsavePlace(Long userId, Long placeId);

    List<SavedPlaceResponse> getSavedPlaces(Long userId);

    boolean isPlaceSaved(Long userId, Long placeId);
}
