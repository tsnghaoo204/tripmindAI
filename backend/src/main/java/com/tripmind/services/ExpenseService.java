package com.tripmind.services;

import com.tripmind.domains.requests.ExpenseRequest;
import com.tripmind.domains.responses.ExpenseResponse;
import com.tripmind.enums.ExpenseCategory;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseService {

    ExpenseResponse create(Long userId, Long tripId, ExpenseRequest request);

    List<ExpenseResponse> list(Long userId, Long tripId, ExpenseCategory category, LocalDate from, LocalDate to);

    ExpenseResponse update(Long userId, Long expenseId, ExpenseRequest request);

    void delete(Long userId, Long expenseId);
}
