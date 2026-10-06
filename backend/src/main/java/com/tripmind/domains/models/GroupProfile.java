package com.tripmind.domains.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripmind.enums.DietaryRestriction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Thông tin nhóm đi của một chuyến, lưu ở cột {@code trips.group_profile}.
 *
 * <p>Mọi trường đều không bắt buộc: {@code {}} nghĩa là người dùng chưa khai. Hệ thống dùng
 * nó bằng mã (giảm số hoạt động mỗi ngày, thêm "chay" vào truy vấn tìm quán, cảnh báo chặng
 * đi bộ dài), không trông vào việc mô hình tự nhớ.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GroupProfile(
        @Min(0) @Max(50) Integer children,
        @Min(0) @Max(50) Integer seniors,
        Set<DietaryRestriction> dietary,
        Boolean limitedMobility,
        @Size(max = 200) String note) {

    public static GroupProfile empty() {
        return new GroupProfile(null, null, null, null, null);
    }

    @JsonIgnore
    public int childrenCount() {
        return children == null ? 0 : children;
    }

    @JsonIgnore
    public int seniorsCount() {
        return seniors == null ? 0 : seniors;
    }

    @JsonIgnore
    public Set<DietaryRestriction> dietaryOrEmpty() {
        return dietary == null ? Set.of() : dietary;
    }

    /** Nhóm có trẻ nhỏ, người lớn tuổi hoặc người đi lại khó: cần nhịp chậm, chặng ngắn. */
    @JsonIgnore
    public boolean needsGentlePace() {
        return childrenCount() > 0 || seniorsCount() > 0 || Boolean.TRUE.equals(limitedMobility);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return childrenCount() == 0 && seniorsCount() == 0 && dietaryOrEmpty().isEmpty()
                && !Boolean.TRUE.equals(limitedMobility) && (note == null || note.isBlank());
    }
}
