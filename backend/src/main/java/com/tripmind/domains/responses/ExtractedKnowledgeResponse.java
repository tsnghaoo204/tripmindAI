package com.tripmind.domains.responses;

import com.tripmind.enums.KnowledgeCategory;
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
public class ExtractedKnowledgeResponse {

    private String destinationName;
    private KnowledgeCategory category;
    private String categoryLabel;
    private String title;
    private String content;
    private String placeName;
    private String sourceName;
    private String sourceUrl;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    @Builder.Default
    private int priority = 2;
    @Builder.Default
    private boolean active = true;
    private String reasoning;
}
