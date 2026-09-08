package com.tripmind.services;

import com.tripmind.domains.requests.CreateTripRequest;
import com.tripmind.entities.DestinationEntity;
import com.tripmind.entities.ItineraryDayEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.entities.UserEntity;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.TripRepository;
import com.tripmind.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final DestinationService destinationService;

    @Override
    @Transactional
    public TripEntity createTrip(Long userId, CreateTripRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found with ID: " + userId));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "End date cannot be before start date");
        }

        DestinationEntity destination = destinationService.getOrCreateDestination(
                request.getDestinationId(),
                request.getDestinationPlaceId(),
                request.getDestinationData()
        );

        TripEntity trip = TripEntity.builder()
                .user(user)
                .destination(destination)
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .travelers((short) request.getTravelers())
                .budget(request.getBudget())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .days(new ArrayList<>())
                .build();

        long numDays = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        for (short i = 1; i <= numDays; i++) {
            LocalDate date = request.getStartDate().plusDays(i - 1);
            ItineraryDayEntity day = ItineraryDayEntity.builder()
                    .trip(trip)
                    .dayNumber(i)
                    .date(date)
                    .build();
            trip.getDays().add(day);
        }

        trip = tripRepository.save(trip);
        log.info("Created trip ID={} with {} days for user ID={}", trip.getId(), numDays, userId);
        return trip;
    }

    @Override
    @Transactional(readOnly = true)
    public TripEntity getTripById(Long userId, Long tripId) {
        return tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Trip not found or access denied"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripEntity> getTripsByUser(Long userId) {
        return tripRepository.findByUserIdOrderByStartDateDesc(userId);
    }

    @Override
    @Transactional
    public void deleteTrip(Long userId, Long tripId) {
        TripEntity trip = getTripById(userId, tripId);
        tripRepository.delete(trip);
        log.info("Deleted trip ID={} for user ID={}", tripId, userId);
    }
}
