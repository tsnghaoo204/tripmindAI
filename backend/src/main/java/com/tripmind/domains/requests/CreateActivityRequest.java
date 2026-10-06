package com.tripmind.domains.requests;

import com.tripmind.enums.ActivityType;
import com.tripmind.enums.CostSource;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Thêm một hoạt động vào một ngày ({@code dayId} hoặc {@code dayNumber}).
 *
 * <p>Địa điểm không bắt buộc (DI-4). Gắn địa điểm theo một trong hai cách: {@code placeId}
 * của một dòng đã có trong CSDL, hoặc {@code placeProvider} + {@code placeExternalId} của
 * một kết quả tìm kiếm Google chưa được lưu — máy chủ nạp nó với {@code adopted_via = ITINERARY}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateActivityRequest {

    private Long dayId;

    @Min(value = 1, message = "Day number must be at least 1")
    private Integer dayNumber;

    @NotBlank(message = "Activity title is required")
    @Size(max = 200, message = "Activity title cannot exceed 200 characters")
    private String title;

    private Long placeId;

    private String placeProvider;

    private String placeExternalId;

    @NotNull(message = "Activity type is required")
    private ActivityType activityType;

    private LocalTime startTime;

    private LocalTime endTime;

    /** Bỏ trống = chưa biết chi phí (khác với 0 = miễn phí). */
    @Min(value = 0, message = "Estimated cost must not be negative")
    private Long estimatedCost;

    /** {@code PRICE_LEVEL} khi người dùng giữ nguyên con số gợi ý; mặc định {@code USER}. */
    private CostSource estimatedCostSource;

    @Size(max = 16)
    private String transportationMode;

    private String notes;
}
