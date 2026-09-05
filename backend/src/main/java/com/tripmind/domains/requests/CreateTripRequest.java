package com.tripmind.domains.requests;

import com.tripmind.enums.BudgetPreference;
import com.tripmind.enums.TravelStyle;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {

    /**
     * Điểm đến đã có trong bảng {@code destinations} — trường hợp người dùng chọn từ danh
     * sách gợi ý. Không còn bắt buộc: hai trường dưới đây phục vụ những nơi chưa từng ai đi.
     */
    private Long destinationId;

    /**
     * Google {@code place_id} khi người dùng chọn một thành phố mới từ kết quả Places.
     * Máy chủ gọi Place Details rồi tự ghi thêm dòng vào {@code destinations}.
     */
    private String destinationPlaceId;

    /**
     * Dữ liệu điểm đến client đã có sẵn từ lượt tìm kiếm — dùng khi không muốn máy chủ
     * gọi lại Place Details, hoặc khi người dùng tự nhập một nơi Google không trả về.
     */
    @Valid
    private DestinationInput destinationData;

    @NotBlank(message = "Trip name is required")
    private String name;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must not be in the past")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Min(value = 1, message = "Travelers must be at least 1")
    @Builder.Default
    private int travelers = 1;

    @Min(value = 0, message = "Budget must not be negative")
    private Long budget;

    @Builder.Default
    private String currency = "VND";

    private TravelStyle travelStyle;

    private BudgetPreference budgetPreference;

    private List<String> preferences;

    /**
     * Đúng một trong ba đường xác định điểm đến là đủ; không có đường nào thì chuyến đi
     * không có đích. Kiểm ở đây thay vì ở service để lỗi trả về đúng dạng 400 kèm tên trường.
     */
    @JsonIgnore
    @AssertTrue(message = "One of destinationId, destinationPlaceId or destinationData is required")
    public boolean isDestinationProvided() {
        return destinationId != null
                || (destinationPlaceId != null && !destinationPlaceId.isBlank())
                || destinationData != null;
    }
}
