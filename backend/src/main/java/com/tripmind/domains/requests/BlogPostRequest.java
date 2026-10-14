package com.tripmind.domains.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Size(max = 160, message = "Tên điểm đến tối đa 160 ký tự")
    private String destinationName;

    @NotBlank(message = "Tiêu đề bài viết không được để trống")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    @NotBlank(message = "Nội dung bài viết không được để trống")
    @Size(max = 20000, message = "Nội dung tối đa 20000 ký tự")
    private String content;

    private Long tripId;

    @Builder.Default
    @Size(max = 10, message = "Tối đa 10 ảnh")
    private List<@NotBlank @Size(max = ValidationLimits.URL_MAX)
            @Pattern(regexp = ValidationLimits.HTTPS_URL, message = ValidationLimits.HTTPS_URL_MESSAGE) String> photoUrls
            = new ArrayList<>();
}
