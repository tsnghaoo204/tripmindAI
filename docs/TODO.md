# TripMind AI — Danh sách Việc cần làm (TO-DO List)

> **Cập nhật:** 08/09/2026  
> **Nguyên tắc kỹ thuật:** Bám sát CSDL [schemas.sql](../schemas.sql), cấu hình trong [application.yml](../backend/src/main/resources/application.yml), JWT không dùng refresh token, lịch trình linh hoạt theo cụm địa điểm & khoảng cách (không ép giờ cứng nhắc), AI minh bạch với cơ chế Giải trình & Hoàn tác.

---

## 📊 Tiến độ tổng quan

- [x] **Giai đoạn 1: Xác thực, Phân quyền & User Service** *(Đã hoàn thành 100%)*
- [x] **Giai đoạn 2: Quản lý Điểm đến & Chuyến đi cơ bản** *(Đã hoàn thành 100%)*
- [x] **Giai đoạn 3: Lịch trình linh hoạt, Cụm địa điểm & Khoảng cách** *(Đã hoàn thành 100%)*
- [ ] **Giai đoạn 4: Dịch vụ ngoài (Thời tiết & Tỷ giá)** *(Ưu tiên số 1)*
- [ ] **Giai đoạn 5: Ngân sách & Chi tiêu thực tế** *(Ưu tiên số 3)*
- [ ] **Giai đoạn 6: Trợ lý AI Agent & Bộ công cụ Tool Calling** *(Ưu tiên số 4)*
- [ ] **Giai đoạn 7: Cơ chế Đề xuất, Giải trình AI & Hoàn tác** *(Ưu tiên số 5 — Cốt lõi đề tài)*

---

##  CHI TIẾT TỪNG GIAI ĐOẠN

### Giai đoạn 1: Xác thực & Quản lý người dùng (AUTH & USER)
*Đã hoàn thành và kiểm thử nghiệm thu xanh 100%.*

- [x] `AUTH-01`: Cấu hình JWT Properties & Token Provider (HMAC-SHA, sinh & validate token, trích xuất claims).
- [x] `AUTH-02`: Cài đặt `UserPrincipal` & `CustomUserDetailsService` tích hợp chuẩn `UserDetails`.
- [x] `AUTH-03`: Bộ lọc `JwtAuthenticationFilter` & `SecurityConfig` (Stateless, CORS, xử lý 401/403 JSON).
- [x] `AUTH-04`: Tiện ích `SecurityUtils.getCurrentUserId()` trích xuất danh tính người dùng thực.
- [x] `AUTH-05`: API Đăng ký tài khoản `POST /api/auth/register` (BCrypt password hash, chống trùng email).
- [x] `AUTH-06`: API Đăng nhập `POST /api/auth/login` (cấp JWT Access Token 24h).
- [x] `AUTH-07`: API Thông tin cá nhân `GET /api/auth/me` & `GET /api/users/profile`.
- [x] `AUTH-08`: API Cập nhật Profile `PUT /api/users/profile` (tên, avatar).
- [x] `AUTH-09`: API Đổi mật khẩu `PUT /api/users/change-password` (so khớp mật khẩu cũ, băm mật khẩu mới).
- [x] `AUTH-10`: Đồng bộ `TripController` sử dụng `SecurityUtils.getCurrentUserId()` thay cho mock.

---

### Giai đoạn 2: Điểm đến & Chuyến đi cơ bản (TRIP & DESTINATION)
*Đã hoàn thành và kiểm thử nghiệm thu xanh 100%.*

- [x] `TRIP-01`: Catalog 15 điểm đến gợi ý có sẵn trong CSDL (`destinations`).
- [x] `TRIP-02`: Tích hợp Google Places Client (`searchText`, `getPlaceDetails`, `nearbySearch`).
- [x] `TRIP-03`: `DestinationService`: Tra cứu nội bộ kết hợp nạp động qua Google Places khi điểm đến chưa có trong DB.
- [x] `TRIP-04`: `DestinationController`: API lấy điểm đến phổ biến `GET /api/destinations/popular`, tìm kiếm `GET /api/destinations?query=...`, chi tiết `GET /api/destinations/{id}`.
- [x] `TRIP-05`: `PlaceService`: Tìm kiếm địa điểm, xem chi tiết đánh giá (rating, review, giờ mở cửa), bổ sung cơ chế fallback nội bộ khi Google hết quota.
- [x] `TRIP-06`: `TripService.createTrip`: Tạo chuyến đi và tự động sinh đủ các ngày (`itinerary_days`).
- [x] `TRIP-07`: API lấy danh sách chuyến đi `GET /api/trips`, chi tiết `GET /api/trips/{id}`, xóa `DELETE /api/trips/{id}`.
- [x] `TRIP-08`: API cập nhật thông tin chuyến đi `PUT /api/trips/{id}` (đổi tên, ngân sách, số người, tiền tệ).
- [x] `TRIP-09`: Xử lý lazy loading Hibernate proxy & cấu hình `open-in-view: true`.

---

### Giai đoạn 3: Lịch trình linh hoạt, Cụm địa điểm & Khoảng cách (ITINERARY & ACTIVITIES)
*Đã hoàn thành và kiểm thử nghiệm thu xanh 100%.*

- [x] `ITIN-01`: Hoàn thiện Entity & Repository cho `ActivityEntity` (gắn với `itinerary_days`, `places`).
- [x] `ITIN-02`: Dịch vụ tính khoảng cách `DistanceService` (công thức Haversine tính km và thời gian di chuyển ước tính phút giữa 2 tọa độ).
- [x] `ITIN-03`: API Lấy toàn bộ lịch trình chuyến đi `GET /api/trips/{id}/itinerary` (trả về danh sách ngày, các hoạt động được sắp theo thứ tự, kèm khoảng cách đến điểm kế tiếp và gợi ý khung giờ vàng).
- [x] `ITIN-04`: API Thêm hoạt động vào ngày `POST /api/trips/{id}/itinerary/activities` (nhận `placeId` hoặc nhập tự do, ghi chú khung giờ đẹp, chi phí ước tính).
- [x] `ITIN-05`: API Cập nhật hoạt động `PUT /api/activities/{id}` và Xóa hoạt động `DELETE /api/activities/{id}`.
- [x] `ITIN-06`: API Sắp xếp lại thứ tự (Kéo thả / Drag-Drop) `PUT /api/itinerary-days/{dayId}/reorder` (cập nhật lại `orderIndex` liên tục và tính lại khoảng cách theo thứ tự mới).
- [x] `ITIN-07`: Bảng `saved_places`: API Lưu địa điểm yêu thích `POST /api/places/{id}/save`, Bỏ lưu `DELETE /api/places/{id}/save` và Lấy danh sách `GET /api/me/saved-places`.

---

### Giai đoạn 4: Dịch vụ ngoài — Thời tiết & Tỷ giá (EXTERNAL SERVICES)
🎯 **Mục tiêu**: Dự báo thời tiết thực tế cho từng ngày trong chuyến đi và hỗ trợ quy đổi ngoại tệ.

- [ ] `EXT-01`: Client tích hợp `Open-Meteo API` (dự báo nhiệt độ min/max, khả năng mưa theo tọa độ điểm đến).
- [ ] `EXT-02`: `WeatherService`: Nhận diện ngày trong chuyến đi để trả về dự báo thời tiết chi tiết từng ngày.
- [ ] `EXT-03`: API Thời tiết chuyến đi `GET /api/trips/{id}/weather` (phục vụ giao diện hiển thị badge thời tiết từng ngày).
- [ ] `EXT-04`: Client tích hợp `Exchange Rate API` (quy đổi chi phí khi đi du lịch nước ngoài sang đơn vị tiền của chuyến đi).

---

### Giai đoạn 5: Quản lý Ngân sách & Chi tiêu thực tế (BUDGET & EXPENSES)
🎯 **Mục tiêu**: Theo dõi chi tiêu thực tế phát sinh trong chuyến đi và cảnh báo hạn mức.

- [ ] `BUD-01`: Entity & Repository cho `ExpenseEntity` (bảng `expenses` trong `schemas.sql`).
- [ ] `BUD-02`: API Thêm khoản chi thực tế `POST /api/trips/{id}/expenses` (số tiền, 6 hạng mục: Lưu trú, Ăn uống, Di chuyển, Vé, Mua sắm, Khác, người chi).
- [ ] `BUD-03`: API Lấy danh sách chi tiêu `GET /api/trips/{id}/expenses` và Xóa khoản chi `DELETE /api/expenses/{id}`.
- [ ] `BUD-04`: Tích hợp `BudgetService.calculate`: So sánh tổng ước tính (từ `activities`) vs tổng thực chi (từ `expenses`), tính số tiền còn lại (`remaining`).
- [ ] `BUD-05`: Cảnh báo hạn mức ngân sách (`warningLevel`: `NONE` <=90%, `NEAR_LIMIT` >90%, `OVER` >100%).
- [ ] `BUD-06`: API Tổng quan ngân sách `GET /api/trips/{id}/budget`.

---

### Giai đoạn 6: Nền tảng Trợ lý AI & Bộ công cụ Tool Calling (AI AGENT CORE)
🎯 **Mục tiêu**: Tích hợp Gemini API với cơ chế gọi công cụ nội bộ và phát luồng trả lời Server-Sent Events.

- [ ] `AI-01`: Cấu hình Gemini Client (`GEMINI_API_KEY`, model `gemini-1.5-flash` từ `application.yml`).
- [ ] `AI-02`: Quản lý lịch sử hội thoại: Entity & Repository cho `ConversationEntity`, `MessageEntity`, `AiToolExecutionEntity`.
- [ ] `AI-03`: Xây dựng bộ công cụ đọc dữ liệu (AI Read Tools):
  - `tool_get_current_trip`: Đọc thông tin chuyến đi hiện tại.
  - `tool_get_itinerary`: Đọc danh sách hoạt động và khoảng cách giữa các điểm.
  - `tool_get_weather`: Đọc dự báo thời tiết của chuyến đi.
  - `tool_search_places`: Tìm địa điểm tiện đường / quán ăn theo yêu cầu.
  - `tool_calculate_budget`: Đọc tình hình ngân sách và chi tiêu.
- [ ] `AI-04`: Vòng lặp Agent (`AgentService`): Nhận tin nhắn người dùng -> Gọi Tool nếu cần -> Thu thập kết quả -> Trả lời.
- [ ] `AI-05`: API Chatbot AI thời gian thực `POST /api/trips/{id}/ai/chat` (phát luồng SSE: stream từng từ và sự kiện gọi tool).

---

### Giai đoạn 7: Đề xuất, Giải trình AI & Hoàn tác an toàn (PROPOSAL, EXPLAIN & UNDO)
🎯 **Mục tiêu**: Điểm khác biệt lớn nhất của đề tài — AI không tự ý sửa DB mà sinh đề xuất kèm giải trình minh bạch, người dùng có thể duyệt hoặc hoàn tác.

- [ ] `PROP-01`: Entity & Repository cho `AiProposalEntity` (bảng `ai_proposals` trong `schemas.sql`).
- [ ] `PROP-02`: Công cụ AI ghi đề xuất `tool_propose_itinerary_changes`: Tạo bản ghi `ai_proposals` chứa danh sách thay đổi (ADD / REMOVE / REORDER) kèm lý do giải trình.
- [ ] `PROP-03`: **Giải trình AI (Explainable AI)**: Lưu metadata giải thích vì sao chọn điểm (1. Tiện đường cách bao xa, 2. Khung giờ vàng vì sao nên ghé, 3. Rating và giờ mở cửa).
- [ ] `PROP-04`: API Xem trước đề xuất `GET /api/proposals/{id}` (trả về dữ liệu để giao diện render Thẻ so sánh Diff Card).
- [ ] `PROP-05`: API Duyệt đề xuất `POST /api/trips/{id}/ai/apply` (áp dụng các thay đổi vào bảng `activities` trong một transaction an toàn, lưu snapshot hoàn tác).
- [ ] `PROP-06`: API Từ chối đề xuất `POST /api/proposals/{id}/reject`.
- [ ] `PROP-07`: **Hoàn tác an toàn (Undo)** `POST /api/proposals/{id}/undo`: Khôi phục lại trạng thái lịch trình trước khi áp dụng đề xuất.

---

## 🚀 Bước cần làm ngay tiếp theo
Bắt đầu triển khai **Giai đoạn 4: Dịch vụ ngoài — Thời tiết & Tỷ giá (EXT-01 -> EXT-04)**.
