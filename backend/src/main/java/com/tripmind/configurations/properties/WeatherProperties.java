package com.tripmind.configurations.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Open-Meteo: dự báo cho các ngày gần, trung bình khí hậu cho các ngày xa (QĐ-07).
 *
 * @param forecastBaseUrl  API dự báo, nhận ngày trong khoảng [hôm nay − pastDays, hôm nay + forecastDays − 1]
 * @param archiveBaseUrl   API lưu trữ (tên miền khác), dùng cho trung bình khí hậu và ngày quá xa trong quá khứ
 * @param forecastDays     số ngày dự báo tính cả hôm nay (Open-Meteo: tối đa 16)
 * @param pastDays         số ngày quá khứ API dự báo còn trả số liệu
 * @param climateYears     số năm trước lấy trung bình cùng kỳ
 * @param timeout          hạn chờ mỗi lượt gọi
 * @param cacheTtl         thời gian giữ kết quả trong Redis
 */
@Validated
@ConfigurationProperties(prefix = "tripmind.open-meteo")
public record WeatherProperties(
        @NotBlank String baseUrl,
        @NotBlank String archiveBaseUrl,
        @DefaultValue("16") @Min(1) @Max(16) int forecastDays,
        @DefaultValue("90") @Min(0) @Max(92) int pastDays,
        @DefaultValue("3") @Min(1) @Max(10) int climateYears,
        @DefaultValue("PT5S") Duration timeout,
        @DefaultValue("PT3H") Duration cacheTtl) {
}
