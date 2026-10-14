package com.tripmind.domains.responses;

import com.tripmind.entities.BlogCommentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogCommentResponse {

    private Long id;
    private Long postId;
    private PublicUserResponse user;
    private String content;
    private int upvoteCount;
    @com.fasterxml.jackson.annotation.JsonProperty("isUpvoted")
    private boolean isUpvoted;
    private Instant createdAt;
    private Instant updatedAt;

    public static BlogCommentResponse fromEntity(BlogCommentEntity entity, boolean isUpvoted) {
        if (entity == null) {
            return null;
        }
        return BlogCommentResponse.builder()
                .id(entity.getId())
                .postId(entity.getPost() != null ? entity.getPost().getId() : null)
                .user(PublicUserResponse.fromEntity(entity.getUser()))
                .content(entity.getContent())
                .upvoteCount(entity.getUpvoteCount())
                .isUpvoted(isUpvoted)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
