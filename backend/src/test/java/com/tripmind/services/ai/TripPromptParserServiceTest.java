package com.tripmind.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.configurations.properties.TripProperties;
import com.tripmind.entities.DestinationEntity;
import com.tripmind.repositories.DestinationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TripPromptParserServiceTest {

    private final DestinationRepository destinations = mock(DestinationRepository.class);
    private TripPromptParserService parser;

    @BeforeEach
    void setUp() {
        LlmGateway llm = mock(LlmGateway.class);
        when(llm.isConfigured()).thenReturn(false);
        TripProperties props = mock(TripProperties.class);
        when(props.maxDays()).thenReturn(30);
        Clock clock = Clock.fixed(Instant.parse("2026-03-10T20:00:00Z"), ZoneOffset.UTC);
        parser = new TripPromptParserService(llm, destinations, new ObjectMapper(), clock, props);

        DestinationEntity daNangMoi = DestinationEntity.builder().id(7L).name("Khu đô thị Đà Nẵng Mới").build();
        DestinationEntity daNang = DestinationEntity.builder().id(3L).name("Đà Nẵng").build();
        when(destinations.findByNameContainingIgnoreCase(anyString())).thenReturn(List.of());
        when(destinations.findAll()).thenReturn(List.of(daNangMoi, daNang));
    }

    @Test
    @DisplayName("Không dấu vẫn khớp đúng điểm đến, ưu tiên khớp trọn tên; ngày theo đồng hồ tiêm vào")
    void accentInsensitiveExactMatch() {
        Map<String, Object> r = parser.parsePrompt("đi Da Nang 3 ngày 2 đêm cho 2 người");

        assertThat(r.get("destinationId")).isEqualTo(3L);
        assertThat(r.get("isCustomDestination")).isEqualTo(false);
        // 20:00 UTC = 03:00 ngày 11/3 giờ Việt Nam, nên ngày mai là 12/3.
        assertThat(r.get("startDate")).isEqualTo("2026-03-12");
        assertThat(r.get("days")).isEqualTo(3);
    }

    @Test
    @DisplayName("Số ngày quá lớn hoặc bằng 0 không làm vỡ phân tích mà bị kẹp vào [1, maxDays]")
    void daysAreClamped() {
        assertThat(parser.parsePrompt("đi Hue 99999999999 ngày").get("days")).isEqualTo(30);
        assertThat(parser.parsePrompt("đi Hue 0 ngày").get("days")).isEqualTo(1);
        assertThat(parser.parsePrompt("đi Hue 2 ngày cho 99999999999 người").get("travelers")).isEqualTo(50);
    }
}
