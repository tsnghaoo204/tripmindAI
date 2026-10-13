package com.tripmind.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.domains.requests.BlogPostRequest;
import com.tripmind.domains.requests.BlogCommentRequest;
import com.tripmind.domains.requests.KnowledgeRequest;
import com.tripmind.domains.responses.BlogPostResponse;
import com.tripmind.domains.responses.BlogCommentResponse;
import com.tripmind.domains.responses.ExtractedKnowledgeResponse;
import com.tripmind.domains.responses.KnowledgeResponse;
import com.tripmind.domains.responses.PageResponse;
import com.tripmind.entities.*;
import com.tripmind.enums.KnowledgeCategory;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.*;
import com.tripmind.services.ai.LlmGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogPostRepository postRepository;
    private final BlogCommentRepository commentRepository;
    private final BlogPostUpvoteRepository postUpvoteRepository;
    private final BlogCommentUpvoteRepository commentUpvoteRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final DestinationKnowledgeRepository destinationKnowledgeRepository;
    private final DestinationKnowledgeService destinationKnowledgeService;
    private final LlmGateway llm;
    private final ObjectMapper objectMapper;

    @Transactional
    public BlogPostResponse createPost(Long userId, BlogPostRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        TripEntity trip = null;
        if (request.getTripId() != null) {
            trip = tripRepository.findById(request.getTripId()).orElse(null);
        }

        BlogPostEntity post = BlogPostEntity.builder()
                .user(user)
                .trip(trip)
                .destinationName(request.getDestinationName().trim())
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .photoUrls(request.getPhotoUrls() != null ? request.getPhotoUrls() : new ArrayList<>())
                .build();

        BlogPostEntity saved = postRepository.save(post);
        log.info("User ID={} created blog post ID={} for destination='{}'", userId, saved.getId(), saved.getDestinationName());
        return BlogPostResponse.fromEntity(saved, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<BlogPostResponse> getPosts(String destination, String search, int page, int size, Long currentUserId) {
        Page<BlogPostEntity> postPage = postRepository.findFiltered(
                destination != null && !destination.isBlank() ? destination.trim() : null,
                search != null && !search.isBlank() ? search.trim() : null,
                PageRequest.of(page, size)
        );

        List<Long> postIds = postPage.getContent().stream().map(BlogPostEntity::getId).toList();
        Set<Long> upvotedIds = (currentUserId != null && !postIds.isEmpty())
                ? postUpvoteRepository.findUpvotedPostIds(currentUserId, postIds)
                : Collections.emptySet();

        List<BlogPostResponse> responses = postPage.getContent().stream()
                .map(p -> BlogPostResponse.fromEntity(p, upvotedIds.contains(p.getId())))
                .collect(Collectors.toList());

        return PageResponse.of(postPage.map(p -> BlogPostResponse.fromEntity(p, upvotedIds.contains(p.getId()))));
    }

    @Transactional(readOnly = true)
    public BlogPostResponse getPostDetail(Long postId, Long currentUserId) {
        BlogPostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Blog post not found: " + postId));
        boolean isUpvoted = currentUserId != null && postUpvoteRepository.existsByUserIdAndPostId(currentUserId, postId);
        return BlogPostResponse.fromEntity(post, isUpvoted);
    }

    @Transactional
    public void deletePost(Long userId, Long postId, boolean isAdmin) {
        BlogPostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Blog post not found: " + postId));

        if (!post.getUser().getId().equals(userId) && !isAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN, "You do not have permission to delete this post");
        }

        postRepository.delete(post);
        log.info("Deleted blog post ID={} by user ID={} (isAdmin={})", postId, userId, isAdmin);
    }

    @Transactional
    public Map<String, Object> togglePostUpvote(Long userId, Long postId) {
        BlogPostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Blog post not found: " + postId));

        boolean alreadyUpvoted = postUpvoteRepository.existsByUserIdAndPostId(userId, postId);
        if (alreadyUpvoted) {
            postUpvoteRepository.deleteByUserIdAndPostId(userId, postId);
            postRepository.updateUpvoteCount(postId, -1);
            int newCount = Math.max(0, post.getUpvoteCount() - 1);
            return Map.of("upvoted", false, "upvoteCount", newCount);
        } else {
            postUpvoteRepository.save(BlogPostUpvoteEntity.builder()
                    .userId(userId)
                    .postId(postId)
                    .build());
            postRepository.updateUpvoteCount(postId, 1);
            int newCount = post.getUpvoteCount() + 1;
            return Map.of("upvoted", true, "upvoteCount", newCount);
        }
    }

    @Transactional(readOnly = true)
    public List<BlogCommentResponse> getComments(Long postId, Long currentUserId) {
        List<BlogCommentEntity> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        List<Long> commentIds = comments.stream().map(BlogCommentEntity::getId).toList();
        Set<Long> upvotedIds = (currentUserId != null && !commentIds.isEmpty())
                ? commentUpvoteRepository.findUpvotedCommentIds(currentUserId, commentIds)
                : Collections.emptySet();

        return comments.stream()
                .map(c -> BlogCommentResponse.fromEntity(c, upvotedIds.contains(c.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public BlogCommentResponse addComment(Long userId, Long postId, BlogCommentRequest request) {
        BlogPostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Blog post not found: " + postId));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        BlogCommentEntity comment = BlogCommentEntity.builder()
                .post(post)
                .user(user)
                .content(request.getContent().trim())
                .build();

        BlogCommentEntity saved = commentRepository.save(comment);
        postRepository.updateCommentCount(postId, 1);
        log.info("User ID={} added comment ID={} to post ID={}", userId, saved.getId(), postId);
        return BlogCommentResponse.fromEntity(saved, false);
    }

    @Transactional
    public Map<String, Object> toggleCommentUpvote(Long userId, Long commentId) {
        BlogCommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Comment not found: " + commentId));

        boolean alreadyUpvoted = commentUpvoteRepository.existsByUserIdAndCommentId(userId, commentId);
        if (alreadyUpvoted) {
            commentUpvoteRepository.deleteByUserIdAndCommentId(userId, commentId);
            commentRepository.updateUpvoteCount(commentId, -1);
            int newCount = Math.max(0, comment.getUpvoteCount() - 1);
            return Map.of("upvoted", false, "upvoteCount", newCount);
        } else {
            commentUpvoteRepository.save(BlogCommentUpvoteEntity.builder()
                    .userId(userId)
                    .commentId(commentId)
                    .build());
            commentRepository.updateUpvoteCount(commentId, 1);
            int newCount = comment.getUpvoteCount() + 1;
            return Map.of("upvoted", true, "upvoteCount", newCount);
        }
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctDestinations() {
        return postRepository.findDistinctDestinations();
    }

    /**
     * AI Agent chắt lọc bài viết và các bình luận để trích xuất tri thức du lịch (RAG Dataset Distillation).
     */
    @Transactional(readOnly = true)
    public ExtractedKnowledgeResponse extractKnowledgeFromPost(Long postId) {
        BlogPostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Blog post not found: " + postId));
        List<BlogCommentEntity> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        StringBuilder commentsText = new StringBuilder();
        if (comments.isEmpty()) {
            commentsText.append("(Chưa có bình luận nào)");
        } else {
            for (int i = 0; i < comments.size(); i++) {
                BlogCommentEntity c = comments.get(i);
                commentsText.append(String.format("- [Bình luận %d] Tác giả %s (+%d upvotes): %s\n",
                        i + 1, c.getUser().getName(), c.getUpvoteCount(), c.getContent()));
            }
        }

        String promptText = String.format("""
                Bạn là chuyên gia thẩm định và chắt lọc tri thức du lịch cho TripMind AI.
                Dưới đây là một bài viết chia sẻ từ cộng đồng du lịch cùng các bình luận thảo luận bên dưới:

                === THÔNG TIN BÀI VIẾT ===
                Điểm đến: %s
                Tiêu đề: %s
                Nội dung bài viết:
                %s
                Tác giả bài viết: %s (Lượt upvote: %d)

                === CÁC BÌNH LUẬN ĐÓNG GÓP TỪ CỘNG ĐỒNG ===
                %s

                === NHIỆM VỤ CỦA BẠN ===
                1. Đọc và phân tích bài viết cùng các bình luận bên dưới.
                2. Chắt lọc lấy 01 kinh nghiệm du lịch thực tế, quán ăn ngon, show diễn, địa điểm ngắm cảnh hoặc mẹo/cảnh báo giá trị nhất.
                3. Bỏ qua các câu chào hỏi, tán gẫu, cảm ơn. Tập trung vào thông tin thực tế: tên quán/địa điểm, thời gian nên đi, mức giá, mẹo tránh bị chặt chém hoặc lưu ý an toàn.
                4. Trả về DUY NHẤT một chuỗi JSON hợp lệ (không kèm markdown ```json hay bất kỳ chữ giải thích nào trước sau) theo cấu trúc:
                {
                  "destinationName": "%s",
                  "category": "FOOD",
                  "title": "Tiêu đề kinh nghiệm ngắn gọn, súc tích (dưới 80 chữ)",
                  "content": "Nội dung mẹo và kinh nghiệm đã được tổng hợp mạch lạc, đầy đủ thông tin hữu ích",
                  "placeName": "Tên quán hoặc địa danh cụ thể (nếu có, không có thì để trống)",
                  "sourceName": "Cộng đồng TripMind - %s",
                  "tags": ["tag1", "tag2", "tag3"],
                  "priority": 2,
                  "reasoning": "Giải thích ngắn gọn tại sao chắt lọc được mẹo này từ bài viết và bình luận"
                }
                Lưu ý: "category" phải là một trong: FOOD, ACCOMMODATION, TRANSPORT, SIGHTSEEING, TIP, WARNING. "priority" là số từ 1 đến 3 (2: Khuyến nghị, 3: Rất quan trọng).
                """,
                post.getDestinationName(),
                post.getTitle(),
                post.getContent(),
                post.getUser().getName(),
                post.getUpvoteCount(),
                commentsText.toString(),
                post.getDestinationName(),
                post.getUser().getName()
        );

        String systemPrompt = "Bạn là AI chuyên gia trích xuất dữ liệu tri thức du lịch từ cộng đồng thành dataset chuẩn mực. Chỉ trả về JSON thuần túy.";

        String aiResponseText = llm.call(new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(promptText)
        ))).getResult().getOutput().getText();

        try {
            int start = aiResponseText.indexOf('{');
            int end = aiResponseText.lastIndexOf('}');
            if (start < 0 || end <= start) {
                throw new IllegalArgumentException("No JSON object in response");
            }
            String jsonClean = aiResponseText.substring(start, end + 1);
            ExtractedKnowledgeResponse extracted = objectMapper.readValue(jsonClean, ExtractedKnowledgeResponse.class);

            if (extracted.getDestinationName() == null || extracted.getDestinationName().isBlank()) {
                extracted.setDestinationName(post.getDestinationName());
            }
            if (extracted.getSourceName() == null || extracted.getSourceName().isBlank()) {
                extracted.setSourceName("Cộng đồng TripMind - " + post.getUser().getName());
            }
            if (extracted.getCategory() == null) {
                extracted.setCategory(KnowledgeCategory.TIP);
            }
            extracted.setCategoryLabel(getCategoryDisplay(extracted.getCategory()));
            return extracted;
        } catch (Exception e) {
            log.error("Khong parse duoc JSON tu AI extract knowledge: {}. Raw: {}", e.getMessage(), aiResponseText);
            // Fallback nếu AI trả format lỗi
            return ExtractedKnowledgeResponse.builder()
                    .destinationName(post.getDestinationName())
                    .category(KnowledgeCategory.TIP)
                    .categoryLabel("Kinh nghiệm")
                    .title(post.getTitle())
                    .content(post.getContent())
                    .sourceName("Cộng đồng TripMind - " + post.getUser().getName())
                    .tags(List.of("kinh nghiệm", "cộng đồng"))
                    .priority(2)
                    .active(true)
                    .reasoning("Trích xuất trực tiếp từ bài viết của " + post.getUser().getName())
                    .build();
        }
    }

    /**
     * Admin xác nhận lưu mẹo đã chắt lọc vào bảng destination_knowledge và liên kết với bài viết.
     */
    @Transactional
    public KnowledgeResponse convertPostToKnowledge(Long postId, KnowledgeRequest request) {
        BlogPostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Blog post not found: " + postId));

        KnowledgeResponse created = destinationKnowledgeService.create(request);
        DestinationKnowledgeEntity knowledgeEntity = destinationKnowledgeRepository.findById(created.getId()).orElse(null);

        post.setConvertedKnowledge(knowledgeEntity);
        postRepository.save(post);
        log.info("Admin converted blog post ID={} to destination knowledge ID={}", postId, created.getId());
        return created;
    }

    private String getCategoryDisplay(KnowledgeCategory category) {
        if (category == null) return "Mẹo";
        return switch (category) {
            case FOOD -> "Ăn uống";
            case ACCOMMODATION -> "Lưu trú";
            case TRANSPORT -> "Đi lại";
            case SIGHTSEEING -> "Tham quan";
            case TIP -> "Kinh nghiệm";
            case WARNING -> "Lưu ý quan trọng";
        };
    }
}
