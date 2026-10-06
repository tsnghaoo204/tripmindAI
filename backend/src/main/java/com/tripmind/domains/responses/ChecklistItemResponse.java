package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripmind.enums.ChecklistCategory;
import com.tripmind.enums.ChecklistKind;
import com.tripmind.enums.ChecklistSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChecklistItemResponse {

    /** Null với mục gợi ý chưa được chọn. */
    private Long id;
    private ChecklistKind kind;
    private String title;
    private ChecklistCategory category;
    private LocalDate dueDate;
    private Boolean done;
    private ChecklistSource source;
    private String reason;
}
