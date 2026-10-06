package com.tripmind.services.ai.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.domains.models.GroupProfile;
import com.tripmind.domains.responses.ActivityResponse;
import com.tripmind.domains.responses.BudgetSummaryResponse;
import com.tripmind.domains.responses.ItineraryResponse;
import com.tripmind.domains.responses.PlaceResponse;
import com.tripmind.domains.responses.TripResponse;
import com.tripmind.domains.responses.TripWeatherResponse;
import com.tripmind.domains.responses.UserPreferencesResponse;
import com.tripmind.services.BudgetService;
import com.tripmind.services.ItineraryService;
import com.tripmind.services.SavedPlaceService;
import com.tripmind.services.TripService;
import com.tripmind.services.UserPreferencesService;
import com.tripmind.services.WeatherService;
import com.tripmind.services.ai.AgentToolSet;
import com.tripmind.services.ai.AgentTurn;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Công cụ đọc dữ liệu chuyến đi. Mỗi công cụ gọi lại đúng dịch vụ mà REST API dùng, với
 * {@code userId} của người đang hỏi — luật sở hữu chuyến áp dụng y như đường REST (BR-103).
 * Kết quả được cắt gọn còn vài trường cần thiết để tiết kiệm token (BR-504).
 */
@Component
@RequiredArgsConstructor
public class TripReadTools implements AgentToolSet {

    private final TripService tripService;
    private final ItineraryService itineraryService;
    private final UserPreferencesService userPreferencesService;
    private final SavedPlaceService savedPlaceService;
    private final BudgetService budgetService;
    private final WeatherService weatherService;
    private final PlaceRatingLookup placeRatingLookup;
    private final ObjectMapper objectMapper;

    @Tool(name = "get_current_trip", description = """
            Read the trip the user is chatting about: destination, dates, travelers, budget, currency,
            travel style, preferences, group profile (children, seniors, diet) and current phase.""")
    public String getCurrentTrip(ToolContext context) {
        AgentTurn turn = ToolSupport.turn(context);
        TripResponse trip = tripService.getTrip(turn.getUserId(), turn.getTripId());
        return ToolSupport.json(objectMapper, new TripView(trip.getName(), trip.getDestination().getName(),
                trip.getDestination().getCountry(), trip.getStartDate(), trip.getEndDate(), trip.getTotalDays(),
                trip.getTravelers(), trip.getBudget(), trip.getCurrency(),
                trip.getTravelStyle() == null ? null : trip.getTravelStyle().name(),
                trip.getPreferences(), trip.getGroupProfile(), trip.getPhase().name(), trip.getToday()));
    }

    @Tool(name = "get_itinerary", description = """
            Read the itinerary day by day: activity id, title, type, times, place (with coordinates),
            estimated cost and status, plus the travel distance to the next activity.""")
    public String getItinerary(
            @ToolParam(description = "Only this day number (1-based). Omit to read all days.", required = false)
            Integer dayNumber,
            ToolContext context) {
        AgentTurn turn = ToolSupport.turn(context);
        ItineraryResponse itinerary = itineraryService.getItinerary(turn.getUserId(), turn.getTripId());
        List<DayView> days = itinerary.getDays().stream()
                .filter(d -> dayNumber == null || d.getDayNumber() == dayNumber.shortValue())
                .map(d -> new DayView(d.getDayNumber(), d.getDate(), d.getFormattedTotalDayDistance(),
                        d.getActivities().stream().map(TripReadTools::activityView).toList()))
                .toList();
        return ToolSupport.json(objectMapper, days);
    }

    @Tool(name = "get_user_preferences", description = """
            Read the user's default travel preferences and the places they liked or disliked on past trips.
            Never suggest a disliked place again.""")
    public String getUserPreferences(ToolContext context) {
        AgentTurn turn = ToolSupport.turn(context);
        UserPreferencesResponse prefs = userPreferencesService.get(turn.getUserId());
        return ToolSupport.json(objectMapper, new PreferencesView(
                prefs.getTravelStyle() == null ? null : prefs.getTravelStyle().name(),
                prefs.getBudgetPreference() == null ? null : prefs.getBudgetPreference().name(),
                prefs.getPreferences(),
                placeRatingLookup.likedNames(turn.getUserId()),
                placeRatingLookup.dislikedNames(turn.getUserId())));
    }

    @Tool(name = "get_saved_places", description = "Read the places the user bookmarked as favorites.")
    public String getSavedPlaces(ToolContext context) {
        AgentTurn turn = ToolSupport.turn(context);
        List<PlaceView> places = savedPlaceService.getSavedPlaces(turn.getUserId()).stream()
                .map(saved -> PlaceView.of(saved.getPlace()))
                .toList();
        places.forEach(p -> turn.getSeenPlaceIds().add(p.externalId()));
        return ToolSupport.json(objectMapper, places);
    }

    @Tool(name = "calculate_trip_budget", description = """
            Budget status: total budget, estimated cost from activities, actual spending, remaining amount,
            warning level, breakdown by category and today's spending allowance.""")
    public String calculateTripBudget(ToolContext context) {
        AgentTurn turn = ToolSupport.turn(context);
        BudgetSummaryResponse budget = budgetService.getTripBudgetSummary(turn.getUserId(), turn.getTripId());
        return ToolSupport.json(objectMapper, budget);
    }

    @Tool(name = "get_weather", description = """
            Weather for each day of the trip. Every day has a source label: FORECAST, CLIMATE_NORMAL
            (historical average, not a forecast) or OBSERVED. Always tell the user which one you used.""")
    public String getWeather(ToolContext context) {
        AgentTurn turn = ToolSupport.turn(context);
        TripWeatherResponse weather = weatherService.getTripWeather(turn.getUserId(), turn.getTripId());
        return ToolSupport.json(objectMapper, new WeatherView(weather.getReason(), weather.getDays().stream()
                .map(d -> new WeatherDayView(d.getDayNumber(), d.getDate(), d.getSource().name(), d.getSummary(),
                        d.getTempMin(), d.getTempMax(), d.getPrecipitationProbability()))
                .toList()));
    }

    private static ActivityView activityView(ActivityResponse a) {
        PlaceResponse p = a.getPlace();
        return new ActivityView(a.getId(), a.getTitle(), a.getActivityType().name(), a.getStartTime(), a.getEndTime(),
                p == null ? null : new ActivityPlaceView(p.getName(), p.getProvider(), p.getExternalId(),
                        p.getLatitude(), p.getLongitude()),
                a.getEstimatedCost(), a.getStatus().name(), a.getFormattedDistanceToNext());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TripView(String name, String destination, String country, LocalDate startDate, LocalDate endDate,
                    Integer totalDays, Short travelers, Long budget, String currency, String travelStyle,
                    List<String> preferences, GroupProfile groupProfile, String phase, LocalDate today) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record DayView(Short dayNumber, LocalDate date, String totalDistance, List<ActivityView> activities) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ActivityView(Long id, String title, String type, LocalTime start, LocalTime end, ActivityPlaceView place,
                        Long estimatedCost, String status, String distanceToNext) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ActivityPlaceView(String name, String provider, String externalId, BigDecimal lat, BigDecimal lng) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PreferencesView(String travelStyle, String budgetPreference, List<String> preferences,
                           List<String> likedPlaces, List<String> dislikedPlaces) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record WeatherView(String reason, List<WeatherDayView> days) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record WeatherDayView(Integer dayNumber, LocalDate date, String source, String summary, Double tempMin,
                          Double tempMax, Integer rainChance) {
    }
}
