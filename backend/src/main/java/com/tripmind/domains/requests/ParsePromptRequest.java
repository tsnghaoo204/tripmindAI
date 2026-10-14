package com.tripmind.domains.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Câu mô tả chuyến đi bằng ngôn ngữ tự nhiên, gửi cho AI tách thành các trường. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsePromptRequest {

    @NotBlank(message = "prompt is required")
    @Size(max = 1000, message = "prompt cannot exceed 1000 characters")
    private String prompt;
}
