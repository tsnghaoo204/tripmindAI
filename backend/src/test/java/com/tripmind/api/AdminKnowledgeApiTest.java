package com.tripmind.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.domains.requests.KnowledgeRequest;
import com.tripmind.enums.KnowledgeCategory;
import com.tripmind.services.DestinationKnowledgeService;
import com.tripmind.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class AdminKnowledgeApiTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DestinationKnowledgeService knowledgeService;

    @Test
    @DisplayName("Admin CRUD tri thức du lịch, lọc, toggle và kiểm tra nạp vào AI prompt")
    void adminKnowledgeLifecycle() throws Exception {
        String normalUser = registerUser();
        // 1. Người dùng thông thường bị chặn 403
        get("/api/admin/knowledge", normalUser, 403);

        // 2. Tạo tài khoản Admin
        String admin = registerUser();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = (SELECT MAX(id) FROM users)");

        // 3. Admin lấy danh sách có sẵn (seed data)
        JsonNode initialList = get("/api/admin/knowledge", admin, 200).get("data");
        assertThat(initialList.get("totalElements").asLong()).isPositive();

        // 4. Admin tạo một mẹo du lịch mới
        KnowledgeRequest createReq = KnowledgeRequest.builder()
                .destinationName("Da Nang")
                .category(KnowledgeCategory.FOOD)
                .title("Mì Quảng Bà Mua thơm ngon đậm đà")
                .content("Nên ăn Mì Quảng ếch hoặc Mì Quảng tôm thịt tại cơ sở đường Trần Bình Trọng.")
                .placeName("Mì Quảng Bà Mua")
                .sourceName("TikTok @foodievn")
                .sourceUrl("https://tiktok.com/@foodievn/123")
                .tags(List.of("mì quảng", "đặc sản"))
                .priority(3)
                .active(true)
                .build();

        JsonNode created = call(post("/api/admin/knowledge"), admin, createReq, 201).get("data");
        long knowledgeId = created.get("id").asLong();
        assertThat(created.get("title").asText()).isEqualTo("Mì Quảng Bà Mua thơm ngon đậm đà");
        assertThat(created.get("isActive").asBoolean()).isTrue();

        // 5. Kiểm tra hàm nạp tri thức cho AI prompt
        String formattedPrompt = knowledgeService.getFormattedKnowledgeForPrompt("Da Nang");
        assertThat(formattedPrompt).contains("Mì Quảng Bà Mua");
        assertThat(formattedPrompt).contains("Bánh xèo Bà Dưỡng");

        // 6. Cập nhật tri thức
        createReq.setTitle("Mì Quảng Bà Mua - Cơ sở Trần Bình Trọng");
        JsonNode updated = call(put("/api/admin/knowledge/" + knowledgeId), admin, createReq, 200).get("data");
        assertThat(updated.get("title").asText()).isEqualTo("Mì Quảng Bà Mua - Cơ sở Trần Bình Trọng");

        // 7. Toggle trạng thái active
        JsonNode toggled = call(patch("/api/admin/knowledge/" + knowledgeId + "/toggle"), admin, null, 200).get("data");
        assertThat(toggled.get("isActive").asBoolean()).isFalse();

        // 8. Lấy danh sách điểm đến
        JsonNode destinations = get("/api/admin/knowledge/destinations", admin, 200).get("data");
        assertThat(destinations.isArray()).isTrue();
        assertThat(destinations.toString()).contains("Da Nang");

        // 9. Xóa tri thức
        call(delete("/api/admin/knowledge/" + knowledgeId), admin, null, 200);
        get("/api/admin/knowledge/" + knowledgeId, admin, 404);
    }
}
