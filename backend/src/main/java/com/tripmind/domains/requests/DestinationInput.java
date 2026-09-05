package com.tripmind.domains.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Điểm đến do client gửi lên nguyên vẹn sau khi người dùng chọn một kết quả Google Places.
 *
 * <p>Dùng khi không muốn máy chủ gọi lại Place Details: client đã có sẵn toạ độ và tên
 * từ lượt tìm kiếm. {@code timezone} không bắt buộc — thiếu thì máy chủ hỏi Time Zone API
 * theo toạ độ, vì {@code destinations.timezone} là {@code NOT NULL}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationInput {

    /** MANUAL khi người dùng tự nhập, GOOGLE/MAPBOX khi chọn từ nhà cung cấp. */
    @Builder.Default
    private String provider = "GOOGLE";

    /** Mã của nhà cung cấp (Google {@code place_id}). Bỏ trống với điểm đến tự nhập. */
    private String externalId;

    @NotBlank(message = "Destination name is required")
    private String name;

    @NotBlank(message = "Destination country is required")
    private String country;

    @NotNull(message = "Destination latitude is required")
    private BigDecimal latitude;

    @NotNull(message = "Destination longitude is required")
    private BigDecimal longitude;

    private String timezone;

    private String formattedAddress;
}
