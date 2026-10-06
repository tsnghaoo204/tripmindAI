package com.tripmind.domains.requests;

import com.tripmind.enums.ChecklistCategory;
import com.tripmind.enums.ChecklistKind;
import com.tripmind.enums.ChecklistSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Thêm một mục checklist. {@code source = SUGGESTED / AI} khi người dùng chọn từ danh sách gợi ý. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemRequest {

    @NotNull(message = "kind is required (PACK or TODO)")
    private ChecklistKind kind;

    @NotBlank(message = "title is required")
    @Size(max = 120, message = "title cannot exceed 120 characters")
    private String title;

    private ChecklistCategory category;

    private LocalDate dueDate;

    private ChecklistSource source;

    @Size(max = 200)
    private String reason;
}
