package com.tripmind.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Nguồn số liệu thời tiết. Không lưu CSDL nên không có ràng buộc CHECK. */
@Getter
@RequiredArgsConstructor
public enum WeatherSource {
    FORECAST("dự báo"),
    CLIMATE_NORMAL("theo trung bình khí hậu"),
    OBSERVED("đã ghi nhận");

    private final String label;
}
