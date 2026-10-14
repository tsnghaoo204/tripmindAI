package com.tripmind.domains.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogCommentRequest {

    @NotBlank(message = "Nội dung bình luận không được để trống")
    @Size(max = 2000, message = "Bình luận tối đa 2000 ký tự")
    private String content;
}
