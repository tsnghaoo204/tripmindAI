package com.tripmind.domains.requests;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Điểm đến do client gửi lên sau khi người dùng chọn một kết quả Google Places hoặc tự nhập.
 *
 * <p>Dữ liệu này KHÔNG được tin: {@code provider = GOOGLE} chỉ được ghi nhận sau khi máy chủ tự
 * gọi Place Details với {@code externalId}; còn lại lưu thành điểm đến MANUAL. Không bao giờ dùng
 * để sửa một dòng đã có trong {@code destinations}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationInput {

    /** MANUAL khi người dùng tự nhập, GOOGLE/MAPBOX khi chọn từ nhà cung cấp. */
    @Builder.Default
    @Size(max = 16)
    private String provider = "GOOGLE";

    /** Mã của nhà cung cấp (Google {@code place_id}). Bỏ trống với điểm đến tự nhập. */
    @Size(max = ValidationLimits.EXTERNAL_ID_MAX)
    private String externalId;

    @NotBlank(message = "Destination name is required")
    @Size(max = 160, message = "Destination name cannot exceed 160 characters")
    private String name;

    @NotBlank(message = "Destination country is required")
    @Size(max = 80, message = "Destination country cannot exceed 80 characters")
    private String country;

    /** Bỏ trống thì tự tra theo tên. */
    @DecimalMin(value = "-90", message = "latitude must be between -90 and 90")
    @DecimalMax(value = "90", message = "latitude must be between -90 and 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180", message = "longitude must be between -180 and 180")
    @DecimalMax(value = "180", message = "longitude must be between -180 and 180")
    private BigDecimal longitude;

    @Size(max = 64)
    private String timezone;

    @Size(max = 500)
    private String formattedAddress;
}
