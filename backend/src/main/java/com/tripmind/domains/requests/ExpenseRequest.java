package com.tripmind.domains.requests;

import com.tripmind.enums.ExpenseCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Ghi hoặc sửa một khoản chi thực tế. Ngày chi được phép trước ngày đi (đặt phòng, mua vé
 * từ sớm). {@code currency} bỏ trống thì lấy tiền của chuyến; khác tiền của chuyến thì bị từ
 * chối vì hệ thống không quy đổi tỷ giá (BR-401).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseRequest {

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    @NotNull(message = "Amount is required")
    @Min(value = 0, message = "Amount must not be negative")
    private Long amount;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be an ISO-4217 code, e.g. VND")
    private String currency;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    private Long activityId;
}
