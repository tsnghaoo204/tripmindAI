package com.tripmind.services;

import com.tripmind.configurations.properties.CostEstimateProperties;
import com.tripmind.configurations.properties.CostEstimateProperties.CurrencyTable;
import com.tripmind.configurations.properties.CostEstimateProperties.PriceRange;
import com.tripmind.domains.responses.CostEstimateResponse;
import com.tripmind.enums.ActivityType;
import com.tripmind.enums.CostSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CostEstimatorTest {

    private static List<PriceRange> ranges(long... bounds) {
        return List.of(
                new PriceRange(bounds[0], bounds[1]), new PriceRange(bounds[2], bounds[3]),
                new PriceRange(bounds[4], bounds[5]), new PriceRange(bounds[6], bounds[7]),
                new PriceRange(bounds[8], bounds[9]));
    }

    private final CostEstimator estimator = new CostEstimator(new CostEstimateProperties(Map.of(
            "VND", new CurrencyTable(1000,
                    ranges(0, 0, 30_000, 100_000, 100_000, 300_000, 300_000, 700_000, 700_000, 2_000_000),
                    ranges(0, 0, 0, 100_000, 100_000, 300_000, 300_000, 800_000, 800_000, 3_000_000)))));

    @Test
    @DisplayName("Quán mức giá $$ cho 2 người: 200k–600k, gợi ý 400k, có câu giải thích")
    void moderateFoodForTwo() {
        CostEstimateResponse r = estimator.estimate(2, ActivityType.FOOD, 2, "VND");

        assertThat(r.getSuggested()).isEqualTo(400_000L);
        assertThat(r.getMin()).isEqualTo(200_000L);
        assertThat(r.getMax()).isEqualTo(600_000L);
        assertThat(r.getSource()).isEqualTo(CostSource.PRICE_LEVEL);
        assertThat(r.getBasis()).isEqualTo("Mức giá $$ trên Google · 100k–300k/người × 2 người");
    }

    @Test
    @DisplayName("Làm tròn xuống tới nghìn đồng")
    void roundsDownToThousand() {
        // giữa khoảng 30k–100k = 65k; × 3 = 195k
        assertThat(estimator.suggest(1, ActivityType.FOOD, 3, "VND")).isEqualTo(195_000L);
    }

    @Test
    @DisplayName("Không có mức giá thì không đoán")
    void noPriceLevel() {
        CostEstimateResponse r = estimator.estimate(null, ActivityType.FOOD, 2, "VND");
        assertThat(r.getSuggested()).isNull();
        assertThat(r.getReason()).isEqualTo(CostEstimator.NO_PRICE_DATA);
    }

    @Test
    @DisplayName("Tiền chưa có bảng giá và loại lưu trú/di chuyển thì không gợi ý")
    void unsupportedCases() {
        assertThat(estimator.estimate(2, ActivityType.FOOD, 2, "JPY").getReason())
                .isEqualTo(CostEstimator.UNSUPPORTED_CURRENCY);
        assertThat(estimator.estimate(3, ActivityType.ACCOMMODATION, 2, "VND").getReason())
                .isEqualTo(CostEstimator.UNSUPPORTED_ACTIVITY_TYPE);
    }
}
