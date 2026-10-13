package com.tripmind.controllers;

import com.tripmind.configurations.security.SecurityUtils;
import com.tripmind.domains.requests.BlogCommentRequest;
import com.tripmind.domains.requests.BlogPostRequest;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.BlogCommentResponse;
import com.tripmind.domains.responses.BlogPostResponse;
import com.tripmind.domains.responses.PageResponse;
import com.tripmind.services.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
@Tag(name = "Community Blog", description = "Cộng đồng chia sẻ kinh nghiệm du lịch, bình luận và upvote")
public class BlogController {

    private final BlogService blogService;

    @GetMapping("/posts")
    @Operation(summary = "Lấy danh sách bài viết cộng đồng (lọc theo điểm đến hoặc tìm kiếm)")
    public ResponseEntity<ApiResponse<PageResponse<BlogPostResponse>>> getPosts(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        Long currentUserId = SecurityUtils.getOptionalUserId();
        return ResponseEntity.ok(ApiResponse.ok(blogService.getPosts(destination, search, page, size, currentUserId)));
    }

    @GetMapping("/posts/destinations")
    @Operation(summary = "Lấy danh sách các điểm đến có bài viết cộng đồng")
    public ResponseEntity<ApiResponse<List<String>>> getDestinations() {
        return ResponseEntity.ok(ApiResponse.ok(blogService.getDistinctDestinations()));
    }

    @GetMapping("/posts/{id}")
    @Operation(summary = "Xem chi tiết bài viết cộng đồng")
    public ResponseEntity<ApiResponse<BlogPostResponse>> getPostDetail(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getOptionalUserId();
        return ResponseEntity.ok(ApiResponse.ok(blogService.getPostDetail(id, currentUserId)));
    }

    @PostMapping("/posts")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Đăng bài viết chia sẻ kinh nghiệm chuyến đi")
    public ResponseEntity<ApiResponse<BlogPostResponse>> createPost(@Valid @RequestBody BlogPostRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        BlogPostResponse post = blogService.createPost(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Đăng bài viết thành công", post));
    }

    @DeleteMapping("/posts/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Xóa bài viết (chỉ tác giả hoặc Admin)")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        blogService.deletePost(userId, id, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Xóa bài viết thành công", null));
    }

    @PostMapping("/posts/{id}/upvote")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upvote hoặc bỏ upvote bài viết")
    public ResponseEntity<ApiResponse<Map<String, Object>>> togglePostUpvote(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(blogService.togglePostUpvote(userId, id)));
    }

    @GetMapping("/posts/{id}/comments")
    @Operation(summary = "Lấy danh sách bình luận của bài viết")
    public ResponseEntity<ApiResponse<List<BlogCommentResponse>>> getComments(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getOptionalUserId();
        return ResponseEntity.ok(ApiResponse.ok(blogService.getComments(id, currentUserId)));
    }

    @PostMapping("/posts/{id}/comments")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Thêm bình luận cho bài viết")
    public ResponseEntity<ApiResponse<BlogCommentResponse>> addComment(
            @PathVariable Long id,
            @Valid @RequestBody BlogCommentRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        BlogCommentResponse comment = blogService.addComment(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Đã gửi bình luận", comment));
    }

    @PostMapping("/comments/{commentId}/upvote")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upvote hoặc bỏ upvote bình luận")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleCommentUpvote(@PathVariable Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(blogService.toggleCommentUpvote(userId, commentId)));
    }
}
