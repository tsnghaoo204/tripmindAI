package com.tripmind.services;

import com.tripmind.entities.DestinationEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.TripPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TripClockTest {

    private TripEntity trip(String timezone, LocalDate start, LocalDate end) {
        return TripEntity.builder()
                .destination(DestinationEntity.builder().timezone(timezone).build())
                .startDate(start)
                .endDate(end)
                .build();
    }

    @Test
    @DisplayName("Hôm nay tính theo múi giờ điểm đến: 17:30 UTC đã là ngày mới ở Tokyo")
    void todayUsesDestinationTimezone() {
        // 2026-10-19T17:30Z = 2026-10-20 02:30 ở Tokyo
        Clock clock = Clock.fixed(Instant.parse("2026-10-19T17:30:00Z"), ZoneOffset.UTC);
        TripClock tripClock = new TripClock(clock);
        TripEntity tokyo = trip("Asia/Tokyo", LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22));
        TripEntity london = trip("Europe/London", LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22));

        assertThat(tripClock.today(tokyo)).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(tripClock.phase(tokyo)).isEqualTo(TripPhase.DURING);
        assertThat(tripClock.phase(london)).isEqualTo(TripPhase.BEFORE);
    }

    @Test
    @DisplayName("Đặt tay DURING trước ngày đi thì 'hôm nay' mô phỏng là ngày đầu tiên")
    void manualDuringClampsToTripRange() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-01T03:00:00Z"), ZoneOffset.UTC);
        TripClock tripClock = new TripClock(clock);
        TripEntity trip = trip("Asia/Ho_Chi_Minh", LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22));
        trip.setPhaseOverride(TripPhase.DURING);

        assertThat(tripClock.phase(trip)).isEqualTo(TripPhase.DURING);
        assertThat(tripClock.effectiveToday(trip)).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(tripClock.isSimulated(trip)).isTrue();
    }

    @Test
    @DisplayName("Múi giờ hỏng thì dùng UTC thay vì ném lỗi")
    void invalidTimezoneFallsBackToUtc() {
        TripClock tripClock = new TripClock(Clock.fixed(Instant.parse("2026-10-01T03:00:00Z"), ZoneOffset.UTC));
        TripEntity trip = trip("Not/AZone", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2));
        assertThat(tripClock.today(trip)).isEqualTo(LocalDate.of(2026, 10, 1));
    }
}
