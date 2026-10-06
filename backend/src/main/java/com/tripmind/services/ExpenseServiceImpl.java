package com.tripmind.services;

import com.tripmind.domains.requests.ExpenseRequest;
import com.tripmind.domains.responses.ExpenseResponse;
import com.tripmind.entities.ActivityEntity;
import com.tripmind.entities.ExpenseEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.ExpenseCategory;
import com.tripmind.enums.ExpenseSource;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.ActivityRepository;
import com.tripmind.repositories.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ActivityRepository activityRepository;
    private final TripService tripService;

    @Override
    @Transactional
    public ExpenseResponse create(Long userId, Long tripId, ExpenseRequest request) {
        TripEntity trip = tripService.getOwnedTrip(userId, tripId);
        ExpenseEntity expense = ExpenseEntity.builder()
                .trip(trip)
                .source(ExpenseSource.FORM)
                .build();
        apply(expense, trip, userId, request);
        ExpenseEntity saved = expenseRepository.save(expense);
        log.info("Recorded expense ID={} {} {} for trip ID={}", saved.getId(), saved.getAmount(), saved.getCurrency(), tripId);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(Long userId, Long tripId, ExpenseCategory category, LocalDate from, LocalDate to) {
        tripService.getOwnedTrip(userId, tripId);
        return expenseRepository.findByTripIdOrderByExpenseDateDescCreatedAtDesc(tripId).stream()
                .filter(e -> category == null || e.getCategory() == category)
                .filter(e -> from == null || !e.getExpenseDate().isBefore(from))
                .filter(e -> to == null || !e.getExpenseDate().isAfter(to))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ExpenseResponse update(Long userId, Long expenseId, ExpenseRequest request) {
        ExpenseEntity expense = owned(userId, expenseId);
        apply(expense, expense.getTrip(), userId, request);
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public void delete(Long userId, Long expenseId) {
        expenseRepository.delete(owned(userId, expenseId));
        log.info("Deleted expense ID={} for user ID={}", expenseId, userId);
    }

    private void apply(ExpenseEntity expense, TripEntity trip, Long userId, ExpenseRequest request) {
        String currency = request.getCurrency() == null ? trip.getCurrency() : request.getCurrency();
        if (!currency.equals(trip.getCurrency())) {
            throw new AppException(ErrorCode.CURRENCY_MISMATCH,
                    "Expense currency " + currency + " differs from trip currency " + trip.getCurrency()
                            + "; convert the amount before recording it",
                    Map.of("tripCurrency", trip.getCurrency()));
        }
        ActivityEntity activity = null;
        if (request.getActivityId() != null) {
            activity = activityRepository.findByIdAndUserId(request.getActivityId(), userId)
                    .filter(a -> a.getItineraryDay().getTrip().getId().equals(trip.getId()))
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Activity " + request.getActivityId() + " not found in this trip"));
        }
        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount());
        expense.setCurrency(currency);
        expense.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setActivity(activity);
    }

    private ExpenseEntity owned(Long userId, Long expenseId) {
        return expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Expense not found or access denied: " + expenseId));
    }

    private ExpenseResponse toResponse(ExpenseEntity e) {
        return ExpenseResponse.builder()
                .id(e.getId())
                .tripId(e.getTrip().getId())
                .activityId(e.getActivity() == null ? null : e.getActivity().getId())
                .activityTitle(e.getActivity() == null ? null : e.getActivity().getTitle())
                .category(e.getCategory())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .description(e.getDescription())
                .expenseDate(e.getExpenseDate())
                .source(e.getSource())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
