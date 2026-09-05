package com.tripmind.domains.responses;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Một ứng viên điểm đến. Có thể là dòng đã có trong bảng {@code destinations}
 * ({@code id} khác null, {@code saved = true}), hoặc một kết quả Google Places chưa được
 * nạp ({@code id} null) — chọn nó thì máy chủ mới ghi xuống cơ sở dữ liệu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationResponse {

    private Long id;
    private String provider;
    private String externalId;
    private String name;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String timezone;
    private String formattedAddress;
    private JsonNode metadata;
    private boolean saved;
}
