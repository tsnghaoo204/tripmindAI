package com.tripmind.domains.requests;

import com.tripmind.enums.KnowledgeCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRequest {

    private Long destinationId;

    @NotBlank(message = "Tên điểm đến không được để trống")
    @Size(max = 160, message = "Tên điểm đến tối đa 160 ký tự")
    private String destinationName;

    @NotNull(message = "Chuyên mục không được để trống")
    private KnowledgeCategory category;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung kinh nghiệm không được để trống")
    @Size(max = 20000, message = "Nội dung tối đa 20000 ký tự")
    private String content;

    @Size(max = 200, message = "Tên địa điểm tối đa 200 ký tự")
    private String placeName;

    @Size(max = 100, message = "Tên nguồn tối đa 100 ký tự")
    private String sourceName;

    @Size(max = ValidationLimits.URL_MAX)
    private String sourceUrl;

    @Size(max = 20, message = "Tối đa 20 tag")
    private List<@Size(max = 40) String> tags;

    @Min(value = 1, message = "Độ ưu tiên tối thiểu là 1")
    @Max(value = 3, message = "Độ ưu tiên tối đa là 3")
    @Builder.Default
    private int priority = 1;

    @Builder.Default
    private Boolean active = true;
}
