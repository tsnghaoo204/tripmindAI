package com.tripmind.domains.requests;

import com.tripmind.domains.models.GroupProfile;
import com.tripmind.enums.BudgetPreference;
import com.tripmind.enums.TravelStyle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Sửa chuyến đi. Trường nào null thì giữ nguyên.
 *
 * <p>Đổi ngày: gửi cả hai ngày thì dùng đúng hai ngày đó; chỉ gửi {@code startDate} thì
 * <b>dời cả chuyến</b> và giữ nguyên số ngày; chỉ gửi {@code endDate} thì kéo dài hoặc rút
 * ngắn. Rút ngắn mà ngày bị bỏ còn hoạt động thì cần {@code confirmDropDays = true}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTripRequest {

    @Size(max = 160, message = "Trip name cannot exceed 160 characters")
    private String name;

    @Min(value = 1, message = "Travelers must be at least 1")
    private Integer travelers;

    @Min(value = 0, message = "Budget must not be negative")
    private Long budget;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be an ISO-4217 code, e.g. VND")
    private String currency;

    private LocalDate startDate;

    private LocalDate endDate;

    /** Đồng ý xoá các ngày cuối còn hoạt động khi rút ngắn chuyến. */
    private boolean confirmDropDays;

    private TravelStyle travelStyle;

    private BudgetPreference budgetPreference;

    private List<@Size(max = 40) String> preferences;

    @Valid
    private GroupProfile groupProfile;
}
