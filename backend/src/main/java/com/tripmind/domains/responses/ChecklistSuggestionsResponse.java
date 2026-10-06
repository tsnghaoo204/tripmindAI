package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Ứng viên checklist từ bộ luật; không ghi gì cho tới khi người dùng chọn. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChecklistSuggestionsResponse {

    private List<ChecklistItemResponse> items;
    /** Nhóm luật không chạy được, ví dụ WEATHER khi dịch vụ thời tiết lỗi. Không đoán thay. */
    private List<String> skippedRules;
}
