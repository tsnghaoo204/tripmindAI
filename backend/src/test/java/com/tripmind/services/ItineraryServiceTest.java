package com.tripmind.services;

import com.tripmind.domains.requests.CreateActivityRequest;
import com.tripmind.domains.requests.UpdateActivityRequest;
import com.tripmind.domains.responses.ActivityResponse;
import com.tripmind.domains.responses.ItineraryDayResponse;
import com.tripmind.domains.responses.ItineraryResponse;
import com.tripmind.entities.ActivityEntity;
import com.tripmind.entities.ItineraryDayEntity;
import com.tripmind.entities.PlaceEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.entities.UserEntity;
import com.tripmind.enums.ActivityType;
import com.tripmind.exceptions.AppException;
import com.tripmind.repositories.ActivityRepository;
import com.tripmind.repositories.ItineraryDayRepository;
import com.tripmind.repositories.PlaceRepository;
import com.tripmind.repositories.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItineraryServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private ItineraryDayRepository itineraryDayRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Spy
    private DistanceService distanceService = new DistanceService();

    @InjectMocks
    private ItineraryServiceImpl itineraryService;

    private UserEntity user;
    private TripEntity trip;
    private ItineraryDayEntity day1;
    private PlaceEntity placeA;
    private PlaceEntity placeB;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder().id(1L).email("user@tripmind.ai").name("User").build();
        trip = TripEntity.builder().id(10L).user(user).name("Du lịch Đà Nẵng").currency("VND").build();
        day1 = ItineraryDayEntity.builder().id(100L).trip(trip).dayNumber((short) 1).date(LocalDate.now()).build();

        placeA = PlaceEntity.builder()
                .id(501L)
                .name("Cầu Rồng")
                .latitude(new BigDecimal("16.0610"))
                .longitude(new BigDecimal("108.2274"))
                .build();

        placeB = PlaceEntity.builder()
                .id(502L)
                .name("Bãi biển Mỹ Khê")
                .latitude(new BigDecimal("16.0592"))
                .longitude(new BigDecimal("108.2435"))
                .build();
    }

    @Test
    @DisplayName("Should get full itinerary with calculated distance between spots")
    void shouldGetFullItineraryWithDistances() {
        ActivityEntity act1 = ActivityEntity.builder()
                .id(1L)
                .itineraryDay(day1)
                .orderIndex((short) 0)
                .title("Check-in Cầu Rồng")
                .place(placeA)
                .activityType(ActivityType.SIGHTSEEING)
                .build();

        ActivityEntity act2 = ActivityEntity.builder()
                .id(2L)
                .itineraryDay(day1)
                .orderIndex((short) 1)
                .title("Tắm biển Mỹ Khê")
                .place(placeB)
                .activityType(ActivityType.SIGHTSEEING)
                .transportationMode("MOTORBIKE")
                .build();

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(itineraryDayRepository.findByTripIdOrderByDayNumberAsc(10L)).thenReturn(List.of(day1));
        when(activityRepository.findByItineraryDayIdOrderByOrderIndexAsc(100L)).thenReturn(List.of(act1, act2));

        ItineraryResponse response = itineraryService.getItinerary(1L, 10L);

        assertThat(response).isNotNull();
        assertThat(response.getDays()).hasSize(1);
        ItineraryDayResponse dayRes = response.getDays().get(0);
        assertThat(dayRes.getActivities()).hasSize(2);

        ActivityResponse firstAct = dayRes.getActivities().get(0);
        assertThat(firstAct.getDistanceToNextMeters()).isNotNull();
        assertThat(firstAct.getDistanceToNextMeters()).isGreaterThan(1500L);
        assertThat(firstAct.getFormattedDistanceToNext()).contains("km");

        ActivityResponse secondAct = dayRes.getActivities().get(1);
        assertThat(secondAct.getDistanceToNextMeters()).isNull(); // Last activity has no next
    }

    @Test
    @DisplayName("Should add activity with auto-incremented orderIndex")
    void shouldAddActivitySuccessfully() {
        CreateActivityRequest request = CreateActivityRequest.builder()
                .dayId(100L)
                .title("Ăn bánh mì")
                .activityType(ActivityType.FOOD)
                .estimatedCost(30000L)
                .build();

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(itineraryDayRepository.findById(100L)).thenReturn(Optional.of(day1));
        when(activityRepository.findMaxOrderIndexByItineraryDayId(100L)).thenReturn((short) 2);
        when(activityRepository.save(any(ActivityEntity.class))).thenAnswer(i -> {
            ActivityEntity saved = i.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        ActivityResponse response = itineraryService.addActivity(1L, 10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderIndex()).isEqualTo((short) 3); // 2 + 1
        assertThat(response.getTitle()).isEqualTo("Ăn bánh mì");
        verify(activityRepository).save(any(ActivityEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when accessing trip of another user")
    void shouldThrowForbiddenWhenAccessingOtherUserTrip() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> itineraryService.getItinerary(999L, 10L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("Should reorder activities correctly")
    void shouldReorderActivities() {
        ActivityEntity act1 = ActivityEntity.builder().id(1L).itineraryDay(day1).orderIndex((short) 0).title("Act 1").build();
        ActivityEntity act2 = ActivityEntity.builder().id(2L).itineraryDay(day1).orderIndex((short) 1).title("Act 2").build();

        when(itineraryDayRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(day1));
        when(activityRepository.findByItineraryDayIdOrderByOrderIndexAsc(100L)).thenReturn(new ArrayList<>(List.of(act1, act2)));

        ItineraryDayResponse response = itineraryService.reorderActivities(1L, 100L, List.of(2L, 1L));

        assertThat(response).isNotNull();
        assertThat(act2.getOrderIndex()).isEqualTo((short) 0);
        assertThat(act1.getOrderIndex()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("Should delete activity and reindex remaining items")
    void shouldDeleteActivityAndReindex() {
        ActivityEntity act1 = ActivityEntity.builder().id(1L).itineraryDay(day1).orderIndex((short) 0).title("Act 1").build();
        ActivityEntity act2 = ActivityEntity.builder().id(2L).itineraryDay(day1).orderIndex((short) 1).title("Act 2").build();

        when(activityRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(act1));
        when(activityRepository.findByItineraryDayIdOrderByOrderIndexAsc(100L)).thenReturn(List.of(act2));

        itineraryService.deleteActivity(1L, 1L);

        verify(activityRepository).delete(act1);
        verify(activityRepository).flush();
        assertThat(act2.getOrderIndex()).isEqualTo((short) 0);
        verify(activityRepository).saveAll(any());
    }
}
