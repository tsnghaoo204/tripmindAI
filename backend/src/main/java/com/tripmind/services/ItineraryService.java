package com.tripmind.services;

import com.tripmind.domains.requests.CreateActivityRequest;
import com.tripmind.domains.requests.UpdateActivityRequest;
import com.tripmind.domains.responses.ActivityResponse;
import com.tripmind.domains.responses.CostEstimateResponse;
import com.tripmind.domains.responses.ItineraryDayResponse;
import com.tripmind.domains.responses.ItineraryResponse;
import com.tripmind.enums.ActivityType;

import java.util.List;

public interface ItineraryService {

    ItineraryResponse getItinerary(Long userId, Long tripId);

    ActivityResponse addActivity(Long userId, Long tripId, CreateActivityRequest request);

    ActivityResponse updateActivity(Long userId, Long activityId, UpdateActivityRequest request);

    void deleteActivity(Long userId, Long activityId);

    ItineraryDayResponse reorderActivities(Long userId, Long dayId, List<Long> activityIds);

    /**
     * Gợi ý chi phí cho một hoạt động: theo {@code placeId} đã có trong CSDL, hoặc theo
     * {@code priceLevel} lấy từ kết quả tìm kiếm khi địa điểm chưa được lưu.
     */
    CostEstimateResponse estimateCost(Long userId, Long tripId, Long placeId, Integer priceLevel, ActivityType activityType);
}
