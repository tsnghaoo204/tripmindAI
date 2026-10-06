package com.tripmind.services;

import com.tripmind.configurations.properties.BudgetProperties;
import com.tripmind.domains.responses.BudgetSummaryResponse;
import com.tripmind.domains.responses.BudgetSummaryResponse.CategoryAmount;
import com.tripmind.domains.responses.BudgetSummaryResponse.DailyAllowance;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.ActivityType;
import com.tripmind.enums.ExpenseCategory;
import com.tripmind.enums.TripPhase;
import com.tripmind.repositories.ActivityRepository;
import com.tripmind.repositories.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    public static final String NO_BUDGET = "NO_BUDGET";
    public static final String TRIP_ENDED = "TRIP_ENDED";

    private final TripService tripService;
    private final ActivityRepository activityRepository;
    private final ExpenseRepository expenseRepository;
    private final TripClock tripClock;
    private final BudgetProperties properties;

    @Override
    @Transactional(readOnly = true)
    public BudgetSummaryResponse getTripBudgetSummary(Long userId, Long tripId) {
        return summarize(tripService.getOwnedTrip(userId, tripId));
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetSummaryResponse summarize(TripEntity trip) {
        Map<ExpenseCategory, CategoryAmount> byCategory = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory category : ExpenseCategory.values()) {
            byCategory.put(category, new CategoryAmount(0L, 0L));
        }

        long estimatedTotal = 0;
        for (Object[] row : activityRepository.sumEstimatedByType(trip.getId())) {
            long amount = ((Number) row[1]).longValue();
            CategoryAmount bucket = byCategory.get(categoryOf((ActivityType) row[0]));
            bucket.setEstimated(bucket.getEstimated() + amount);
            estimatedTotal += amount;
        }
        long actualTotal = 0;
        for (Object[] row : expenseRepository.sumByCategory(trip.getId())) {
            long amount = ((Number) row[1]).longValue();
            byCategory.get((ExpenseCategory) row[0]).setActual(amount);
            actualTotal += amount;
        }

        Long budget = trip.getBudget();
        long committed = Math.max(estimatedTotal, actualTotal);
        BudgetSummaryResponse.BudgetSummaryResponseBuilder response = BudgetSummaryResponse.builder()
                .tripId(trip.getId())
                .budget(budget)
                .currency(trip.getCurrency())
                .estimatedTotal(estimatedTotal)
                .actualTotal(actualTotal)
                .remaining(budget == null ? null : budget - committed)
                .usedRatio(budget == null || budget == 0 ? null : (double) committed / budget)
                .warningLevel(warningLevel(budget, committed))
                .byCategory(byCategory);

        if (budget == null) {
            return response.dailyReason(NO_BUDGET).build();
        }
        TripPhase phase = tripClock.phase(trip);
        if (phase == TripPhase.AFTER) {
            return response.dailyReason(TRIP_ENDED).build();
        }
        return response.daily(phase == TripPhase.BEFORE ? planAllowance(trip, budget, actualTotal) : todayAllowance(trip, budget))
                .build();
    }

    /**
     * BR-403. Ước tính và thực chi tách rời; cảnh báo dựa trên phần lớn hơn của hai — đã lên
     * kế hoạch tiêu nhiều thì cảnh báo ngay cả khi chưa tiêu đồng nào.
     */
    private String warningLevel(Long budget, long committed) {
        if (budget == null) {
            return "NONE";
        }
        if (committed > budget) {
            return "OVER";
        }
        if (budget > 0 && committed > budget * properties.nearLimitRatio()) {
            return "NEAR_LIMIT";
        }
        return "NONE";
    }

    /** Chưa đi: bình quân mỗi ngày được tiêu, sau khi trừ các khoản đã chi trước (đặt phòng, vé). */
    private DailyAllowance planAllowance(TripEntity trip, long budget, long spentSoFar) {
        int days = trip.lengthInDays();
        long left = budget - spentSoFar;
        return DailyAllowance.builder()
                .mode("PLAN")
                .date(tripClock.today(trip))
                .simulated(false)
                .remainingDays(days)
                .allowance(left <= 0 ? 0 : roundDown(left / days, trip.getCurrency()))
                .status(left < 0 ? "OVER" : "ON_TRACK")
                .overBy(left < 0 ? -left : null)
                .build();
    }

    /**
     * Đang đi. {@code allowance = (ngân sách − đã chi trước hôm nay) ÷ số ngày còn lại tính cả
     * hôm nay}. Lấy phần chi <i>trước hôm nay</i> để hạn mức đứng yên suốt ngày: ghi thêm một
     * khoản thì {@code todayLeft} giảm, còn {@code allowance} không co lại theo.
     */
    private DailyAllowance todayAllowance(TripEntity trip, long budget) {
        LocalDate today = tripClock.effectiveToday(trip);
        long spentBefore = expenseRepository.sumBefore(trip.getId(), today);
        long spentToday = expenseRepository.sumOn(trip.getId(), today);
        int remainingDays = (int) ChronoUnit.DAYS.between(today, trip.getEndDate()) + 1;
        long left = budget - spentBefore;
        long allowance = left <= 0 ? 0 : roundDown(left / remainingDays, trip.getCurrency());

        int daysElapsed = (int) ChronoUnit.DAYS.between(trip.getStartDate(), today);
        Long average = daysElapsed <= 0 ? null
                : expenseRepository.sumBetween(trip.getId(), trip.getStartDate(), today) / daysElapsed;
        long overBy = spentBefore + spentToday - budget;

        return DailyAllowance.builder()
                .mode("TODAY")
                .date(today)
                .simulated(tripClock.isSimulated(trip))
                .dayNumber(daysElapsed + 1)
                .remainingDays(remainingDays)
                .allowance(allowance)
                .spentToday(spentToday)
                .todayLeft(allowance - spentToday)
                .status(dailyStatus(allowance, spentToday, left))
                .averageSpentPerDay(average)
                .overBy(overBy > 0 ? overBy : null)
                .build();
    }

    private String dailyStatus(long allowance, long spentToday, long left) {
        if (left <= 0 || spentToday > allowance) {
            return "OVER";
        }
        if (spentToday > allowance * properties.nearLimitRatio()) {
            return "NEAR";
        }
        return "ON_TRACK";
    }

    private long roundDown(long amount, String currency) {
        long rounding = properties.roundingFor(currency);
        return (amount / rounding) * rounding;
    }

    /** Loại hoạt động → hạng mục chi tiêu, để so ước tính với thực chi theo cùng một trục. */
    static ExpenseCategory categoryOf(ActivityType type) {
        return switch (type) {
            case SIGHTSEEING -> ExpenseCategory.ACTIVITIES;
            case FOOD -> ExpenseCategory.FOOD;
            case TRANSPORT -> ExpenseCategory.TRANSPORTATION;
            case ACCOMMODATION -> ExpenseCategory.ACCOMMODATION;
            case REST, OTHER -> ExpenseCategory.OTHER;
        };
    }
}
