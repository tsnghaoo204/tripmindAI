package com.tripmind.configurations.properties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * Ngưỡng cảnh báo ngân sách (BR-403) và cách làm tròn hạn mức chi tiêu mỗi ngày.
 *
 * @param nearLimitRatio vượt tỉ lệ này thì cảnh báo NEAR_LIMIT
 * @param dailyRounding  mã tiền → làm tròn xuống tới bội số này; tiền không có trong bảng thì không làm tròn
 */
@Validated
@ConfigurationProperties(prefix = "tripmind.budget")
public record BudgetProperties(
        @DefaultValue("0.9") @DecimalMin("0.1") @DecimalMax("1.0") double nearLimitRatio,
        Map<String, Long> dailyRounding) {

    public long roundingFor(String currency) {
        if (dailyRounding == null || currency == null) {
            return 1;
        }
        return Math.max(1, dailyRounding.getOrDefault(currency.toUpperCase(), 1L));
    }
}
