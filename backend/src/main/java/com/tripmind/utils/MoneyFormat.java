package com.tripmind.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Viết số tiền gọn để hiện cho người đọc: {@code 150000 VND → "150k"}, {@code 1200000 → "1,2tr"}. */
public final class MoneyFormat {

    private static final DecimalFormat ONE_DECIMAL =
            new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN")));

    private MoneyFormat() {
    }

    public static String compact(long amount, String currency) {
        if (!"VND".equalsIgnoreCase(currency)) {
            return amount + " " + currency;
        }
        if (amount >= 1_000_000) {
            synchronized (ONE_DECIMAL) {
                return ONE_DECIMAL.format(amount / 1_000_000.0) + "tr";
            }
        }
        if (amount >= 1_000) {
            return (amount / 1_000) + "k";
        }
        return amount + "đ";
    }
}
