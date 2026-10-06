package com.tripmind.services;

import com.tripmind.configurations.properties.TripProperties;
import com.tripmind.domains.models.GroupProfile;
import com.tripmind.domains.requests.CreateTripRequest;
import com.tripmind.domains.requests.UpdateTripRequest;
import com.tripmind.domains.responses.TripResponse;
import com.tripmind.entities.DestinationEntity;
import com.tripmind.entities.ItineraryDayEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.entities.UserEntity;
import com.tripmind.entities.UserPreferencesEntity;
import com.tripmind.enums.TripPhase;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.ExpenseRepository;
import com.tripmind.repositories.TripRepository;
import com.tripmind.repositories.UserPreferencesRepository;
import com.tripmind.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final ExpenseRepository expenseRepository;
    private final DestinationService destinationService;
    private final TripMapper tripMapper;
    private final TripClock tripClock;
    private final TripProperties tripProperties;

    @Override
    @Transactional
    public TripResponse createTrip(Long userId, CreateTripRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found with ID: " + userId));

        validateDateRange(request.getStartDate(), request.getEndDate());
        GroupProfile group = request.getGroupProfile() == null ? GroupProfile.empty() : request.getGroupProfile();
        validateGroup(group, request.getTravelers());

        DestinationEntity destination = destinationService.getOrCreateDestination(
                request.getDestinationId(),
                request.getDestinationPlaceId(),
                request.getDestinationData()
        );

        // Trường nào người dùng không chọn ở bước tạo chuyến thì lấy mặc định của tài khoản.
        UserPreferencesEntity defaults = userPreferencesRepository.findByUserId(userId).orElse(null);
        List<String> preferences = request.getPreferences() != null
                ? normalize(request.getPreferences())
                : defaults == null ? new ArrayList<>() : new ArrayList<>(defaults.getPreferences());

        TripEntity trip = TripEntity.builder()
                .user(user)
                .destination(destination)
                .name(request.getName().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .travelers((short) request.getTravelers())
                .budget(request.getBudget())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .travelStyle(request.getTravelStyle() != null ? request.getTravelStyle()
                        : defaults == null ? null : defaults.getTravelStyle())
                .budgetPreference(request.getBudgetPreference() != null ? request.getBudgetPreference()
                        : defaults == null ? null : defaults.getBudgetPreference())
                .preferences(preferences)
                .groupProfile(group)
                .days(new ArrayList<>())
                .build();

        int numDays = trip.lengthInDays();
        for (int i = 1; i <= numDays; i++) {
            trip.getDays().add(newDay(trip, i, request.getStartDate().plusDays(i - 1L)));
        }

        // Người dùng mới chưa có sở thích mặc định: lấy luôn lựa chọn của chuyến đầu tiên.
        if (defaults == null && (trip.getTravelStyle() != null || trip.getBudgetPreference() != null
                || !preferences.isEmpty())) {
            userPreferencesRepository.save(UserPreferencesEntity.builder()
                    .user(user)
                    .travelStyle(trip.getTravelStyle())
                    .budgetPreference(trip.getBudgetPreference())
                    .preferences(new ArrayList<>(preferences))
                    .build());
        }

        trip = tripRepository.save(trip);
        log.info("Created trip ID={} with {} days for user ID={}", trip.getId(), numDays, userId);
        return tripMapper.toDetail(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public TripEntity getOwnedTrip(Long userId, Long tripId) {
        return tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Trip not found or access denied"));
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse getTrip(Long userId, Long tripId) {
        return tripMapper.toDetail(getOwnedTrip(userId, tripId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getTrips(Long userId, TripPhase phase) {
        List<TripEntity> trips = tripRepository.findByUserIdOrderByStartDateDesc(userId).stream()
                .filter(trip -> phase == null || tripClock.phase(trip) == phase)
                .toList();
        return tripMapper.toSummaries(trips);
    }

    @Override
    @Transactional
    public TripResponse updateTrip(Long userId, Long tripId, UpdateTripRequest request) {
        TripEntity trip = getOwnedTrip(userId, tripId);
        if (request.getName() != null && !request.getName().isBlank()) {
            trip.setName(request.getName().trim());
        }
        if (request.getTravelers() != null) {
            trip.setTravelers(request.getTravelers().shortValue());
        }
        if (request.getBudget() != null) {
            trip.setBudget(request.getBudget());
        }
        if (request.getCurrency() != null && !request.getCurrency().equals(trip.getCurrency())) {
            // Không quy đổi tỷ giá: đổi tiền của chuyến khi đã có chi tiêu sẽ cộng lẫn hai loại tiền.
            if (expenseRepository.existsByTripId(tripId)) {
                throw new AppException(ErrorCode.CURRENCY_MISMATCH,
                        "Cannot change currency of a trip that already has expenses in " + trip.getCurrency());
            }
            trip.setCurrency(request.getCurrency());
        }
        if (request.getTravelStyle() != null) {
            trip.setTravelStyle(request.getTravelStyle());
        }
        if (request.getBudgetPreference() != null) {
            trip.setBudgetPreference(request.getBudgetPreference());
        }
        if (request.getPreferences() != null) {
            trip.setPreferences(normalize(request.getPreferences()));
        }
        if (request.getGroupProfile() != null) {
            trip.setGroupProfile(request.getGroupProfile());
        }
        validateGroup(trip.getGroupProfile(), trip.getTravelers());

        if (request.getStartDate() != null || request.getEndDate() != null) {
            LocalDate start = request.getStartDate() != null ? request.getStartDate() : trip.getStartDate();
            // Chỉ gửi ngày đi = dời cả chuyến, giữ nguyên số ngày.
            LocalDate end = request.getEndDate() != null
                    ? request.getEndDate()
                    : start.plusDays(trip.lengthInDays() - 1L);
            resizeDays(trip, start, end, request.isConfirmDropDays());
        }

        trip = tripRepository.save(trip);
        log.info("Updated trip ID={} for user ID={}", tripId, userId);
        return tripMapper.toDetail(trip);
    }

    @Override
    @Transactional
    public TripResponse updatePhase(Long userId, Long tripId, TripPhase phase) {
        TripEntity trip = getOwnedTrip(userId, tripId);
        trip.setPhaseOverride(phase);
        trip = tripRepository.save(trip);
        log.info("Trip ID={} phase override = {}", tripId, phase);
        return tripMapper.toDetail(trip);
    }

    @Override
    @Transactional
    public void deleteTrip(Long userId, Long tripId) {
        TripEntity trip = getOwnedTrip(userId, tripId);
        tripRepository.delete(trip);
        log.info("Deleted trip ID={} for user ID={}", tripId, userId);
    }

    /**
     * Đặt lại khoảng ngày (BR-203). Ngày thứ {@code i} luôn có {@code date = start + (i-1)}:
     * ngày còn giữ thì đổi ngày, thiếu thì sinh thêm ở cuối, thừa thì xoá các ngày cuối.
     * Ràng buộc {@code UNIQUE (trip_id, date)} được hoãn tới lúc commit (V2), nên dời
     * chuyến không vấp trùng ngày giữa chừng.
     */
    private void resizeDays(TripEntity trip, LocalDate start, LocalDate end, boolean confirmDropDays) {
        validateDateRange(start, end);
        int newLength = (int) ChronoUnit.DAYS.between(start, end) + 1;

        List<ItineraryDayEntity> days = new ArrayList<>(trip.getDays());
        days.sort(Comparator.comparing(ItineraryDayEntity::getDayNumber));

        List<ItineraryDayEntity> toDrop = days.stream().filter(d -> d.getDayNumber() > newLength).toList();
        List<Short> nonEmpty = toDrop.stream()
                .filter(d -> !d.getActivities().isEmpty())
                .map(ItineraryDayEntity::getDayNumber)
                .toList();
        if (!nonEmpty.isEmpty() && !confirmDropDays) {
            throw new AppException(ErrorCode.DAYS_HAVE_ACTIVITIES,
                    "Days " + nonEmpty + " still have activities; resend with confirmDropDays=true to delete them",
                    Map.of("dayNumbers", nonEmpty));
        }
        trip.getDays().removeAll(toDrop);

        for (ItineraryDayEntity day : trip.getDays()) {
            day.setDate(start.plusDays(day.getDayNumber() - 1L));
        }
        for (int i = trip.getDays().size() + 1; i <= newLength; i++) {
            trip.getDays().add(newDay(trip, i, start.plusDays(i - 1L)));
        }

        trip.setStartDate(start);
        trip.setEndDate(end);
        log.info("Trip ID={} resized to {} → {} ({} days, dropped {})", trip.getId(), start, end, newLength, toDrop.size());
    }

    private ItineraryDayEntity newDay(TripEntity trip, int dayNumber, LocalDate date) {
        return ItineraryDayEntity.builder()
                .trip(trip)
                .dayNumber((short) dayNumber)
                .date(date)
                .build();
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE, "End date cannot be before start date");
        }
        long length = ChronoUnit.DAYS.between(start, end) + 1;
        if (length > tripProperties.maxDays()) {
            throw new AppException(ErrorCode.TRIP_TOO_LONG,
                    "Trip cannot be longer than " + tripProperties.maxDays() + " days",
                    Map.of("maxDays", tripProperties.maxDays(), "requestedDays", length));
        }
    }

    private void validateGroup(GroupProfile group, int travelers) {
        if (group == null) {
            return;
        }
        if (group.childrenCount() + group.seniorsCount() > travelers) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "children + seniors (" + (group.childrenCount() + group.seniorsCount())
                            + ") cannot exceed travelers (" + travelers + ")");
        }
    }

    private List<String> normalize(List<String> values) {
        return new ArrayList<>(values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }
}
