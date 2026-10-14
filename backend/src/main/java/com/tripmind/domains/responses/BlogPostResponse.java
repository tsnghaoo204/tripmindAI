package com.tripmind.domains.responses;

import com.tripmind.entities.BlogPostEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostResponse {

    private Long id;
    private PublicUserResponse user;
    private Long tripId;
    private String destinationName;
    private String title;
    private String content;
    private List<String> photoUrls;
    private int upvoteCount;
    private int commentCount;
    @com.fasterxml.jackson.annotation.JsonProperty("isUpvoted")
    private boolean isUpvoted;
    @com.fasterxml.jackson.annotation.JsonProperty("isConvertedToKnowledge")
    private boolean isConvertedToKnowledge;
    private Long convertedKnowledgeId;
    private Instant createdAt;
    private Instant updatedAt;

    public static BlogPostResponse fromEntity(BlogPostEntity entity, boolean isUpvoted) {
        if (entity == null) {
            return null;
        }
        return BlogPostResponse.builder()
                .id(entity.getId())
                .user(PublicUserResponse.fromEntity(entity.getUser()))
                .tripId(entity.getTrip() != null ? entity.getTrip().getId() : null)
                .destinationName(entity.getDestinationName())
                .title(entity.getTitle())
                .content(entity.getContent())
                .photoUrls(entity.getPhotoUrls())
                .upvoteCount(entity.getUpvoteCount())
                .commentCount(entity.getCommentCount())
                .isUpvoted(isUpvoted)
                .isConvertedToKnowledge(entity.getConvertedKnowledge() != null)
                .convertedKnowledgeId(entity.getConvertedKnowledge() != null ? entity.getConvertedKnowledge().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
