package com.tripmind.domains.requests;

import com.tripmind.enums.ChecklistCategory;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Sửa hoặc tick một mục checklist. Trường null thì giữ nguyên. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChecklistItemRequest {

    @Size(max = 120)
    private String title;

    private Boolean done;

    private ChecklistCategory category;

    private LocalDate dueDate;
}
