package com.tripmind.services;

import com.tripmind.entities.TripEntity;
import com.tripmind.enums.TripPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * "Hôm nay" của một chuyến đi luôn tính theo múi giờ của điểm đến, không theo múi giờ máy chủ.
 * 23:30 ở Đà Nẵng đã là ngày mới, trong khi UTC vẫn là hôm trước — dùng nhầm thì chuyến
 * "bắt đầu hôm nay" vẫn hiện "chưa đi", và hạn mức chi tiêu của ngày bị tính sai.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TripClock {

    private final Clock clock;

    public Instant now() {
        return clock.instant();
    }

    public ZoneId zoneOf(TripEntity trip) {
        String timezone = trip.getDestination() == null ? null : trip.getDestination().getTimezone();
        if (timezone == null || timezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            log.warn("Mui gio '{}' cua chuyen #{} khong hop le, dung UTC", timezone, trip.getId());
            return ZoneOffset.UTC;
        }
    }

    public LocalDate today(TripEntity trip) {
        return LocalDate.now(clock.withZone(zoneOf(trip)));
    }

    /** Giai đoạn suy từ ngày, bỏ qua giá trị đặt tay. */
    public TripPhase autoPhase(TripEntity trip) {
        LocalDate today = today(trip);
        if (today.isBefore(trip.getStartDate())) {
            return TripPhase.BEFORE;
        }
        if (today.isAfter(trip.getEndDate())) {
            return TripPhase.AFTER;
        }
        return TripPhase.DURING;
    }

    /** Giai đoạn hiệu lực: đặt tay nếu có, ngược lại suy từ ngày (FR-1001, FR-1002). */
    public TripPhase phase(TripEntity trip) {
        return trip.getPhaseOverride() != null ? trip.getPhaseOverride() : autoPhase(trip);
    }

    /**
     * Ngày dùng cho các phép tính "trong lúc đi". Khi người dùng đặt tay giai đoạn DURING mà
     * ngày thật nằm ngoài chuyến, kẹp vào khoảng [ngày đầu, ngày cuối] để mô phỏng.
     */
    public LocalDate effectiveToday(TripEntity trip) {
        LocalDate today = today(trip);
        if (phase(trip) != TripPhase.DURING) {
            return today;
        }
        if (today.isBefore(trip.getStartDate())) {
            return trip.getStartDate();
        }
        if (today.isAfter(trip.getEndDate())) {
            return trip.getEndDate();
        }
        return today;
    }

    /** {@code true} khi "hôm nay" đang được mô phỏng do đặt tay giai đoạn. */
    public boolean isSimulated(TripEntity trip) {
        return !effectiveToday(trip).equals(today(trip));
    }
}
