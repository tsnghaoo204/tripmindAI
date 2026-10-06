package com.tripmind.services;

import com.tripmind.configurations.properties.CostEstimateProperties;
import com.tripmind.configurations.properties.CostEstimateProperties.CurrencyTable;
import com.tripmind.configurations.properties.CostEstimateProperties.PriceRange;
import com.tripmind.domains.responses.CostEstimateResponse;
import com.tripmind.enums.ActivityType;
import com.tripmind.enums.CostSource;
import com.tripmind.utils.MoneyFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Gợi ý chi phí ước tính từ mức giá Google ({@code price_level} 0–4) × số người.
 *
 * <p>Ba nguyên tắc: không có mức giá thì <b>không đoán</b>; lưu trú và di chuyển không gợi ý
 * (mức giá của Google không phản ánh giá phòng hay giá vé); con số luôn đi kèm câu giải thích
 * {@code basis} để người dùng biết nó từ đâu ra và tự sửa.
 */
@Service
@RequiredArgsConstructor
public class CostEstimator {

    public static final String NO_PRICE_DATA = "NO_PRICE_DATA";
    public static final String UNSUPPORTED_CURRENCY = "UNSUPPORTED_CURRENCY";
    public static final String UNSUPPORTED_ACTIVITY_TYPE = "UNSUPPORTED_ACTIVITY_TYPE";

    private static final Set<ActivityType> NOT_PRICED = Set.of(ActivityType.ACCOMMODATION, ActivityType.TRANSPORT);

    private final CostEstimateProperties properties;

    public CostEstimateResponse estimate(Integer priceLevel, ActivityType activityType, int travelers, String currency) {
        CostEstimateResponse.CostEstimateResponseBuilder base = CostEstimateResponse.builder()
                .currency(currency)
                .travelers(travelers)
                .priceLevel(priceLevel);

        if (activityType != null && NOT_PRICED.contains(activityType)) {
            return base.reason(UNSUPPORTED_ACTIVITY_TYPE).build();
        }
        if (priceLevel == null || priceLevel < 0 || priceLevel > 4) {
            return base.reason(NO_PRICE_DATA).build();
        }
        CurrencyTable table = properties.currencies().get(currency == null ? null : currency.toUpperCase());
        if (table == null) {
            return base.reason(UNSUPPORTED_CURRENCY).build();
        }

        PriceRange perPerson = (activityType == ActivityType.FOOD ? table.food() : table.other()).get(priceLevel);
        int people = Math.max(1, travelers);
        long min = perPerson.min() * people;
        long max = perPerson.max() * people;
        long suggested = roundDown((perPerson.min() + perPerson.max()) / 2 * people, table.rounding());

        return base
                .suggested(suggested)
                .min(min)
                .max(max)
                .source(CostSource.PRICE_LEVEL)
                .basis(basis(priceLevel, perPerson, people, currency))
                .build();
    }

    /** Chỉ lấy con số; {@code null} khi không gợi ý được. */
    public Long suggest(Integer priceLevel, ActivityType activityType, int travelers, String currency) {
        return estimate(priceLevel, activityType, travelers, currency).getSuggested();
    }

    private String basis(int priceLevel, PriceRange perPerson, int people, String currency) {
        String level = priceLevel == 0 ? "Miễn phí" : "$".repeat(priceLevel);
        String range = perPerson.min() == perPerson.max()
                ? MoneyFormat.compact(perPerson.min(), currency)
                : MoneyFormat.compact(perPerson.min(), currency) + "–" + MoneyFormat.compact(perPerson.max(), currency);
        return "Mức giá " + level + " trên Google · " + range + "/người × " + people + " người";
    }

    private long roundDown(long amount, long rounding) {
        return rounding <= 1 ? amount : (amount / rounding) * rounding;
    }
}
