package com.tripmind.services;

import com.tripmind.domains.responses.ActivityResponse;
import com.tripmind.domains.responses.ItineraryDayResponse;
import com.tripmind.domains.responses.ItineraryResponse;
import com.tripmind.domains.responses.PlaceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IcsExporterTest {

    private final IcsExporter exporter = new IcsExporter("tripmind",
            Clock.fixed(Instant.parse("2026-10-06T00:00:00Z"), ZoneOffset.UTC));

    @Test
    @DisplayName("Giờ địa phương đổi sang UTC, hoạt động không giờ gom thành một sự kiện cả ngày, UID cố định")
    void exportsEvents() {
        ItineraryResponse itinerary = ItineraryResponse.builder().tripName("Đà Nẵng, mùa thu").currency("VND").days(List.of(
                ItineraryDayResponse.builder().id(11L).dayNumber((short) 1).date(LocalDate.of(2026, 10, 20)).activities(List.of(
                        ActivityResponse.builder().id(101L).title("Cầu Rồng; phun lửa").startTime(LocalTime.of(21, 0))
                                .estimatedCost(0L)
                                .place(PlaceResponse.builder().name("Cầu Rồng").address("Nguyễn Văn Linh, Đà Nẵng")
                                        .latitude(new BigDecimal("16.0610")).longitude(new BigDecimal("108.2274")).build())
                                .build(),
                        ActivityResponse.builder().id(102L).title("Chợ Hàn").build(),
                        ActivityResponse.builder().id(103L).title("Bảo tàng Chăm").build())).build())).build();

        String ics = exporter.export(itinerary, ZoneId.of("Asia/Ho_Chi_Minh"));

        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n");
        assertThat(ics).contains("UID:activity-101@tripmind", "DTSTART:20261020T140000Z", "DTEND:20261020T150000Z");
        assertThat(ics).contains("SUMMARY:Cầu Rồng\\; phun lửa", "GEO:16.0610;108.2274");
        assertThat(ics).contains("UID:day-11@tripmind", "DTSTART;VALUE=DATE:20261020", "SUMMARY:Ngày 1 · Chợ Hàn → Bảo tàng Chăm");
        assertThat(ics).contains("X-WR-CALNAME:Đà Nẵng\\, mùa thu");
    }

    @Test
    @DisplayName("Dòng dài gập ở 75 byte UTF-8, không cắt giữa ký tự tiếng Việt")
    void foldsLongLines() {
        String line = "SUMMARY:" + "Thưởng thức ẩm thực đường phố ".repeat(5);
        String folded = IcsExporter.fold(line);

        for (String part : folded.split("\r\n")) {
            assertThat(part.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75);
        }
        assertThat(folded.replace("\r\n ", "")).isEqualTo(line);
    }
}
