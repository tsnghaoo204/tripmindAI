package com.tripmind.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class SecurityHardeningApiTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private record Account(String email, String token, long id) {
    }

    private Account register(String password) throws Exception {
        String email = "sec-" + UUID.randomUUID() + "@tripmind.test";
        JsonNode body = call(post("/api/auth/register"), null,
                Map.of("email", email, "password", password, "name", "Sec Tester"), 201);
        return new Account(email, body.at("/data/accessToken").asText(), body.at("/data/user/id").asLong());
    }

    private String admin() throws Exception {
        Account account = register("secret123");
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", account.id());
        return account.token();
    }

    private long createPost(String token) throws Exception {
        return call(post("/api/blog/posts"), token, Map.of(
                "destinationName", "Da Nang",
                "title", "Kinh nghiem",
                "content", "An mi Quang o cho Han",
                "photoUrls", List.of("https://example.com/a.jpg")), 201).at("/data/id").asLong();
    }

    private static void assertPublicUser(JsonNode user) {
        assertThat(user.has("id")).isTrue();
        assertThat(user.has("name")).isTrue();
        assertThat(user.has("email")).isFalse();
        assertThat(user.has("role")).isFalse();
        assertThat(user.has("isActive")).isFalse();
        assertThat(user.has("active")).isFalse();
        assertThat(user.has("createdAt")).isFalse();
    }

    @Test
    @DisplayName("Blog công khai: GET không cần đăng nhập và không lộ email/vai trò; POST ẩn danh bị 401")
    void blogPublicResponsesHidePrivateFields() throws Exception {
        Account author = register("secret123");
        long postId = createPost(author.token());
        call(post("/api/blog/posts/" + postId + "/comments"), author.token(), Map.of("content", "Bo sung"), 201);

        JsonNode list = get("/api/blog/posts?size=50", null, 200).at("/data/content");
        JsonNode mine = null;
        for (JsonNode p : list) {
            if (p.get("id").asLong() == postId) {
                mine = p;
            }
        }
        assertThat(mine).isNotNull();
        assertPublicUser(mine.get("user"));
        assertPublicUser(get("/api/blog/posts/" + postId, null, 200).at("/data/user"));
        assertPublicUser(get("/api/blog/posts/" + postId + "/comments", null, 200).at("/data/0/user"));
        assertThat(get("/api/blog/posts/" + postId, null, 200).toString()).doesNotContain(author.email());

        call(post("/api/blog/posts"), null, Map.of("destinationName", "X", "title", "T", "content", "C"), 401);
        call(post("/api/blog/posts/" + postId + "/comments"), null, Map.of("content", "x"), 401);
    }

    @Test
    @DisplayName("Xoá bài: người khác 403, quản trị xoá được; USER gọi trích xuất tri thức 403")
    void blogDeleteAndAdminExtractPermissions() throws Exception {
        Account author = register("secret123");
        Account other = register("secret123");
        long postId = createPost(author.token());

        call(delete("/api/blog/posts/" + postId), other.token(), null, 403);
        call(post("/api/admin/blog/posts/" + postId + "/extract-knowledge"), other.token(), null, 403);

        String admin = admin();
        llm.reset();
        llm.thenText("{\"title\":\"Mi Quang\",\"content\":\"Nen an o cho Han\",\"category\":\"FOOD\",\"sourceName\":\"gia mao\"}");
        JsonNode extracted = call(post("/api/admin/blog/posts/" + postId + "/extract-knowledge"), admin, null, 200)
                .get("data");
        assertThat(extracted.get("sourceSnippet").asText()).contains("An mi Quang");
        assertThat(extracted.get("sourceName").asText()).isEqualTo("Cộng đồng TripMind - Sec Tester");
        String prompt = llm.prompts().get(llm.prompts().size() - 1).getContents();
        assertThat(prompt).contains("<untrusted_post>").contains("</untrusted_post>");

        call(delete("/api/blog/posts/" + postId), admin, null, 200);
        get("/api/blog/posts/" + postId, null, 404);
    }

    @Test
    @DisplayName("Bài viết: không gắn được chuyến của người khác, ảnh phải là https và tối đa 10")
    void blogPostValidation() throws Exception {
        Account owner = register("secret123");
        Account other = register("secret123");
        long tripId = createTrip(owner.token(), LocalDate.now().plusDays(5).toString(),
                LocalDate.now().plusDays(6).toString()).get("id").asLong();

        Map<String, Object> body = new HashMap<>(Map.of("destinationName", "Da Nang", "title", "T", "content", "C",
                "tripId", tripId));
        call(post("/api/blog/posts"), other.token(), body, 404);
        assertThat(call(post("/api/blog/posts"), owner.token(), body, 201).at("/data/tripId").asLong()).isEqualTo(tripId);

        body.remove("tripId");
        body.put("photoUrls", List.of("http://insecure.example.com/a.jpg"));
        call(post("/api/blog/posts"), owner.token(), body, 400);
        body.put("photoUrls", List.of("javascript:alert(1)"));
        call(post("/api/blog/posts"), owner.token(), body, 400);
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            many.add("https://example.com/" + i + ".jpg");
        }
        body.put("photoUrls", many);
        call(post("/api/blog/posts"), owner.token(), body, 400);
        body.put("photoUrls", List.of());
        body.put("content", "x".repeat(20001));
        call(post("/api/blog/posts"), owner.token(), body, 400);
    }

    @Test
    @DisplayName("Upvote bật/tắt hai lần trả số đếm khớp với bảng upvote")
    void upvoteToggleIsConsistent() throws Exception {
        Account author = register("secret123");
        Account voter = register("secret123");
        long postId = createPost(author.token());

        JsonNode first = call(post("/api/blog/posts/" + postId + "/upvote"), voter.token(), null, 200).get("data");
        assertThat(first.get("upvoted").asBoolean()).isTrue();
        assertThat(first.get("upvoteCount").asInt()).isEqualTo(1);
        JsonNode second = call(post("/api/blog/posts/" + postId + "/upvote"), voter.token(), null, 200).get("data");
        assertThat(second.get("upvoted").asBoolean()).isFalse();
        assertThat(second.get("upvoteCount").asInt()).isZero();
        assertThat(get("/api/blog/posts/" + postId, null, 200).at("/data/upvoteCount").asInt()).isZero();

        call(post("/api/blog/posts/" + postId + "/upvote"), voter.token(), null, 200);
        JsonNode both = call(post("/api/blog/posts/" + postId + "/upvote"), author.token(), null, 200).get("data");
        assertThat(both.get("upvoteCount").asInt()).isEqualTo(2);

        long commentId = call(post("/api/blog/posts/" + postId + "/comments"), author.token(),
                Map.of("content", "hay"), 201).at("/data/id").asLong();
        JsonNode c1 = call(post("/api/blog/comments/" + commentId + "/upvote"), voter.token(), null, 200).get("data");
        JsonNode c2 = call(post("/api/blog/comments/" + commentId + "/upvote"), voter.token(), null, 200).get("data");
        assertThat(c1.get("upvoteCount").asInt()).isEqualTo(1);
        assertThat(c2.get("upvoteCount").asInt()).isZero();
        call(post("/api/blog/posts/999999999/upvote"), voter.token(), null, 404);
    }

    @Test
    @DisplayName("parse-prompt: ẩn danh 401, quá 1000 ký tự hoặc rỗng 400")
    void parsePromptRequiresAuthAndLimits() throws Exception {
        call(post("/api/trips/parse-prompt"), null, Map.of("prompt", "Da Nang 3 ngay"), 401);
        String token = registerUser();
        call(post("/api/trips/parse-prompt"), token, Map.of("prompt", "x".repeat(1001)), 400);
        call(post("/api/trips/parse-prompt"), token, Map.of("prompt", "  "), 400);
    }

    @Test
    @DisplayName("Places và destinations (gọi Google tính phí) bắt đăng nhập")
    void placesAndDestinationsRequireAuth() throws Exception {
        get("/api/places/search?query=pho", null, 401);
        get("/api/destinations", null, 401);
        get("/api/destinations/popular", null, 401);
        get("/api/destinations/popular", registerUser(), 200);
    }

    private JsonNode login(String email, String password, String ip, int expected) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/auth/login").with(r -> {
            r.setRemoteAddr(ip);
            return r;
        });
        return call(request, null, Map.of("email", email, "password", password), expected);
    }

    @Test
    @DisplayName("Đăng nhập sai 5 lần thì bị khoá 429, kể cả khi sau đó nhập đúng mật khẩu")
    void loginLockout() throws Exception {
        Account account = register("secret123");
        String ip = "10.77." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250);
        login(account.email(), "secret123", ip, 200);
        for (int i = 0; i < 5; i++) {
            login(account.email(), "wrong-password", ip, 401);
        }
        JsonNode locked = login(account.email(), "secret123", ip, 429);
        assertThat(locked.get("code").asText()).isEqualTo("RATE_LIMITED");
        assertThat(locked.at("/details/retryAfterSeconds").asLong()).isPositive();
        // Khoá theo email, đổi IP cũng không qua được
        login(account.email(), "secret123", "10.66.0.1", 429);
    }

    @Test
    @DisplayName("Mật khẩu đăng ký tối thiểu 8 ký tự")
    void passwordMinLength() throws Exception {
        call(post("/api/auth/register"), null, Map.of("email", "short-" + UUID.randomUUID() + "@tripmind.test",
                "password", "1234567", "name", "Short"), 400);
    }

    @Test
    @DisplayName("Đổi mật khẩu thu hồi phiếu cũ và trả phiếu mới; quản trị khoá tài khoản cũng thu hồi phiếu")
    void tokenRevokedAfterPasswordChange() throws Exception {
        Account account = register("secret123");
        get("/api/auth/me", account.token(), 200);

        JsonNode changed = call(put("/api/users/change-password"), account.token(),
                Map.of("currentPassword", "secret123", "newPassword", "new-secret-456"), 200);
        String fresh = changed.at("/data/accessToken").asText();
        assertThat(fresh).isNotBlank();
        get("/api/auth/me", account.token(), 401);
        get("/api/auth/me", fresh, 200);

        String admin = admin();
        call(put("/api/admin/users/" + account.id()), admin, Map.of("active", false), 200);
        get("/api/auth/me", fresh, 401);
        call(put("/api/admin/users/" + account.id()), admin, Map.of("active", true), 200);
        get("/api/auth/me", fresh, 401);
    }

    @Test
    @DisplayName("Phiếu truyền qua ?token= không còn xác thực được")
    void queryTokenNoLongerAuthenticates() throws Exception {
        String token = registerUser();
        get("/api/auth/me?token=" + token, null, 401);
        get("/api/auth/me", token, 200);
    }

    @Test
    @DisplayName("destinationData của client không ghi đè được dòng gieo sẵn; toạ độ/múi giờ sai bị 400")
    void destinationPoisoningIsBlocked() throws Exception {
        Map<String, Object> seeded = jdbc.queryForMap("""
                SELECT id, provider, external_id, name, country, latitude, longitude, timezone
                FROM destinations WHERE external_id IS NULL ORDER BY id LIMIT 1 OFFSET 1""");
        String token = registerUser();

        Map<String, Object> poisoned = new HashMap<>();
        poisoned.put("provider", "GOOGLE");
        poisoned.put("externalId", "attacker-place-" + UUID.randomUUID());
        poisoned.put("name", seeded.get("name"));
        poisoned.put("country", seeded.get("country"));
        poisoned.put("latitude", 1.5);
        poisoned.put("longitude", 2.5);
        poisoned.put("timezone", "Europe/London");
        JsonNode trip = call(post("/api/trips"), token, Map.of(
                "destinationData", poisoned,
                "name", "Chuyen doc",
                "startDate", LocalDate.now().plusDays(3).toString(),
                "endDate", LocalDate.now().plusDays(4).toString()), 201).get("data");
        assertThat(trip.at("/destination/id").asLong()).isEqualTo(((Number) seeded.get("id")).longValue());

        Map<String, Object> after = jdbc.queryForMap("""
                SELECT id, provider, external_id, name, country, latitude, longitude, timezone
                FROM destinations WHERE id = ?""", seeded.get("id"));
        assertThat(after).isEqualTo(seeded);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM destinations WHERE external_id = ?", Long.class,
                poisoned.get("externalId"))).isZero();

        Map<String, Object> badZone = new HashMap<>(poisoned);
        badZone.put("name", "Noi moi " + UUID.randomUUID());
        badZone.put("timezone", "Not/AZone");
        call(post("/api/trips"), token, Map.of("destinationData", badZone, "name", "x",
                "startDate", LocalDate.now().plusDays(3).toString(),
                "endDate", LocalDate.now().plusDays(4).toString()), 400);

        Map<String, Object> badLat = new HashMap<>(badZone);
        badLat.put("timezone", "Asia/Tokyo");
        badLat.put("latitude", 123);
        call(post("/api/trips"), token, Map.of("destinationData", badLat, "name", "x",
                "startDate", LocalDate.now().plusDays(3).toString(),
                "endDate", LocalDate.now().plusDays(4).toString()), 400);
    }

    @Test
    @DisplayName("Thân request quá 1MB bị 413; lỗi JSON không lộ chi tiết nội bộ")
    void requestBodyLimits() throws Exception {
        String token = registerUser();
        MvcResult tooLarge = mockMvc.perform(post("/api/blog/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"" + "x".repeat(1_100_000) + "\"}")).andReturn();
        assertThat(tooLarge.getResponse().getStatus()).isEqualTo(413);

        MvcResult malformed = mockMvc.perform(post("/api/blog/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": [1,2,")).andReturn();
        assertThat(malformed.getResponse().getStatus()).isEqualTo(400);
        assertThat(malformed.getResponse().getContentAsString()).doesNotContain("com.fasterxml").doesNotContain("jackson");
    }

    @Test
    @DisplayName("CORS chỉ cho origin trong danh sách")
    void corsAllowlist() throws Exception {
        MvcResult allowed = mockMvc.perform(options("/api/blog/posts")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")).andReturn();
        assertThat(allowed.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("http://localhost:5173");

        MvcResult denied = mockMvc.perform(options("/api/blog/posts")
                .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")).andReturn();
        assertThat(denied.getResponse().getStatus()).isEqualTo(403);
        assertThat(denied.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }
}
