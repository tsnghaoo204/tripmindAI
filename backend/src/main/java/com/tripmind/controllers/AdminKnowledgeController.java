package com.tripmind.controllers;

import com.tripmind.domains.requests.KnowledgeRequest;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.KnowledgeResponse;
import com.tripmind.domains.responses.PageResponse;
import com.tripmind.enums.KnowledgeCategory;
import com.tripmind.services.DestinationKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
@Tag(name = "Admin Knowledge", description = "Quản lý dataset tri thức du lịch và mẹo cộng đồng cho AI")
@SecurityRequirement(name = "bearerAuth")
public class AdminKnowledgeController {

    private final DestinationKnowledgeService knowledgeService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tri thức du lịch (lọc theo điểm đến, chuyên mục, từ khóa)")
    public ResponseEntity<ApiResponse<PageResponse<KnowledgeResponse>>> listKnowledge(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) KnowledgeCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ResponseEntity.ok(ApiResponse.ok(knowledgeService.findFiltered(destination, category, search, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một tri thức du lịch")
    public ResponseEntity<ApiResponse<KnowledgeResponse>> getKnowledge(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(knowledgeService.findById(id)));
    }

    @GetMapping("/destinations")
    @Operation(summary = "Lấy danh sách các điểm đến đã có dữ liệu tri thức")
    public ResponseEntity<ApiResponse<List<String>>> getDestinations() {
        return ResponseEntity.ok(ApiResponse.ok(knowledgeService.getDistinctDestinations()));
    }

    @PostMapping
    @Operation(summary = "Thêm mới tri thức / mẹo du lịch vào dataset")
    public ResponseEntity<ApiResponse<KnowledgeResponse>> createKnowledge(
            @Valid @RequestBody KnowledgeRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.created("Thêm tri thức du lịch thành công", knowledgeService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật tri thức du lịch")
    public ResponseEntity<ApiResponse<KnowledgeResponse>> updateKnowledge(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật tri thức thành công", knowledgeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa tri thức du lịch khỏi dataset")
    public ResponseEntity<ApiResponse<Void>> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa tri thức thành công", null));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Bật/tắt trạng thái sử dụng cho AI")
    public ResponseEntity<ApiResponse<KnowledgeResponse>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công", knowledgeService.toggleActive(id)));
    }
}
