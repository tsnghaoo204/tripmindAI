package com.tripmind.utils;

/** Mã thời tiết WMO (Open-Meteo) → mô tả tiếng Việt ngắn. */
public final class WeatherCodes {

    private WeatherCodes() {
    }

    public static String describe(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 0 -> "Trời quang";
            case 1, 2 -> "Ít mây";
            case 3 -> "Nhiều mây";
            case 45, 48 -> "Sương mù";
            case 51, 53, 55, 56, 57 -> "Mưa phùn";
            case 61, 63, 66, 80, 81 -> "Mưa";
            case 65, 67, 82 -> "Mưa to";
            case 71, 73, 75, 77, 85, 86 -> "Tuyết";
            case 95, 96, 99 -> "Dông";
            default -> "Không rõ";
        };
    }
}
