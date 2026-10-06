package com.tripmind.services.ai.tools;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Địa điểm người dùng đã thích / đã chê ở các chuyến trước, để công cụ lọc bằng mã
 * thay vì trông vào việc mô hình tự nhớ. Đánh giá được ghi ở tab "Nhìn lại" (P7).
 */
@Component
public class PlaceRatingLookup {

    public Set<String> dislikedExternalIds(Long userId) {
        return Set.of();
    }

    public Set<String> likedExternalIds(Long userId) {
        return Set.of();
    }

    public List<String> likedNames(Long userId) {
        return List.of();
    }

    public List<String> dislikedNames(Long userId) {
        return List.of();
    }
}
