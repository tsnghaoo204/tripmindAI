package com.tripmind.services;

import com.tripmind.configurations.properties.BudgetProperties;
import com.tripmind.domains.responses.BudgetSummaryResponse;
import com.tripmind.entities.DestinationEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.ActivityType;
import com.tripmind.enums.ExpenseCategory;
import com.tripmind.repositories.ActivityRepository;
import com.tripmind.repositories.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BudgetServiceImplTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 20);

    private final ActivityRepository activityRepository = mock(ActivityRepository.class);
    private final ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
    private TripEntity trip;

    @BeforeEach
    void setUp() {
        trip = TripEntity.builder()
                .id(7L)
                .destination(DestinationEntity.builder().timezone("Asia/Ho_Chi_Minh").build())
                .startDate(START)
                .endDate(START.plusDays(3))
                .budget(8_000_000L)
                .currency("VND")
                .build();
        when(activityRepository.sumEstimatedByType(7L)).thenReturn(List.of());
        when(expenseRepository.sumByCategory(7L)).thenReturn(List.of());
    }

    /** Đồng hồ cố định lúc 10:00 sáng giờ Việt Nam của ngày {@code date}. */
    private BudgetServiceImpl serviceOn(LocalDate date) {
        Clock clock = Clock.fixed(date.atTime(3, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new BudgetServiceImpl(null, activityRepository, expenseRepository, new TripClock(clock),
                new BudgetProperties(0.9, Map.of("VND", 1000L)));
    }

    @ParameterizedTest(name = "đã dùng {0} → {1}")
    @CsvSource({"7120000, NONE", "7280000, NEAR_LIMIT", "8080000, OVER"})
    @DisplayName("Ba mốc cảnh báo 89% / 91% / 101% (BR-403)")
    void warningThresholds(long estimated, String expected) {
        List<Object[]> rows = List.<Object[]>of(new Object[]{ActivityType.FOOD, estimated});
        when(activityRepository.sumEstimatedByType(7L)).thenReturn(rows);

        BudgetSummaryResponse r = serviceOn(START.minusDays(5)).summarize(trip);

        assertThat(r.getWarningLevel()).isEqualTo(expected);
        assertThat(r.getByCategory().get(ExpenseCategory.FOOD).getEstimated()).isEqualTo(estimated);
    }

    @Test
    @DisplayName("remaining = ngân sách − phần lớn hơn giữa ước tính và thực chi")
    void remainingUsesLargerOfEstimateAndActual() {
        List<Object[]> estimated = List.<Object[]>of(new Object[]{ActivityType.SIGHTSEEING, 7_350_000L});
        List<Object[]> actual = List.<Object[]>of(new Object[]{ExpenseCategory.FOOD, 2_100_000L});
        when(activityRepository.sumEstimatedByType(7L)).thenReturn(estimated);
        when(expenseRepository.sumByCategory(7L)).thenReturn(actual);

        BudgetSummaryResponse r = serviceOn(START.minusDays(5)).summarize(trip);

        assertThat(r.getRemaining()).isEqualTo(650_000L);
        assertThat(r.getWarningLevel()).isEqualTo("NEAR_LIMIT");
        assertThat(r.getByCategory().get(ExpenseCategory.ACTIVITIES).getEstimated()).isEqualTo(7_350_000L);
    }

    @Test
    @DisplayName("Ngày 2/4: trước hôm nay chi 2tr, hôm nay 500k → hạn mức 2tr, còn 1,5tr")
    void dailyAllowanceDuringTrip() {
        LocalDate day2 = START.plusDays(1);
        when(expenseRepository.sumBefore(7L, day2)).thenReturn(2_000_000L);
        when(expenseRepository.sumOn(7L, day2)).thenReturn(500_000L);
        when(expenseRepository.sumBetween(eq(7L), eq(START), eq(day2))).thenReturn(1_800_000L);

        BudgetSummaryResponse.DailyAllowance d = serviceOn(day2).summarize(trip).getDaily();

        assertThat(d.getMode()).isEqualTo("TODAY");
        assertThat(d.getDayNumber()).isEqualTo(2);
        assertThat(d.getRemainingDays()).isEqualTo(3);
        assertThat(d.getAllowance()).isEqualTo(2_000_000L);
        assertThat(d.getTodayLeft()).isEqualTo(1_500_000L);
        assertThat(d.getStatus()).isEqualTo("ON_TRACK");
        assertThat(d.getAverageSpentPerDay()).isEqualTo(1_800_000L);
    }

    @Test
    @DisplayName("Đã tiêu lố ngân sách: hạn mức 0, trạng thái OVER, báo lố bao nhiêu")
    void dailyOverBudget() {
        LocalDate day3 = START.plusDays(2);
        when(expenseRepository.sumBefore(7L, day3)).thenReturn(8_500_000L);
        when(expenseRepository.sumOn(7L, day3)).thenReturn(100_000L);
        when(expenseRepository.sumBetween(any(), any(), any())).thenReturn(8_500_000L);

        BudgetSummaryResponse.DailyAllowance d = serviceOn(day3).summarize(trip).getDaily();

        assertThat(d.getAllowance()).isZero();
        assertThat(d.getStatus()).isEqualTo("OVER");
        assertThat(d.getOverBy()).isEqualTo(600_000L);
    }

    @Test
    @DisplayName("Chưa đi: bình quân mỗi ngày sau khi trừ khoản đã đặt trước, làm tròn nghìn đồng")
    void planModeBeforeTrip() {
        List<Object[]> actual = List.<Object[]>of(new Object[]{ExpenseCategory.ACCOMMODATION, 3_000_000L});
        when(expenseRepository.sumByCategory(7L)).thenReturn(actual);

        BudgetSummaryResponse.DailyAllowance d = serviceOn(START.minusDays(3)).summarize(trip).getDaily();

        assertThat(d.getMode()).isEqualTo("PLAN");
        assertThat(d.getAllowance()).isEqualTo(1_250_000L);
    }

    @Test
    @DisplayName("Sau chuyến và khi chưa đặt ngân sách thì không có hạn mức, kèm lý do")
    void noDailyAllowance() {
        assertThat(serviceOn(START.plusDays(10)).summarize(trip).getDailyReason()).isEqualTo(BudgetServiceImpl.TRIP_ENDED);
        trip.setBudget(null);
        BudgetSummaryResponse r = serviceOn(START).summarize(trip);
        assertThat(r.getDaily()).isNull();
        assertThat(r.getDailyReason()).isEqualTo(BudgetServiceImpl.NO_BUDGET);
        assertThat(r.getWarningLevel()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("23:30 giờ Việt Nam đã là ngày mới dù UTC vẫn là hôm trước")
    void todayFollowsDestinationTimezone() {
        LocalDate day2 = START.plusDays(1);
        // 16:30Z ngày 1 = 23:30 ngày 1 ở VN → vẫn ngày 1; 17:30Z = 00:30 ngày 2 ở VN.
        Clock lateNight = Clock.fixed(Instant.parse("2026-10-20T17:30:00Z"), ZoneOffset.UTC);
        BudgetServiceImpl service = new BudgetServiceImpl(null, activityRepository, expenseRepository,
                new TripClock(lateNight), new BudgetProperties(0.9, Map.of("VND", 1000L)));
        when(expenseRepository.sumBefore(7L, day2)).thenReturn(0L);

        assertThat(service.summarize(trip).getDaily().getDate()).isEqualTo(day2);
    }
}
