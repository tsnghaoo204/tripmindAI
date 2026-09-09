package com.tripmind.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.entities.PlaceEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class DistanceService {

    private static final double EARTH_RADIUS_METERS = 6371000.0;

    @Getter
    @Builder
    public static class TravelEstimate {
        private Long distanceMeters;
        private String formattedDistance;
        private Integer travelTimeMinutes;
        private String formattedTravelTime;
    }

    /**
     * Calculates geodesic distance between two GPS points in meters using Haversine formula.
     */
    public long calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return Math.round(EARTH_RADIUS_METERS * c);
    }

    public TravelEstimate estimateTravel(PlaceEntity origin, PlaceEntity destination, String transportationMode) {
        if (origin == null || destination == null) {
            return null;
        }

        BigDecimal lat1 = origin.getLatitude();
        BigDecimal lon1 = origin.getLongitude();
        BigDecimal lat2 = destination.getLatitude();
        BigDecimal lon2 = destination.getLongitude();

        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }

        long distanceMeters = calculateDistanceMeters(
                lat1.doubleValue(), lon1.doubleValue(),
                lat2.doubleValue(), lon2.doubleValue()
        );

        int travelMinutes = estimateTravelMinutes(distanceMeters, transportationMode);

        return TravelEstimate.builder()
                .distanceMeters(distanceMeters)
                .formattedDistance(formatDistance(distanceMeters))
                .travelTimeMinutes(travelMinutes)
                .formattedTravelTime(formatDuration(travelMinutes))
                .build();
    }

    public int estimateTravelMinutes(long distanceMeters, String transportationMode) {
        if (distanceMeters <= 0) {
            return 0;
        }

        String mode = transportationMode != null ? transportationMode.trim().toUpperCase() : "MOTORBIKE";
        double speedMetersPerMinute;
        int baseBufferMinutes;

        switch (mode) {
            case "WALKING":
            case "WALK":
                speedMetersPerMinute = 75.0; // ~4.5 km/h
                baseBufferMinutes = 1;
                break;
            case "BICYCLE":
            case "BIKE":
                speedMetersPerMinute = 250.0; // ~15 km/h
                baseBufferMinutes = 2;
                break;
            case "CAR":
            case "TAXI":
            case "DRIVING":
                speedMetersPerMinute = 580.0; // ~35 km/h
                baseBufferMinutes = 4; // parking/traffic buffer
                break;
            case "MOTORBIKE":
            case "SCOOTER":
            default:
                speedMetersPerMinute = 500.0; // ~30 km/h
                baseBufferMinutes = 2;
                break;
        }

        int calculatedMinutes = (int) Math.ceil(distanceMeters / speedMetersPerMinute) + baseBufferMinutes;
        return Math.max(calculatedMinutes, 2);
    }

    public String formatDistance(long distanceMeters) {
        if (distanceMeters < 1000) {
            return distanceMeters + " m";
        }
        double km = distanceMeters / 1000.0;
        return String.format("%.1f km", km);
    }

    public String formatDuration(int minutes) {
        if (minutes <= 0) {
            return "Tại chỗ";
        }
        if (minutes < 60) {
            return minutes + " phút";
        }
        int hours = minutes / 60;
        int remMinutes = minutes % 60;
        if (remMinutes == 0) {
            return hours + " giờ";
        }
        return hours + " giờ " + remMinutes + " phút";
    }

    /**
     * Generates an intelligent, flexible "ideal timing tip" based on place characteristics.
     */
    public String getIdealTimingTip(PlaceEntity place, String activityTitle) {
        String searchText = "";
        if (activityTitle != null) {
            searchText += " " + activityTitle.toLowerCase();
        }
        if (place != null) {
            if (place.getName() != null) {
                searchText += " " + place.getName().toLowerCase();
            }
            if (place.getCategory() != null) {
                searchText += " " + place.getCategory().toLowerCase();
            }
        }

        if (searchText.contains("bình minh") || searchText.contains("sunrise") || searchText.contains("hải đăng")
                || searchText.contains("mũi né") || searchText.contains("đồi cát")) {
            return "05:00 - 06:30 (Ngắm bình minh & chụp ảnh đẹp nhất)";
        }

        if (searchText.contains("hoàng hôn") || searchText.contains("sunset") || searchText.contains("rooftop")
                || searchText.contains("hồ tây") || searchText.contains("bãi biển") || searchText.contains("bãi tắm")) {
            return "16:30 - 18:30 (Ngắm hoàng hôn lãng mạn, thời tiết dịu mát)";
        }

        if (searchText.contains("chợ đêm") || searchText.contains("night market") || searchText.contains("phố đi bộ")
                || searchText.contains("walking street")) {
            return "18:30 - 21:30 (Thời điểm sầm uất, nhộn nhịp ẩm thực)";
        }

        if (searchText.contains("cà phê") || searchText.contains("cafe") || searchText.contains("coffee")
                || searchText.contains("bánh mì") || searchText.contains("ăn sáng") || searchText.contains("phở")) {
            return "07:00 - 09:00 (Thưởng thức bữa sáng & cà phê nạp năng lượng)";
        }

        if (searchText.contains("ăn trưa") || searchText.contains("quán cơm") || searchText.contains("cơm niêu")
                || searchText.contains("lunch")) {
            return "11:30 - 13:00 (Khung giờ ăn trưa phù hợp)";
        }

        if (searchText.contains("hải sản") || searchText.contains("quán nhậu") || searchText.contains("lẩu")
                || searchText.contains("nướng") || searchText.contains("ăn tối") || searchText.contains("dinner")) {
            return "18:00 - 20:30 (Khung giờ ăn tối & gặp gỡ bạn bè)";
        }

        if (searchText.contains("bảo tàng") || searchText.contains("museum") || searchText.contains("chùa")
                || searchText.contains("đền") || searchText.contains("di tích") || searchText.contains("lăng")
                || searchText.contains("dinh độc lập")) {
            return "08:30 - 10:30 hoặc 14:30 - 16:30 (Nên đi sáng sớm hoặc chiều để tránh nắng)";
        }

        if (searchText.contains("bar") || searchText.contains("pub") || searchText.contains("lounge")) {
            return "20:30 - 23:30 (Không khí về đêm sôi động)";
        }

        // Check opening hours if present
        if (place != null && place.getOpeningHours() != null) {
            JsonNode openingHours = place.getOpeningHours();
            if (openingHours.has("weekday_text") && openingHours.get("weekday_text").isArray() && !openingHours.get("weekday_text").isEmpty()) {
                String firstDay = openingHours.get("weekday_text").get(0).asText();
                return "Mở cửa: " + firstDay;
            }
        }

        return "Thời gian linh hoạt (Nên đi buổi sáng mát mẻ hoặc chiều tà)";
    }
}
