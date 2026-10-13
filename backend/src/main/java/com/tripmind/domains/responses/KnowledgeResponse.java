package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tripmind.entities.DestinationKnowledgeEntity;
import com.tripmind.enums.KnowledgeCategory;
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
public class KnowledgeResponse {

    private Long id;
    private Long destinationId;
    private String destinationName;
    private KnowledgeCategory category;
    private String categoryLabel;
    private String title;
    private String content;
    private String placeName;
    private String sourceName;
    private String sourceUrl;
    private List<String> tags;
    private int priority;

    @JsonProperty("isActive")
    private boolean active;

    private Instant createdAt;
    private Instant updatedAt;

    public static KnowledgeResponse fromEntity(DestinationKnowledgeEntity entity) {
        if (entity == null) return null;
        return KnowledgeResponse.builder()
                .id(entity.getId())
                .destinationId(entity.getDestination() != null ? entity.getDestination().getId() : null)
                .destinationName(entity.getDestinationName())
                .category(entity.getCategory())
                .categoryLabel(getCategoryLabel(entity.getCategory()))
                .title(entity.getTitle())
                .content(entity.getContent())
                .placeName(entity.getPlaceName())
                .sourceName(entity.getSourceName())
                .sourceUrl(entity.getSourceUrl())
                .tags(entity.getTags())
                .priority(entity.getPriority())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static String getCategoryLabel(KnowledgeCategory category) {
        if (category == null) return "";
        return switch (category) {
            case FOOD -> "Ăn uống";
            case ACCOMMODATION -> "Lưu trú";
            case TRANSPORT -> "Đi lại";
            case SIGHTSEEING -> "Tham quan";
            case TIP -> "Mẹo hay";
            case WARNING -> "Cảnh báo / Tránh bẫy";
        };
    }
}
