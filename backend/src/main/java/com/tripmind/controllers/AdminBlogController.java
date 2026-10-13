package com.tripmind.controllers;

import com.tripmind.domains.requests.KnowledgeRequest;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.ExtractedKnowledgeResponse;
import com.tripmind.domains.responses.KnowledgeResponse;
import com.tripmind.services.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/admin/blog")
@RequiredArgsConstructor
@Tag(name = "Admin Blog Dataset", description = "Quản trị trích xuất bài viết cộng đồng thành Dataset tri thức du lịch cho AI")
@SecurityRequirement(name = "bearerAuth")
public class AdminBlogController {

    private final BlogService blogService;

    @PostMapping("/posts/{id}/extract-knowledge")
    @Operation(summary = "AI phân tích và chắt lọc bài viết + các bình luận thành bản nháp tri thức du lịch")
    public ResponseEntity<ApiResponse<ExtractedKnowledgeResponse>> extractKnowledge(@PathVariable Long id) {
        ExtractedKnowledgeResponse extracted = blogService.extractKnowledgeFromPost(id);
        return ResponseEntity.ok(ApiResponse.ok("Trích xuất tri thức từ bài viết thành công", extracted));
    }

    @PostMapping("/posts/{id}/convert-to-knowledge")
    @Operation(summary = "Admin xác nhận lưu tri thức đã chắt lọc vào bảng destination_knowledge")
    public ResponseEntity<ApiResponse<KnowledgeResponse>> convertToKnowledge(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeRequest request) {
        KnowledgeResponse created = blogService.convertPostToKnowledge(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Đã chuyển bài viết thành Dataset tri thức du lịch cho AI", created));
    }
}
