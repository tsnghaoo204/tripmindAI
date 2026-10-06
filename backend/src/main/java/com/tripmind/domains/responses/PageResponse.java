package com.tripmind.domains.responses;

import org.springframework.data.domain.Page;

import java.util.List;

/** Phân trang theo ADS-30 §1: {@code { content, page, size, totalElements }}. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
