package com.tripmind.services;

import com.tripmind.domains.responses.SavedPlaceResponse;
import com.tripmind.entities.PlaceEntity;
import com.tripmind.entities.SavedPlaceEntity;
import com.tripmind.entities.UserEntity;
import com.tripmind.repositories.PlaceRepository;
import com.tripmind.repositories.SavedPlaceRepository;
import com.tripmind.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedPlaceServiceTest {

    @Mock
    private SavedPlaceRepository savedPlaceRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SavedPlaceServiceImpl savedPlaceService;

    private UserEntity user;
    private PlaceEntity place;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder().id(1L).email("user@tripmind.ai").name("User").build();
        place = PlaceEntity.builder().id(10L).name("Cầu Vàng Bà Nà").build();
    }

    @Test
    @DisplayName("Should save place successfully")
    void shouldSavePlace() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(placeRepository.findById(10L)).thenReturn(Optional.of(place));
        when(savedPlaceRepository.findByUserIdAndPlaceId(1L, 10L)).thenReturn(Optional.empty());

        SavedPlaceEntity savedEntity = SavedPlaceEntity.builder()
                .id(100L)
                .user(user)
                .place(place)
                .createdAt(Instant.now())
                .build();
        when(savedPlaceRepository.save(any(SavedPlaceEntity.class))).thenReturn(savedEntity);

        SavedPlaceResponse response = savedPlaceService.savePlace(1L, 10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getPlace().getName()).isEqualTo("Cầu Vàng Bà Nà");
        verify(savedPlaceRepository).save(any(SavedPlaceEntity.class));
    }

    @Test
    @DisplayName("Should unsave place successfully")
    void shouldUnsavePlace() {
        savedPlaceService.unsavePlace(1L, 10L);
        verify(savedPlaceRepository).deleteByUserIdAndPlaceId(1L, 10L);
    }

    @Test
    @DisplayName("Should retrieve saved places list")
    void shouldGetSavedPlaces() {
        SavedPlaceEntity entity = SavedPlaceEntity.builder()
                .id(100L)
                .user(user)
                .place(place)
                .createdAt(Instant.now())
                .build();

        when(savedPlaceRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(entity));

        List<SavedPlaceResponse> responses = savedPlaceService.getSavedPlaces(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getPlace().getName()).isEqualTo("Cầu Vàng Bà Nà");
    }
}
