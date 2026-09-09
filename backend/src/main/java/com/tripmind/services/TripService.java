package com.tripmind.services;

import com.tripmind.domains.requests.CreateTripRequest;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.entities.TripEntity;

import java.util.List;

public interface TripService {

    TripEntity createTrip(Long userId, CreateTripRequest request);

    TripEntity getTripById(Long userId, Long tripId);

    List<TripEntity> getTripsByUser(Long userId);

    TripEntity updateTrip(Long userId, Long tripId, com.tripmind.domains.requests.UpdateTripRequest request);

    void deleteTrip(Long userId, Long tripId);
}
