package com.tripmind.services;

import com.tripmind.domains.requests.CreateActivityRequest;
import com.tripmind.domains.requests.UpdateActivityRequest;
import com.tripmind.domains.responses.ActivityResponse;
import com.tripmind.domains.responses.ItineraryDayResponse;
import com.tripmind.domains.responses.ItineraryResponse;

import java.util.List;

public interface ItineraryService {

    ItineraryResponse getItinerary(Long userId, Long tripId);

    ActivityResponse addActivity(Long userId, Long tripId, CreateActivityRequest request);

    ActivityResponse updateActivity(Long userId, Long activityId, UpdateActivityRequest request);

    void deleteActivity(Long userId, Long activityId);

    ItineraryDayResponse reorderActivities(Long userId, Long dayId, List<Long> activityIds);
}
