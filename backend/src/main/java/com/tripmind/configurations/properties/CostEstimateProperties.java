package com.tripmind.configurations.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

/**
 * Bảng giá theo người cho từng mức {@code price_level} của Google (0–4), theo từng loại tiền.
 * Cấu hình ở {@code tripmind.cost-estimate.*} để chỉnh giá không cần build lại.
 *
 * @param currencies mã tiền ISO-4217 → bảng giá; tiền không có bảng thì không gợi ý
 */
@Validated
@ConfigurationProperties(prefix = "tripmind.cost-estimate")
public record CostEstimateProperties(@NotNull Map<String, @Valid CurrencyTable> currencies) {

    /**
     * @param rounding làm tròn xuống tới bội số này (VND: 1000)
     * @param food     5 khoảng giá cho hoạt động ăn uống, chỉ số = {@code price_level}
     * @param other    5 khoảng giá cho tham quan, giải trí và loại khác
     */
    public record CurrencyTable(
            @Min(1) long rounding,
            @NotNull @Size(min = 5, max = 5) List<@Valid PriceRange> food,
            @NotNull @Size(min = 5, max = 5) List<@Valid PriceRange> other) {
    }

    public record PriceRange(@Min(0) long min, @Min(0) long max) {
    }
}
