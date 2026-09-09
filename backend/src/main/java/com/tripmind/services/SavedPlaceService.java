package com.tripmind.services;

import com.tripmind.domains.responses.SavedPlaceResponse;

import java.util.List;

public interface SavedPlaceService {

    SavedPlaceResponse savePlace(Long userId, Long placeId);

    void unsavePlace(Long userId, Long placeId);

    List<SavedPlaceResponse> getSavedPlaces(Long userId);

    boolean isPlaceSaved(Long userId, Long placeId);
}
