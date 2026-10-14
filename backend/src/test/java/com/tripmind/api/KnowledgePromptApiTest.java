package com.tripmind.api;

import com.tripmind.domains.requests.KnowledgeRequest;
import com.tripmind.entities.DestinationEntity;
import com.tripmind.enums.KnowledgeCategory;
import com.tripmind.services.DestinationKnowledgeService;
import com.tripmind.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class KnowledgePromptApiTest extends IntegrationTest {

    @Autowired
    private DestinationKnowledgeService knowledgeService;

    @Test
    @DisplayName("Tri thức bị admin tắt thì không còn được nạp vào prompt AI, bật lại thì có")
    void inactiveKnowledgeExcludedFromPrompt() throws Exception {
        String admin = registerAdmin();
        // Tên điểm đến riêng để không lẫn với dữ liệu của lớp test khác
        String destination = "Prompt Town " + UUID.randomUUID();
        String keep = "Giữ lại " + UUID.randomUUID();
        String hide = "Ẩn đi " + UUID.randomUUID();

        call(post("/api/admin/knowledge"), admin, request(destination, keep), 201);
        long hiddenId = call(post("/api/admin/knowledge"), admin, request(destination, hide), 201)
                .at("/data/id").asLong();

        assertThat(knowledgeService.getFormattedKnowledgeForPrompt(byName(destination))).contains(keep, hide);

        call(patch("/api/admin/knowledge/" + hiddenId + "/toggle"), admin, null, 200);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_active FROM destination_knowledge WHERE id = ?", Boolean.class, hiddenId)).isFalse();

        String prompt = knowledgeService.getFormattedKnowledgeForPrompt(byName(destination));
        assertThat(prompt).contains(keep).doesNotContain(hide);
        assertThat(knowledgeService.getActiveKnowledgeEntities(byName(destination)))
                .extracting(e -> e.getTitle()).containsExactly(keep);
        assertThat(knowledgeService.searchKnowledge(null, destination, "", 10))
                .extracting(k -> k.getTitle()).doesNotContain(hide);

        call(patch("/api/admin/knowledge/" + hiddenId + "/toggle"), admin, null, 200);
        assertThat(knowledgeService.getFormattedKnowledgeForPrompt(byName(destination))).contains(keep, hide);
    }

    // Mục tri thức chỉ gắn theo tên nên tra bằng điểm đến chưa có id
    private DestinationEntity byName(String name) {
        DestinationEntity destination = new DestinationEntity();
        destination.setName(name);
        return destination;
    }

    private KnowledgeRequest request(String destination, String title) {
        return KnowledgeRequest.builder()
                .destinationName(destination)
                .category(KnowledgeCategory.TIP)
                .title(title)
                .content("Nội dung " + title)
                .tags(List.of("test"))
                .priority(2)
                .active(true)
                .build();
    }
}
