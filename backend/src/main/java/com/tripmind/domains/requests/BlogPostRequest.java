package com.tripmind.domains.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostRequest {

    @NotBlank(message = "Tên điểm đến không được để trống")
    @Size(max = 160)
    private String destinationName;

    @NotBlank(message = "Tiêu đề bài viết không được để trống")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "Nội dung bài viết không được để trống")
    private String content;

    private Long tripId;

    @Builder.Default
    private List<String> photoUrls = new ArrayList<>();
}
