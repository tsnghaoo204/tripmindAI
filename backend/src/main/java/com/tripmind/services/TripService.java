package com.tripmind.services;

import com.tripmind.domains.requests.CreateTripRequest;
import com.tripmind.domains.requests.UpdateTripRequest;
import com.tripmind.domains.responses.TripResponse;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.TripPhase;

import java.util.List;

public interface TripService {

    TripResponse createTrip(Long userId, CreateTripRequest request);

    TripResponse getTrip(Long userId, Long tripId);

    /** {@code phase = null} trả mọi chuyến. */
    List<TripResponse> getTrips(Long userId, TripPhase phase);

    TripResponse updateTrip(Long userId, Long tripId, UpdateTripRequest request);

    TripResponse updatePhase(Long userId, Long tripId, TripPhase phase);

    void deleteTrip(Long userId, Long tripId);

    /**
     * Chuyến của đúng người gọi. Không có hoặc không phải của mình đều ném {@code 404}
     * (BR-104) — mọi dịch vụ khác cần kiểm sở hữu chuyến đều đi qua đây.
     */
    TripEntity getOwnedTrip(Long userId, Long tripId);
}
