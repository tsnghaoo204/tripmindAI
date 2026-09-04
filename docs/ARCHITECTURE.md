# TripMind — Tech Stack & Luồng hệ thống

> Đi kèm [PLAN.md](PLAN.md) (lộ trình 12 tuần). File này trả lời: **dùng gì** và **chạy như thế nào**.
>
> AI provider chính: **Google Gemini** qua lớp tương thích OpenAI. Có thể mở rộng sang provider khác mà không sửa code nghiệp vụ.

---

## 1. Tech Stack

### 1.1. Frontend

| Thành phần | Lựa chọn | Dùng để làm gì |
|---|---|---|
| Framework | React 18 + TypeScript | UI |
| Build | Vite | dev server + build |
| Styling | Tailwind CSS | toàn bộ giao diện |
| Routing | React Router v6 | điều hướng, protected route |
| Server state | TanStack Query | fetch, cache, invalidate dữ liệu từ API |
| UI state | Zustand | state của wizard tạo trip, sidebar AI |
| Bản đồ | Leaflet + react-leaflet, tile OpenStreetMap | marker, đường nối, thứ tự activity |
| Kéo-thả | dnd-kit | đổi thứ tự activity trong ngày |
| AI streaming | `EventSource` hoặc `fetch` + `ReadableStream` | nhận SSE từ agent |
| Form | React Hook Form + Zod | validate wizard 6 bước |
| Ngày tháng | date-fns | xử lý khoảng ngày của trip |

> Không dùng thư viện component có sẵn (MUI, Ant). Tailwind + tự viết component để giao diện không bị "template".

### 1.2. Backend

| Thành phần | Lựa chọn | Ghi chú |
|---|---|---|
| Ngôn ngữ | **Java 21 (LTS)** | xem §1.5 |
| Framework | **Spring Boot 4.1.x** | cần Spring Framework 7.0.9+ |
| Web | Spring Web MVC | REST + SSE |
| Bảo mật | Spring Security + JWT | tự cấu hình filter, không dùng session |
| ORM | Spring Data JPA + Hibernate | |
| Migration | **Flyway** | `ddl-auto: validate`, không bao giờ `update` |
| Cache | Spring Data Redis | weather, currency, places, rate limit |
| AI | **Spring AI 2.0.x** — `spring-ai-starter-openai` | chỉ chạy Boot 4.x. Trỏ vào Gemini, xem §2 |
| HTTP client | Spring `WebClient` | gọi API bên ngoài |
| Chịu lỗi | Resilience4j | timeout + retry + circuit breaker cho API ngoài |
| Mapper | MapStruct | entity ↔ DTO |
| Validate | Jakarta Bean Validation | |
| API docs | springdoc-openapi | Swagger UI |
| Rate limit | Bucket4j trên Redis, hoặc tự viết bằng `INCR` + `EXPIRE` | |
| Test | JUnit 5, Mockito, Spring Boot Test, Testcontainers | |

### 1.3. Dữ liệu & hạ tầng

```text
PostgreSQL 16    dữ liệu nghiệp vụ
Redis 7          cache + rate limit + trạng thái tạm của AI
Docker Compose   postgres · redis · backend · frontend · nginx
Nginx            reverse proxy, phục vụ static của React
```

### 1.4. Dịch vụ bên ngoài

| Dịch vụ | Provider | Key? | Ghi chú |
|---|---|---|---|
| LLM | **Google Gemini** (AI Studio API key) | có | provider chính |
| LLM dự phòng | Gateway đa model (OpenAI-compatible) | có | xem §2.3 |
| Địa điểm | Google Places API (New) | có | đặt quota cap trước khi code |
| Thời tiết | Open-Meteo Forecast + Archive | **không** | tối đa 16 ngày dự báo |
| Tỷ giá | open.er-api.com | **không** | có VND |
| Nền bản đồ | OpenStreetMap tiles | không | |
| Khoảng cách | Tự tính Haversine trong Java | không | không tốn API cho vòng lặp tối ưu |

### 1.5. Vì sao Java 21

Bốn lựa chọn, tra ngày 03/09/2026:

| | Trạng thái | Hết hỗ trợ | Virtual thread? | Boot 4.1 nhận? |
|---|---|---|---|---|
| Java 17 | LTS (09/2021) | 10/2027 | ❌ **không có** | ✅ (mức tối thiểu) |
| **Java 21** | **LTS (10/2023)** | **12/2029** | ✅ chính thức | ✅ |
| Java 25 | LTS (09/2025) | 09/2031 | ✅ | ✅ |
| Java 26 | non-LTS (03/2026) | ~09/2026 | ✅ | ✅ (mức tối đa) |

**Loại Java 17** — không phải vì cũ, mà vì **không có virtual thread**. Tính năng này chỉ xuất hiện dạng preview từ Java 19 (JEP 425), preview lần hai ở 20 (JEP 436), và chính thức ở 21 (JEP 444). Java 17 ra đời trước cả bản preview đầu tiên: không flag, không backport.

**Loại Java 26** — non-LTS, cửa sổ hỗ trợ 6 tháng, phát hành 03/2026 nên hết hạn ngay quanh thời điểm bắt đầu dự án.

**Chọn 21 thay vì 25** — cả hai đều LTS và đều có đủ thứ dự án cần. Chọn 21 vì độ phủ hệ sinh thái sâu nhất: Docker image, thư viện, tutorial, câu trả lời StackOverflow đều đã ổn định quanh nó. Dự án đã chấp nhận một rủi ro mới ở tầng Spring Boot 4.1 + Spring AI 2.0 (tutorial chưa theo kịp), nên không nên chồng thêm rủi ro ở tầng runtime. Hỗ trợ tới 12/2029, dư xa so với vòng đời đồ án.

> Đổi lên 25 về sau chỉ là sửa `<java.version>` trong `pom.xml`, không đụng source. Quyết định này không khoá gì cả.

**Virtual thread dùng để làm gì ở đây.** Agent loop gần như chỉ ngồi chờ I/O: một tin nhắn sinh 5–8 lượt gọi Gemini, mỗi lượt 2–5 giây, xen kẽ Google Places và Open-Meteo, trong khi kết nối SSE bị giữ mở 30–60 giây. Với thread nền tảng, pool mặc định của Tomcat thành trần chịu tải dù CPU gần như rảnh. Bật bằng một dòng:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

> Cân đúng trọng số: với tải cỡ đồ án (vài người dùng đồng thời lúc demo), virtual thread **không phải điều kiện sống còn** — pool thread thường vẫn chạy tốt. Giá trị thật của nó là một lựa chọn kiến trúc sạch và giải thích được trong báo cáo, không phải để cứu hiệu năng.

**Sealed interface + pattern matching cho `switch`** (chính thức từ 21) hợp đúng với mô hình thao tác trong `ai_proposals` — `ADD` / `REMOVE` / `UPDATE` / `REORDER` khai báo thành sealed hierarchy, quên xử lý một nhánh là trình biên dịch bắt ngay.

> **Đánh đổi phải biết trước:** Spring Boot 4 và Spring AI 2.0 còn mới. Phần lớn tutorial và câu trả lời StackOverflow vẫn viết cho Boot 3.x + Spring AI 1.x, và **tên artifact của Spring AI đã đổi giữa 1.x và 2.x**. Bám tài liệu chính thức, ghim version cứng, đừng copy cấu hình từ blog.

---

## 2. Cấu hình AI provider

### 2.1. Vì sao đi qua lớp tương thích OpenAI

Gemini expose một endpoint nói đúng giao thức OpenAI:

```text
https://generativelanguage.googleapis.com/v1beta/openai/
```

Theo tài liệu chính thức, lớp này **hỗ trợ đầy đủ function calling (tool calling), streaming và structured output** — đúng ba thứ dự án cần. Nghĩa là:

- Code chỉ viết một lần với `spring-ai-starter-openai`
- Đổi sang provider khác = đổi `base-url` + `api-key` + `model`, **không sửa một dòng code nghiệp vụ nào**
- Không bị khoá chặt vào Gemini

### 2.2. Cấu hình

```yaml
spring:
  ai:
    openai:
      base-url: https://generativelanguage.googleapis.com/v1beta/openai
      api-key: ${GEMINI_API_KEY}
      chat:
        options:
          model: ${LLM_MODEL:gemini-3.8-flash}
          temperature: 0.4
```

Ba biến môi trường, không hardcode: `GEMINI_API_KEY`, `LLM_MODEL`, `LLM_BASE_URL`.

**Chiến lược chọn model theo tác vụ:**

| Tác vụ | Model | Lý do |
|---|---|---|
| Chat thường, agent loop | bản **flash** | gọi nhiều lần mỗi lượt, cần rẻ và nhanh |
| Sinh itinerary cả chuyến, phân tích ngân sách | bản **pro** | chạy một lần, cần suy luận tốt |

> Model id thay đổi theo thời gian. Lấy danh sách hiện hành trong AI Studio rồi ghim vào `application.yml`, đừng copy từ blog.

### 2.3. Mở rộng sang provider khác

> ⚠️ Cần xác nhận: "9routes" mà bạn nhắc là **OpenRouter** hay một gateway khác? Cách làm bên dưới đúng cho mọi gateway nói giao thức OpenAI — chỉ khác giá trị `base-url`.

Spring AI tự cấu hình **một** `ChatModel` từ file properties. Muốn có provider thứ hai thì tự tạo bean:

```text
ChatModelRouter
├── primary   → Gemini flash    (mặc định, ~95% request)
├── deep      → Gemini pro      (sinh itinerary, phân tích)
└── fallback  → gateway khác    (khi Gemini 429 / 503 / hết quota)
```

Quy tắc: **service nghiệp vụ không bao giờ tự chọn model.** Chúng gọi `ChatModelRouter.forTask(TaskType.AGENT_CHAT)`. Đổi provider chỉ sửa router.

### 2.4. Ba điểm dễ vấp với Gemini

1. **Tham số lạ bị bỏ qua trong im lặng.** Tài liệu ghi rõ: tham số không nằm trong danh sách hỗ trợ sẽ bị lớp tương thích *silently ignored* — không báo lỗi. Nghĩa là cấu hình sai vẫn chạy nhưng không có tác dụng. Khi bật một option quan trọng, phải kiểm chứng bằng response thật chứ đừng tin là nó đã có hiệu lực.
2. **Lớp tương thích vẫn ở trạng thái beta.** Ghim version, và có kịch bản lui về SDK gốc của Google nếu một tính năng nào đó hụt.
3. **Rate limit free tier nhân lên theo agent loop.** Một tin nhắn người dùng có thể tạo 5–8 lượt gọi LLM (mỗi vòng tool là một lượt). 10 tin nhắn khi test = có thể 60–80 request. Google không công bố con số RPM/RPD trong tài liệu — phải xem trang rate limit trong AI Studio cho đúng tài khoản của mình, rồi đặt giới hạn phía ứng dụng thấp hơn.

---

## 3. Sơ đồ tổng thể

```text
                          Browser (React)
                                │
                    REST  │  SSE │  (JWT ở header)
                                ▼
                          Nginx reverse proxy
                                │
                                ▼
                    ┌───────────────────────┐
                    │     Spring Boot       │
                    │                       │
                    │  Controller layer     │
                    │         │             │
                    │  Service layer  ◄─────┼──── AI Tools gọi vào ĐÂY
                    │         │             │     (không gọi thẳng repository)
                    │  Repository layer     │
                    └───────────────────────┘
                       │        │        │
              ┌────────┘        │        └────────┐
              ▼                 ▼                 ▼
        PostgreSQL           Redis          Integration layer
                                                  │
                          ┌───────────┬───────────┼───────────┐
                          ▼           ▼           ▼           ▼
                       Gemini     Google      Open-Meteo   er-api
                       (LLM)      Places      (weather)   (tỷ giá)
```

**Quy tắc bất di bất dịch:** LLM không chạm database. Đường đi luôn là

```text
LLM → Tool → Service → Repository → PostgreSQL
```

Tool là một *client khác* của cùng service mà REST controller đang dùng. Nhờ vậy validation, business rule và kiểm tra quyền chỉ tồn tại ở một chỗ duy nhất.

---

## 4. Danh mục luồng

| # | Luồng | Nhóm | Tuần | Bắt buộc |
|---|---|---|---|---|
| 1 | Đăng ký / Đăng nhập / Refresh token | Nền | 1 | ✅ |
| 2 | Tạo trip qua wizard | Nền | 2 | ✅ |
| 3 | Xem & sửa itinerary | Nền | 3 | ✅ |
| 4 | Tìm & lưu địa điểm | Nền | 4 | ✅ |
| 5 | Hiển thị bản đồ theo ngày | Nền | 5 | ✅ |
| 6 | Ngân sách & chi tiêu | Nền | 5 | ✅ |
| 7 | Lấy dữ liệu ngoài có cache | Tích hợp | 6 | ✅ |
| 8 | **Chat AI — agent loop + streaming** | AI | 7–8 | ✅ |
| 9 | **Đề xuất → Duyệt → Áp dụng** | AI | 9 | ✅ |
| 10 | Sinh itinerary tự động | AI | 10 | ✅ |
| 11 | Đổi lịch theo thời tiết | AI | 10 | ⬜ |
| 12 | Tối ưu thứ tự trong ngày | AI | 10 | ⬜ |
| 13 | Rate limit & chống lạm dụng | Vận hành | 8 | ✅ |
| 14 | Ghi log tool execution | Vận hành | 7 | ✅ |
| 15 | Admin xem log & thống kê | Vận hành | 11 | ⬜ |

---

## 5. Nhóm A — Luồng nền

### Luồng 1 — Đăng ký / Đăng nhập / Refresh

```text
POST /api/auth/register
  → validate email chưa tồn tại
  → BCrypt hash password
  → tạo user (role = USER) + user_preferences rỗng
  → trả access token (15 phút) + refresh token (7 ngày)

POST /api/auth/login
  → xác thực email + password
  → phát access + refresh token
  → lưu hash của refresh token vào bảng refresh_tokens

Mọi request sau đó
  → JwtAuthenticationFilter đọc header Authorization
  → verify chữ ký + hạn
  → nạp UserDetails vào SecurityContext

Access token hết hạn
  → FE nhận 401
  → axios interceptor tự gọi POST /api/auth/refresh
  → xoay vòng refresh token (cấp mới, thu hồi cũ)
  → chạy lại request ban đầu
  → nếu refresh cũng hỏng → đăng xuất, về trang login
```

**Chú ý:** `SecurityContext` chính là nguồn `userId` cho toàn bộ hệ thống, **kể cả các tool của AI** (xem luồng 8).

### Luồng 2 — Tạo trip qua wizard

```text
FE giữ state 6 bước bằng Zustand (destination → ngày → số người
     → ngân sách → sở thích → phong cách), cho phép quay lại sửa

Bấm "Generate my trip"
  → POST /api/trips  (một request duy nhất, không lưu từng bước)
       │
       ├─ validate: end_date ≥ start_date, độ dài ≤ 30 ngày, budget > 0
       ├─ kiểm tra destination tồn tại
       ├─ [transaction]
       │    ├─ insert trips
       │    ├─ sinh itinerary_days cho từng ngày trong khoảng
       │    └─ upsert user_preferences (để AI dùng lại về sau)
       └─ trả tripId
  → FE điều hướng sang Trip Workspace
  → (từ tuần 10) kích hoạt luồng 10 — sinh itinerary tự động
```

### Luồng 3 — Xem & sửa itinerary

```text
GET /api/trips/{id}/itinerary
  → kiểm tra ownership
  → nạp days + activities + place (một truy vấn có JOIN FETCH, tránh N+1)
  → trả về đã sắp theo day_number, rồi order_index

Thêm activity
  POST /api/trips/{id}/itinerary/activities
    → validate: day thuộc trip, giờ hợp lệ, place tồn tại (nếu có)
    → order_index = max hiện tại + 1

Đổi thứ tự (kéo-thả)
  PUT /api/itinerary-days/{dayId}/reorder
    body: { activityIds: [12, 9, 31, 7] }
    → gửi CẢ danh sách, không gửi từng cái một
    → [transaction] gán lại order_index theo vị trí trong mảng
    → tránh được tình trạng thứ tự sai giữa chừng nếu request lỗi

Sửa / Xoá
  PUT|DELETE /api/activities/{id}  → kiểm tra ownership xuyên qua day → trip → user
```

### Luồng 4 — Tìm & lưu địa điểm

```text
GET /api/places?q=...&lat=...&lng=...
        │
        ├─ tạo cacheKey = hash(q + lat + lng + radius)
        ├─ Redis GET places:{cacheKey}
        │     └─ hit  → trả ngay
        │     └─ miss ↓
        ├─ PlaceSearchProvider.search()
        │     ├─ profile dev  → MockPlaceProvider (đọc file JSON)
        │     └─ profile prod → GooglePlacesProvider
        │            → field mask tối thiểu
        │            → id, displayName, location, formattedAddress,
        │              rating, priceLevel, types
        ├─ upsert vào bảng places theo (provider, external_id)
        │     → giữ external_id vĩnh viễn
        │     → các trường khác là snapshot, có fetched_at
        ├─ Redis SET TTL 1h
        └─ trả danh sách

POST /api/places/{id}/save     → thêm vào saved_places
DELETE /api/places/{id}/save   → bỏ lưu
```

### Luồng 5 — Bản đồ theo ngày

```text
Chọn ngày ở tab Map
  → lấy activities của ngày đó (đã có sẵn từ luồng 3, không gọi API mới)
  → lọc bỏ activity không có toạ độ
  → vẽ marker đánh số theo order_index
  → nối polyline theo đúng thứ tự
  → tính khoảng cách Haversine giữa các điểm liên tiếp
  → ước tính thời gian di chuyển theo tốc độ trung bình của phương tiện
  → click marker → cuộn và highlight activity tương ứng ở timeline
```

Không gọi API routing cho việc này. Chỉ gọi khi cần vẽ tuyến đường thật.

### Luồng 6 — Ngân sách & chi tiêu

```text
GET /api/trips/{id}/budget
  → tổng chi phí ước tính  = SUM(activities.estimated_cost)
  → tổng chi tiêu thực tế  = SUM(expenses.amount)
  → nhóm theo category
  → còn lại = budget − max(ước tính, thực tế)
  → cờ cảnh báo khi vượt 90% và khi vượt 100%

POST /api/trips/{id}/expenses  → ghi chi tiêu thực tế, có thể gắn activity_id
```

**Tiền lưu bằng `BIGINT` theo đơn vị nhỏ nhất** (VND: đồng, USD: cent) kèm cột `currency`. Không dùng `double` cho tiền.

---

## 6. Nhóm B — Luồng tích hợp ngoài

### Luồng 7 — Mẫu chung cho mọi API bên ngoài

Cả ba dịch vụ (thời tiết, tỷ giá, địa điểm) dùng chung một khuôn:

```text
Service nghiệp vụ
    │
    ├─ 1. Redis GET cacheKey
    │        hit → trả, kết thúc
    │
    ├─ 2. miss → WebClient gọi API ngoài
    │        ├─ timeout 5s
    │        ├─ retry 2 lần với backoff
    │        └─ circuit breaker (Resilience4j)
    │
    ├─ 3. thành công → Redis SET + TTL → trả
    │
    └─ 4. thất bại → KHÔNG ném lỗi ra người dùng
             ├─ có dữ liệu cũ trong DB/cache → trả kèm cờ stale
             └─ không có → trả rỗng kèm lý do
                  → UI hiện "chưa có dữ liệu thời tiết", trang vẫn chạy
```

**Quy tắc: API bên ngoài chết không được làm chết trang.**

Khoá cache và TTL:

```text
weather:{lat}:{lng}:{date}    TTL 3h     (dự báo)
currency:{from}:{to}          TTL 12h    (tỷ giá đổi mỗi ngày)
places:{queryHash}            TTL 1h
```

**Riêng thời tiết** có thêm một nhánh vì Open-Meteo chỉ dự báo 16 ngày:

```text
ngày cần tra − hôm nay
        │
        ├─ ≤ 16 ngày → Forecast API   → source = FORECAST
        └─ > 16 ngày → Archive API    → lấy trung bình cùng kỳ các năm trước
                                      → source = CLIMATE_NORMAL
```

Trường `source` phải đi kèm ra tận UI và tận prompt của AI. AI không được phép nói "ngày mai mưa 80%" khi con số đó thực ra là trung bình khí hậu tháng 10.

---

## 7. Nhóm C — Luồng AI

### Luồng 8 — Chat AI: agent loop + streaming

Đây là luồng trung tâm của đề tài.

```text
FE: POST /api/trips/{tripId}/ai/chat   (Accept: text/event-stream)
     body: { message, conversationId? }
        │
        ▼
AiChatController
        ├─ userId ← SecurityContext          ◄── KHÔNG lấy từ body
        ├─ kiểm tra user sở hữu tripId
        ├─ kiểm tra rate limit (luồng 13)
        └─ mở SseEmitter
        │
        ▼
ConversationService
        ├─ lấy hoặc tạo conversation
        ├─ nạp N tin nhắn gần nhất (kèm cả lượt TOOL)
        └─ lưu tin nhắn của user
        │
        ▼
AgentService  ─────────── VÒNG LẶP ───────────┐
        │                                      │
        ├─ dựng ToolContext {userId, tripId}   │  ◄── LLM KHÔNG thấy
        ├─ gọi ChatModelRouter → Gemini flash  │
        │                                      │
        ├─ LLM trả về?                         │
        │    ├─ có tool call ↓                 │
        │    │     ├─ SSE: tool_start          │
        │    │     ├─ ToolExecutor chạy tool   │
        │    │     │     → Service → Repo → DB │
        │    │     ├─ ghi ai_tool_executions   │
        │    │     ├─ SSE: tool_end (+ ms)     │
        │    │     ├─ nối kết quả vào messages │
        │    │     └─ quay lại đầu vòng  ──────┘
        │    │
        │    └─ trả lời cuối → SSE: token từng phần → done
        │
        └─ CHẶN CỨNG:
             ├─ tối đa 8 vòng lặp
             ├─ tổng thời gian ≤ 60 giây
             ├─ chặn gọi lại cùng tool với cùng tham số
             └─ quá giới hạn → dừng, trả lời bằng dữ liệu đang có
        │
        ▼
Lưu tin nhắn assistant (kèm tool_calls_json, token_usage) → đóng SSE
```

**Sự kiện SSE gửi cho FE:**

```text
event: tool_start   data: {"tool":"get_weather","label":"Đang kiểm tra thời tiết..."}
event: tool_end     data: {"tool":"get_weather","ms":320,"status":"OK"}
event: token        data: {"text":"Ngày mai "}
event: proposal     data: {"proposalId":"..."}      ← nếu có đề xuất
event: done         data: {"messageId":...}
```

Chuỗi sự kiện này vừa là UX tốt (người dùng không nhìn màn hình trắng 30 giây), vừa là **bằng chứng trực quan rằng agent thực sự gọi tool** khi bảo vệ đồ án.

**Danh mục tool:**

```text
Tool đọc — an toàn, chạy tự do
├── get_current_trip          tripId lấy từ ToolContext
├── get_itinerary
├── get_user_preferences
├── get_saved_places
├── calculate_trip_budget
├── get_weather               qua WeatherService, có cache
├── search_places             trả TỐI ĐA 5 kết quả, mỗi cái ≤ 8 trường
└── calculate_distance        Haversine, không tốn API

Tool ghi — KHÔNG ghi thẳng vào dữ liệu chuyến đi
└── propose_itinerary_changes  chỉ tạo bản đề xuất, xem luồng 9
```

**Điểm bảo mật quan trọng nhất:** `userId` và `tripId` **không nằm trong tool schema**. Chúng đi qua `ToolContext` của Spring AI, lấy từ `SecurityContext` của request. LLM không nhìn thấy và không điền được. Nếu để LLM tự điền, người dùng chỉ cần gõ *"đọc trip số 42 giúp tôi"* là đọc được dữ liệu người khác — và tên hoặc mô tả của một địa điểm lấy từ Google cũng có thể chứa prompt injection lái LLM làm điều tương tự. Ngoài ra, **mọi service vẫn kiểm tra ownership như với request REST thường**. Không có ngoại lệ cho đường đi từ AI.

### Luồng 9 — Đề xuất → Duyệt → Áp dụng

Luồng tạo nên khác biệt của đề tài. AI **không bao giờ** tự sửa lịch trình.

```text
GIAI ĐOẠN 1 — ĐỀ XUẤT  (trong agent loop)

LLM gọi propose_itinerary_changes(...)
        │
        ├─ validate từng thao tác:
        │     ├─ activity cần xoá có thuộc trip này không
        │     ├─ place cần thêm có tồn tại không
        │     └─ giờ giấc có chồng lấn nhau không
        ├─ tính chênh lệch chi phí và thời gian di chuyển
        ├─ INSERT ai_proposals
        │     status = PENDING, expires_at = +30 phút
        └─ trả proposalId cho LLM để nó diễn giải bằng lời

        ⚠️ KHÔNG đụng vào bảng activities ở bước này

GIAI ĐOẠN 2 — NGƯỜI DÙNG XEM

FE nhận event proposal → GET /api/proposals/{id}
    → hiển thị card diff:
         REMOVE  My Khe Beach — 15:00          (đỏ)
         ADD     Bảo tàng Chăm — 14:30         (xanh)
         ADD     Chợ Hàn — 16:30               (xanh)
         Chi phí:        +50.000 VND
         Di chuyển:      −18 phút
         Ngân sách sau:  7.400.000 / 8.000.000
    → [Áp dụng]  [Huỷ]

GIAI ĐOẠN 3 — ÁP DỤNG

POST /api/trips/{tripId}/ai/apply   body: { proposalId }
        │                                    ▲
        │                          chỉ có ID, KHÔNG có danh sách thay đổi
        ├─ nạp proposal từ DB
        ├─ kiểm tra: thuộc đúng trip + user
        ├─ kiểm tra: status == PENDING
        ├─ kiểm tra: chưa quá expires_at
        ├─ [transaction]
        │     ├─ áp dụng từng thao tác theo thứ tự
        │     ├─ đánh lại order_index của ngày bị ảnh hưởng
        │     └─ status = APPLIED, applied_at = now
        └─ trả itinerary mới → FE cập nhật timeline + map

Gọi lại lần hai → status đã là APPLIED → no-op, không nhân đôi dữ liệu
```

**Vì sao `apply` chỉ nhận `proposalId`:** nếu nhận cả danh sách thay đổi do client gửi lên thì bất kỳ ai cũng POST được một payload tuỳ ý, bỏ qua hoàn toàn AI lẫn mọi kiểm tra. Khi đó "user approval" chỉ còn là trang trí. Đây cũng là lý do bảng `ai_proposals` phải có — spec gốc thiếu nó.

### Luồng 10 — Sinh itinerary tự động

Chạy lâu (30–90 giây), nên phải bất đồng bộ.

```text
POST /api/trips/{id}/ai/generate
        → trả ngay 202 Accepted + jobId
        → chạy nền (@Async hoặc hàng đợi)

Tiến trình chạy nền:
   1. nạp trip + preferences + travel style
   2. get_weather cho từng ngày            (có cache)
   3. search_places theo từng nhóm sở thích (ăn uống, biển, chụp ảnh...)
   4. gọi LLM (bản pro) sinh bản nháp lịch trình có cấu trúc
   5. đối chiếu: mọi place LLM nhắc tới PHẢI khớp một place có thật
        → cái nào bịa → loại bỏ
   6. tính khoảng cách, sắp lại thứ tự trong ngày (luồng 12)
   7. tính tổng chi phí ước tính, đối chiếu ngân sách
   8. ghi vào itinerary (đây là hành động do người dùng chủ động
      yêu cầu trên một trip trống, nên ghi thẳng — khác luồng 9)

FE theo dõi qua SSE hoặc poll GET /api/trips/{id}/ai/generate/{jobId}
        → "Đang tìm địa điểm..." → "Đang sắp lịch ngày 2/4..." → xong
```

Bước 5 là bắt buộc. Không có nó, LLM sẽ bịa ra nhà hàng không tồn tại và toàn bộ giá trị của việc tích hợp Google Places biến mất.

### Luồng 11 — Đổi lịch theo thời tiết

```text
"Ngày mai trời mưa thì đổi lịch giúp tôi"
   → get_current_trip → get_itinerary → get_weather
   → nếu source == CLIMATE_NORMAL: nói rõ đây là số liệu trung bình,
     không phải dự báo
   → xác định activity ngoài trời trong ngày đó
   → search_places(loại trong nhà, gần vị trí cũ)
   → calculate_distance
   → calculate_trip_budget
   → propose_itinerary_changes   → quay về luồng 9
```

### Luồng 12 — Tối ưu thứ tự trong ngày

```text
Tool optimize_day_order(dayId)
   → lấy activity có toạ độ của ngày đó
   → giữ nguyên activity bị neo giờ (chuyến bay, đặt bàn)
   → nearest-neighbor dựng nghiệm ban đầu
   → 2-opt cải thiện
   → so tổng quãng đường trước / sau
   → nếu cải thiện < 5% → báo "đã gần tối ưu", không đề xuất gì
   → nếu đáng kể → propose_itinerary_changes  → luồng 9
```

**Thuật toán viết bằng Java, không giao cho LLM.** Một ngày hiếm khi quá 8 activity nên chạy dưới 10ms. LLM chỉ quyết định *có nên tối ưu không* và diễn giải kết quả — phần tính toán do code làm, cho kết quả ổn định và kiểm chứng được.

---

## 8. Nhóm D — Luồng vận hành

### Luồng 13 — Rate limit

Hai tầng, vì hai loại chi phí khác nhau:

```text
Tầng 1 — API thường
    Redis: rl:api:{userId}:{phút}   → 120 request / phút

Tầng 2 — AI  (đắt hơn nhiều)
    Redis: rl:ai:{userId}:{giờ}     → 20 tin nhắn / giờ
    Redis: rl:ai:{userId}:{ngày}    → 100 tin nhắn / ngày

Vượt → HTTP 429 + thông báo còn bao lâu nữa được gọi tiếp
```

Không có tầng 2 thì một vòng lặp lỗi lúc dev có thể tiêu hết quota Gemini trong vài phút.

### Luồng 14 — Ghi log tool execution

Mỗi lần chạy tool đều ghi lại, kể cả khi lỗi:

```text
ai_tool_executions
    conversation_id, message_id, trip_id, user_id
    tool_name, arguments (JSON), result (JSON, cắt bớt nếu dài)
    status: OK | ERROR | TIMEOUT
    execution_time_ms
    created_at
```

Phục vụ bốn việc: gỡ lỗi, kiểm toán, thống kê cho admin, và **chứng minh khi bảo vệ rằng agent thực sự dùng tool calling** chứ không phải LLM tự bịa câu trả lời.

### Luồng 15 — Admin

```text
GET /api/admin/tool-executions   lọc theo user, tool, trạng thái, khoảng ngày
GET /api/admin/ai-usage          số tin nhắn, số token, tool hay dùng nhất
GET /api/admin/users
GET /api/admin/trips
```

Chặn bằng `@PreAuthorize("hasRole('ADMIN')")`. Nếu trễ tiến độ thì chỉ giữ lại trang `tool-executions` — đó là trang cần cho buổi bảo vệ.

---

## 9. Bảng biến môi trường

```bash
# Database
POSTGRES_URL / POSTGRES_USER / POSTGRES_PASSWORD
REDIS_HOST / REDIS_PORT

# Auth
JWT_SECRET                 # ≥ 256 bit
JWT_ACCESS_TTL=15m
JWT_REFRESH_TTL=7d

# AI
GEMINI_API_KEY
LLM_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
LLM_MODEL_FAST             # dùng cho agent loop
LLM_MODEL_DEEP             # dùng cho sinh itinerary
LLM_FALLBACK_BASE_URL      # provider dự phòng (tuỳ chọn)
LLM_FALLBACK_API_KEY

# External
GOOGLE_PLACES_API_KEY
```

**Không commit `.env`.** Đưa `.env.example` với giá trị rỗng vào repo.

---

## 10. Những chỗ dễ sai — kiểm lại trước khi đóng mỗi tính năng

1. Tool nào cũng phải nhận `userId`/`tripId` từ `ToolContext`, không từ tham số của LLM.
2. Service phải kiểm tra ownership kể cả khi được gọi từ tool.
3. Tool trả payload gọn — nguyên response của Google Places đưa vào LLM sẽ làm context phình rất nhanh.
4. Agent loop luôn có giới hạn số vòng và timeout.
5. API bên ngoài chết không được làm chết trang.
6. Tiền dùng `BIGINT`, không dùng `double`.
7. Truy vấn itinerary phải `JOIN FETCH`, tránh N+1.
8. `apply` phải idempotent.
9. Mọi thay đổi schema đi qua Flyway, không dùng `ddl-auto: update`.
10. Cấu hình Gemini sai có thể **không báo lỗi** — tham số lạ bị bỏ qua trong im lặng. Kiểm chứng bằng response thật.
