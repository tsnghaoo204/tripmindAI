package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripmind.enums.WeatherSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Thời tiết từng ngày của chuyến. Mỗi ngày luôn có {@code source} để giao diện phân biệt dự
 * báo với trung bình khí hậu (FR-503). Dịch vụ lỗi thì {@code days} rỗng kèm {@code reason}
 * chứ không ném lỗi (QĐ-11).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripWeatherResponse {

    private Long tripId;
    private String timezone;
    private List<DayWeather> days;
    /** WEATHER_UNAVAILABLE (không có ngày nào) · PARTIAL (thiếu một số ngày). */
    private String reason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DayWeather {
        private LocalDate date;
        private Integer dayNumber;
        private WeatherSource source;
        private Double tempMin;
        private Double tempMax;
        /**
         * Dự báo: khả năng mưa của mô hình. Trung bình khí hậu: tỉ lệ số năm có mưa ≥ 1 mm
         * vào ngày này trong các năm đã lấy mẫu.
         */
        private Integer precipitationProbability;
        private Double precipitationMm;
        private Integer weatherCode;
        private String summary;
    }
}
