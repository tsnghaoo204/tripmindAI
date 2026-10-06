package com.tripmind.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Ngưỡng của bộ luật gợi ý checklist, cấu hình ở {@code tripmind.checklist.*}.
 *
 * @param rainProbability   ngày có khả năng mưa từ mức này (%) thì gợi ý áo mưa
 * @param rainMm            trung bình khí hậu từ mức này (mm/ngày) thì cũng tính là ngày mưa
 * @param hotTempMax        nhiệt độ cao nhất từ mức này (°C) thì gợi ý chống nắng
 * @param coldTempMin       nhiệt độ thấp nhất tới mức này (°C) thì gợi ý áo ấm
 * @param longTripDays      chuyến từ chừng này ngày thì gợi ý đồ cho chuyến dài
 * @param homeCountries     mã hoặc tên quốc gia được coi là "trong nước"
 */
@Validated
@ConfigurationProperties(prefix = "tripmind.checklist")
public record ChecklistProperties(
        @DefaultValue("50") int rainProbability,
        @DefaultValue("5") double rainMm,
        @DefaultValue("32") double hotTempMax,
        @DefaultValue("15") double coldTempMin,
        @DefaultValue("4") int longTripDays,
        @DefaultValue({"VN", "Vietnam", "Việt Nam", "Viet Nam"}) List<String> homeCountries) {
}
