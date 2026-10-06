package com.tripmind.configurations.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Luật nghiệp vụ của chuyến đi, cấu hình ở {@code tripmind.trip.*}.
 *
 * @param maxDays                   độ dài tối đa của một chuyến (BR-201)
 * @param plannedDayMinActivities   một ngày có ít nhất bấy nhiêu hoạt động thì tính là "đã lên lịch" (BR-207)
 */
@Validated
@ConfigurationProperties(prefix = "tripmind.trip")
public record TripProperties(
        @DefaultValue("30") @Min(1) int maxDays,
        @DefaultValue("2") @Min(1) int plannedDayMinActivities) {
}
