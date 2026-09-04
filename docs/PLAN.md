# TripMind — Kế hoạch triển khai

> **Nguồn sự thật (source of truth) cho tiến độ dự án.** Spec sản phẩm nằm ở
> [TripMind_AI_Travel_Planner.md](../TripMind_AI_Travel_Planner.md). File này nói *làm gì trước, làm thế nào, và cắt gì khi trễ*.
> Tech stack chi tiết và toàn bộ luồng hệ thống: [ARCHITECTURE.md](ARCHITECTURE.md).
> Bộ tài liệu đặc tả: [ADS/README.md](ADS/README.md) — 7 tài liệu, diễn giải §12 của file này.
>
> Ràng buộc: **12 tuần · 1 người · Gemini qua Spring AI · Google Places API**

---

## 0. TL;DR

Rủi ro lớn nhất của đồ án này **không phải là AI** — mà là làm một mình một hệ full-stack (Java + React + Postgres + Redis + 4 external API + AI agent) trong 12 tuần. Spec hiện tại nếu làm đủ 100% cần khoảng 20–24 tuần cho 1 người.

Chiến lược:

1. **Tuần 1–5: dựng website chạy được, chưa có AI.** Kết thúc tuần 5 phải tạo trip → thêm activity → xem map → xem budget được bằng tay. Đây là mốc chống rủi ro quan trọng nhất: nếu AI hỏng thì vẫn còn đồ án để bảo vệ.
2. **Tuần 6–9: gắn external API rồi gắn AI agent.** Chốt ở luồng propose → approve → apply, vì đó là điểm khác biệt cốt lõi của đề tài (mục 34 trong spec).
3. **Tuần 10–12: tính năng thông minh, làm cứng, deploy, viết báo cáo.**

Mỗi tuần kết thúc bằng một thứ **demo được**, không phải một thứ "code xong chưa chạy".

---

## 1. Quyết định đã chốt

| Hạng mục | Chốt | Ghi chú |
|---|---|---|
| Backend | **Java 21 (LTS)** + **Spring Boot 4.1** | Spring Web, Security, Data JPA, Validation. Lý do chọn 21: [ARCHITECTURE.md §1.5](ARCHITECTURE.md) |
| Migration DB | **Flyway** | Spec không nhắc. Bắt buộc có — `ddl-auto: validate`, không dùng `update` |
| Frontend | React 18 + TS + Vite + Tailwind | + React Router, TanStack Query, Zustand |
| Map | Leaflet + tile OpenStreetMap | Chỉ render bản đồ |
| Dữ liệu địa điểm | **Google Places API (New)** | Text Search + Nearby Search + Place Details |
| AI | **Google Gemini** qua Spring AI (endpoint OpenAI-compatible) | Đã xác nhận hỗ trợ đủ tool calling + streaming. Đổi provider = đổi `base-url` + `api-key`. Chi tiết: [ARCHITECTURE.md §2](ARCHITECTURE.md) |
| Thời tiết | Open-Meteo | Không cần API key |
| Tỷ giá | **open.er-api.com** (không dùng Frankfurter) | Frankfurter không có VND — đã kiểm chứng, xem [R3](#r3--frankfurter-không-hỗ-trợ-vnd--đã-kiểm-chứng) |
| DB / Cache | PostgreSQL 16 + Redis 7 | Chạy qua Docker Compose ngay từ tuần 1 |
| Đóng gói | Docker Compose (postgres, redis, backend, frontend, nginx) | |
| Repo | Monorepo: `backend/`, `frontend/`, `docs/`, `docker/` | Git khởi tạo ngay hôm nay |

**Vì sao Spring AI thay vì tự viết agent loop:** Spring AI đã có sẵn vòng lặp tool-calling, parse tool call, và `ToolContext` để truyền dữ liệu tin cậy vào tool mà LLM không nhìn thấy — chính là cơ chế bảo mật ta cần ở §5. Với 12 tuần và 1 người, tự viết lại vòng lặp đó là chi phí không đáng. Trong báo cáo vẫn trình bày được đầy đủ kiến trúc agent, vì `ToolRegistry` / `ToolExecutor` / logging vẫn do ta tự viết.

> ⚠️ **Spring AI 2.0 chỉ chạy với Spring Boot 4.x**, và tên artifact đã đổi so với Spring AI 1.x. Hầu hết tutorial trên mạng còn viết cho Boot 3.x + Spring AI 1.x — bám docs chính thức, đừng copy từ blog cũ. Ghim version cứng trong `pom.xml`.

---

## 2. Năm nguyên tắc thực thi

1. **Vertical slice, không phải horizontal layer.** Làm xong hoàn toàn một tính năng từ DB → API → UI rồi mới sang tính năng kế. Không dành cả tuần 2 để viết hết mọi entity.
2. **API contract trước.** Mỗi module: viết DTO + endpoint signature + Swagger trước, rồi mới viết service. Tránh phải sửa FE vì đổi shape response.
3. **Một tuần = một thứ demo được.** Nếu cuối tuần không mở trình duyệt bấm được, tuần đó tính là trễ.
4. **AI là lớp trên cùng, không phải nền móng.** Mọi thứ AI làm phải làm được bằng tay qua REST API trước. Tool của AI chỉ gọi lại đúng service đó.
5. **Báo cáo viết song song, không dồn cuối kỳ.** Xem [§10](#10-báo-cáo-viết-song-song).

---

## 3. Phạm vi: Must / Should / Won't

### MUST — không có thì không bảo vệ được (tuần 1–9)
- Auth: register, login, JWT + refresh token, RBAC (USER/ADMIN)
- Trip CRUD + wizard 6 bước
- Itinerary: ngày, activity, sắp xếp thứ tự, sửa/xoá
- Places: search qua Google, xem chi tiết, lưu, thêm vào itinerary
- Map: marker + thứ tự + đường nối theo ngày
- Budget: tổng ngân sách, chi phí ước tính, cảnh báo vượt
- Thời tiết theo ngày của trip
- AI chat có ngữ cảnh trip + tool calling + log tool execution
- **Luồng propose → review → apply** (khác biệt cốt lõi)
- AI sinh itinerary tự động cho trip mới

### SHOULD — làm nếu đúng tiến độ (tuần 10–11)
- Weather-aware replan
- Tối ưu thứ tự activity (giảm thời gian di chuyển)
- Phân tích ngân sách bằng AI
- Expense tracking (chi tiêu thực tế vs ước tính)
- Admin: xem user, xem tool execution log, thống kê AI usage
- Rate limiting, Swagger, unit test + integration test cho service lõi

### WON'T — không làm trong đồ án này
Booking (vé máy bay, khách sạn), thanh toán, mạng xã hội, chat real-time giữa user, mobile app, RAG / vector DB, multi-agent, i18n đa ngôn ngữ, historical weather, offline mode.

> Đưa hết vào chương "Hướng phát triển" của báo cáo — đó là chỗ chúng có giá trị nhất.

---

## 4. Kiến trúc & cấu trúc repo

```
tripMind/
├── backend/                 # Spring Boot
│   └── src/main/java/com/tripmind/
│       ├── auth/            # controller, service, dto, security (JWT filter, UserDetails)
│       ├── user/            # user, user_preferences
│       ├── trip/            # trip, destination
│       ├── itinerary/       # itinerary_day, activity
│       ├── place/           # place, saved_place
│       ├── budget/          # expense, budget calculation
│       ├── ai/
│       │   ├── AgentService.java        # gọi Spring AI ChatClient
│       │   ├── ConversationService.java # lưu/nạp lịch sử hội thoại
│       │   ├── ProposalService.java     # tạo & apply proposed changes
│       │   ├── ToolExecutionLogger.java # ghi ai_tool_executions
│       │   └── tools/                   # các @Tool bean
│       ├── integration/
│       │   ├── weather/     # OpenMeteoClient
│       │   ├── places/      # PlaceSearchProvider (interface) + GooglePlacesProvider
│       │   ├── routing/     # DistanceService
│       │   └── currency/    # CurrencyClient
│       ├── admin/
│       └── common/          # exception handler, ApiResponse, config, RedisCache
├── frontend/                # React + Vite
│   └── src/
│       ├── api/             # axios client + hooks TanStack Query
│       ├── features/        # auth/ trips/ itinerary/ map/ budget/ places/ ai/
│       ├── components/ui/   # component dùng chung
│       ├── stores/          # Zustand
│       └── routes/
├── docker/                  # Dockerfile, nginx.conf
├── docs/                    # PLAN.md (file này), ADR, ERD, ảnh cho báo cáo
└── docker-compose.yml
```

**Luồng bắt buộc của tool** (không được đi tắt):

```
LLM → @Tool method → Service (đã có sẵn từ REST API) → Repository → PostgreSQL
```

Tool **không được** tự viết query riêng. Tool là một client khác của cùng service mà REST controller đang dùng. Điều này đảm bảo business rule và validation chỉ tồn tại ở một chỗ.

---

## 5. Bổ sung thiết kế DB so với spec

Spec §20 thiếu vài thứ mà đến tuần 9 sẽ chặn ta lại. Bổ sung ngay từ đầu, tránh phải viết migration đau đớn về sau.

### 5.1. Thiếu bảng `ai_proposals` — **nghiêm trọng nhất**

Spec có `POST /api/trips/{id}/ai/apply` (§23) và luồng human approval (§18), nhưng không có bảng nào lưu đề xuất. Nếu không có, endpoint `apply` buộc phải nhận danh sách thay đổi **do client gửi lên** — nghĩa là bất kỳ ai cũng có thể POST thẳng một payload tuỳ ý và bỏ qua toàn bộ AI. Lỗ hổng, và cũng làm mất ý nghĩa của "human approval".

```
ai_proposals
- id
- trip_id
- conversation_id
- message_id            -- message của AI đã sinh ra đề xuất này
- summary               -- text hiển thị cho user
- changes_json          -- danh sách thao tác: ADD/REMOVE/UPDATE/REORDER activity
- estimated_cost_delta
- status                -- PENDING | APPLIED | REJECTED | EXPIRED
- created_at
- applied_at
- expires_at            -- ví dụ 30 phút
```

`POST /ai/apply` chỉ nhận `proposalId`. Backend nạp proposal, kiểm tra ownership + status = PENDING + chưa hết hạn, rồi apply trong một transaction và đổi status = APPLIED. Apply lần hai là no-op (idempotent).

### 5.2. `activities` cần `title` và `place_id` nullable

Spec §8 có "Airport arrival", "Lunch", "Dinner" — không phải activity nào cũng gắn với một Place cụ thể. Bảng hiện tại bắt buộc `place_id`.

```
activities
+ title             -- bắt buộc, hiển thị khi không có place
+ activity_type     -- SIGHTSEEING | FOOD | TRANSPORT | ACCOMMODATION | REST | OTHER
+ created_by        -- USER | AI   (để thống kê "AI đã đóng góp bao nhiêu %")
~ place_id          -- cho phép NULL
```

### 5.3. `messages` chưa lưu được tool call

Vòng lặp agent cần dựng lại đúng lịch sử hội thoại ở lượt sau, bao gồm cả các lượt tool. Với `role` + `content` không đủ.

```
messages
~ role              -- USER | ASSISTANT | TOOL | SYSTEM
+ tool_calls_json   -- khi assistant yêu cầu gọi tool
+ tool_call_id      -- khi role = TOOL
+ token_usage       -- để thống kê AI usage cho admin
```

### 5.4. Bổ sung nhỏ khác

- `refresh_tokens` (id, user_id, token_hash, expires_at, revoked_at) — hoặc lưu Redis. Spec ghi "refresh token nếu cần", nhưng có RBAC + JWT thì cần thật.
- `ai_tool_executions` thêm `trip_id` và `user_id` — để admin lọc và để rate-limit theo user.
- `expenses` thêm `activity_id` (nullable) — nối chi tiêu thực tế với activity đã lên kế hoạch.
- **Chốt định nghĩa "72% planned"** (Dashboard §5) ngay từ đầu, vì nó xuất hiện ở nhiều màn hình. Đề xuất: `số ngày có ≥ 2 activity / tổng số ngày`. Tính ở service, không lưu cột.
- Tiền tệ: lưu số tiền dạng **`BIGINT` đơn vị nhỏ nhất** (VND: đồng; USD: cent) + cột `currency`. Đừng dùng `double` cho tiền.

---

## 6. Rủi ro kỹ thuật cần xử lý trước khi code

Đây là những chỗ tôi kiểm tra spec và thấy sẽ vỡ khi chạy thật. Xử lý sớm, đừng đợi phát hiện ở tuần 10.

### R1 — Open-Meteo chỉ dự báo tối đa 16 ngày — ✅ đã kiểm chứng

Gọi thử API ngày 03/09/2026 với `forecast_days=30`, Open-Meteo trả về:

```json
{"reason":"Forecast days is invalid. Allowed range 0 to 16.","error":true}
```

Người dùng lập kế hoạch cho chuyến đi 2 tháng nữa sẽ **không có dữ liệu thời tiết**. Toàn bộ tính năng weather-aware planning sẽ trả về rỗng ở đúng kịch bản phổ biến nhất — người ta thường lên kế hoạch du lịch sớm hơn 16 ngày.

**Xử lý:** `WeatherService` trả về kèm `source`:
- Trong 16 ngày → `FORECAST` (dự báo thật)
- Ngoài 16 ngày → `CLIMATE_NORMAL` (trung bình lịch sử cùng thời điểm, lấy từ Open-Meteo Archive API)

UI và system prompt của AI đều phải phân biệt hai loại này. AI không được nói "ngày mai mưa 80%" khi thực ra đó là trung bình khí hậu tháng 10. Trong kịch bản demo (§41), đặt ngày trip **trong vòng 16 ngày** để dùng dự báo thật.

### R2 — Google Places: chi phí và giới hạn cache
Places API (New) tính tiền theo SKU và theo *field mask* — xin càng nhiều trường càng đắt. Đồng thời điều khoản của Google giới hạn việc cache nội dung place (`place_id` được lưu lâu dài, các trường khác thì không).

**Xử lý:**
- **Ngày đầu tiên: mở trang pricing hiện hành của Google Places API và đọc kỹ.** Mô hình free tier của Google đã thay đổi trong thời gian gần đây — đừng giả định là "$200 credit mỗi tháng".
- Đặt **budget alert + quota cap** trong Google Cloud Console trước khi viết dòng code gọi API đầu tiên. Một vòng lặp agent bị lỗi có thể đốt hết hạn mức trong vài phút.
- Luôn dùng field mask tối thiểu. Search chỉ lấy: id, displayName, location, formattedAddress, rating, priceLevel, types.
- Cache Redis theo `places:{hash(query+lat+lng+radius)}` TTL ngắn (~1h) để chống lặp trong lúc dev.
- Bảng `places` lưu `external_id` + `provider` vĩnh viễn; các trường chi tiết coi là snapshot có `fetched_at`.
- Hiển thị attribution "Powered by Google" ở màn hình có dữ liệu Places.
- **Chuẩn bị sẵn `MockPlaceProvider`** đọc từ file JSON, bật bằng Spring profile `dev`. Dev hằng ngày không nên đốt quota thật.

### R3 — Frankfurter **không** hỗ trợ VND — ✅ đã kiểm chứng

Spec §28 và §40 khuyến nghị Frankfurter. Nhưng Frankfurter lấy tỷ giá tham chiếu của ECB, và danh sách đó **không có VND**:

```
AUD BRL CAD CHF CNY CZK DKK EUR GBP HKD HUF IDR ILS INR ISK JPY KRW
MXN MYR NOK NZD PHP PLN RON SEK SGD THB TRY USD ZAR      → 30 đồng tiền, không có VND
```

Trong khi đó VND là đơn vị tiền mặc định của toàn bộ app (ngân sách 8.000.000 VND trong mọi ví dụ của spec). Dùng Frankfurter thì tính năng đổi tiền vô dụng ở đúng thị trường chính.

**Xử lý — đã chốt: dùng `open.er-api.com` thay thế.** Đã gọi thử: miễn phí, không cần API key, 166 đồng tiền, có VND (`1 USD ≈ 26.015 VND`), cập nhật mỗi ngày.

```
GET https://open.er-api.com/v6/latest/USD  →  { "result": "success", "rates": { "VND": 26015.67, ... } }
```

Vẫn giữ `CurrencyProvider` là interface — nếu provider này chết thì đổi chỉ tốn một class. Cache Redis TTL 12 giờ (tỷ giá chỉ đổi mỗi ngày một lần, không cần gọi thường xuyên).

> Đây là một điểm đáng viết vào báo cáo: *"khuyến nghị ban đầu không thoả mãn yêu cầu thị trường mục tiêu, đã đánh giá và thay thế provider"* — cho thấy có kiểm chứng chứ không copy nguyên spec.

### R4 — Chi phí và độ trễ của agent loop
Mỗi lượt chat có thể gọi 5–7 tool, mất 15–40 giây. Người dùng nhìn vào màn hình trắng sẽ tưởng hỏng. Và một tool trả về JSON 50KB sẽ làm context phình rất nhanh.

**Xử lý:**
- Endpoint chat dùng **SSE (Server-Sent Events)** thay vì POST-rồi-đợi. Stream ra từng bước: `tool_start` → `tool_end` → `token`. UI hiện "Đang kiểm tra thời tiết..." đúng như mô hình §19 của spec — vừa đẹp vừa chứng minh được tool calling khi bảo vệ.
- **Giới hạn cứng cho agent loop:** tối đa 8 vòng lặp, timeout 60s, chặn lặp lại cùng một tool với cùng tham số.
- **Mọi tool phải trả về payload gọn.** `search_places` trả tối đa 5 kết quả, mỗi kết quả ≤ 8 trường. Đừng đưa nguyên response của Google vào LLM.
- Rate limit theo user: ví dụ 20 tin nhắn AI / giờ, đếm bằng Redis.

### R5 — LLM không được tự chọn `trip_id` — **rủi ro bảo mật chính**
Nếu tool khai báo tham số `tripId` để LLM điền, người dùng chỉ cần gõ *"đọc trip số 42 giúp tôi"* là LLM ngoan ngoãn gọi `get_trip(42)` — trip của người khác. Prompt injection từ dữ liệu bên ngoài (ví dụ tên/review của một địa điểm) cũng có thể lái LLM làm điều tương tự.

**Xử lý — hai lớp:**
1. `tripId` và `userId` **không nằm trong tool schema**. Chúng được truyền qua `ToolContext` của Spring AI, lấy từ SecurityContext của request. LLM không nhìn thấy và không điền được.
2. Kể cả vậy, **mọi service vẫn kiểm tra ownership** như với request REST thường. Không có ngoại lệ cho đường đi từ AI.

Đây là điểm đáng viết hẳn một mục trong chương Thiết kế và chương Kiểm thử của báo cáo — nó phân biệt "gắn chatbot vào web" với "thiết kế agent có kiểm soát".

### R6 — Đừng bắt LLM làm bài toán tối ưu
"Tối ưu thứ tự activity để giảm thời gian di chuyển" là bài toán TSP có ràng buộc thời gian. LLM giải bằng cách đoán, kết quả không ổn định và không thể kiểm chứng.

**Xử lý:** viết thuật toán trong Java (một ngày hiếm khi quá 8 activity → nearest-neighbor + 2-opt là quá đủ, chạy dưới 10ms). Expose thành tool `optimize_day_order`. LLM chỉ quyết định *có nên tối ưu không* và *diễn giải kết quả*; phần tính toán do code làm. Có thuật toán thật để viết vào báo cáo cũng tốt hơn nhiều so với "LLM tự sắp xếp".

Cho khoảng cách: dùng **Haversine + tốc độ trung bình** cho vòng lặp tối ưu (miễn phí, tức thời), chỉ gọi API routing thật để hiển thị tuyến đường cuối cùng trên map.

---

## 7. Lộ trình 12 tuần

Giả định ~20–25 giờ/tuần. Mỗi tuần có một **Demo** — thứ phải bấm được trên trình duyệt vào cuối tuần.

### Tuần 1 — Nền móng + Auth
- `git init`, tạo monorepo, `.gitignore`, README
- `docker-compose.yml`: postgres + redis. Spring Boot skeleton + Flyway migration V1 (users, refresh_tokens)
- JWT: register / login / refresh / logout, `JwtAuthenticationFilter`, RBAC `USER`/`ADMIN`
- `GlobalExceptionHandler` + wrapper response chuẩn + Swagger bật sẵn
- Frontend: Vite + TS + Tailwind + Router, trang Login/Register, axios interceptor tự refresh token, protected route
- **Xác minh ngay:** Spring AI version + Google Places pricing + Frankfurter có VND không (R1/R2/R3)
- **Demo:** đăng ký → đăng nhập → vào trang trống có tên mình

### Tuần 2 — Trip + Destination
- Migration V2: destinations, trips, user_preferences. Seed ~15 destination Việt Nam + châu Á
- Trip CRUD đầy đủ + kiểm tra ownership + validate ngày (end ≥ start, độ dài ≤ 30 ngày)
- Wizard tạo trip 6 bước ở FE (state bằng Zustand, có thể quay lại bước trước)
- Dashboard: danh sách upcoming / past trip, hiển thị `% planned`
- **Demo:** tạo trip qua wizard, thấy nó xuất hiện ở dashboard

### Tuần 3 — Itinerary
- Migration V3: itinerary_days, activities (có `title`, `activity_type`, `created_by`, `place_id` nullable)
- Tạo trip → tự sinh itinerary_days theo khoảng ngày
- Activity CRUD + `reorder` endpoint (nhận cả danh sách id, không sửa từng cái)
- FE: timeline theo ngày, thêm/sửa/xoá activity, kéo-thả đổi thứ tự (dnd-kit)
- **Demo:** dựng thủ công một lịch trình 3 ngày hoàn chỉnh

### Tuần 4 — Places (Google) + Saved Places
- `PlaceSearchProvider` interface + `GooglePlacesProvider` + `MockPlaceProvider` (profile dev)
- Search / detail, upsert vào bảng `places`, cache Redis
- Saved places: lưu / bỏ lưu / danh sách
- FE: modal tìm địa điểm → xem chi tiết → thêm thẳng vào một ngày của itinerary
- **Demo:** tìm "bún chả Hà Nội" → thêm vào Day 2 → thấy trên timeline

### Tuần 5 — Map + Budget · 🏁 **MILESTONE 1**
- Leaflet: marker theo ngày, đánh số thứ tự, đường nối, click marker → highlight activity
- `DistanceService` (Haversine) + hiển thị khoảng cách/thời gian giữa các điểm
- Budget: ngân sách tổng, tổng chi phí ước tính theo hạng mục, thanh cảnh báo vượt
- Expenses CRUD cơ bản
- **Demo:** 🏁 **Website đầy đủ chức năng, không có AI.** Đây là mốc an toàn — đến đây đã đủ để bảo vệ một đồ án web bình thường.

### Tuần 6 — External APIs
- `OpenMeteoClient` + `WeatherService` có phân biệt `FORECAST` / `CLIMATE_NORMAL` (R1)
- Cache Redis cho weather + currency, có TTL, có fallback khi API lỗi
- `CurrencyProvider` (hoặc bỏ nếu R3 xác nhận không khả thi)
- FE: widget thời tiết trên Overview và trên mỗi ngày của itinerary
- **Demo:** trip Đà Nẵng hiện thời tiết từng ngày; ngắt mạng vẫn không sập trang

### Tuần 7 — Nền tảng AI + 3 tool đọc
- Migration V4: conversations, messages (có tool_calls_json), ai_tool_executions
- Cấu hình Spring AI, `AgentService`, system prompt v1 (nhét sẵn ngữ cảnh trip)
- 3 tool chỉ đọc: `get_current_trip`, `get_itinerary`, `get_weather` — **dùng ToolContext, không cho LLM điền tripId (R5)**
- `ToolExecutionLogger` ghi lại mọi lần gọi: tên tool, tham số, kết quả, thời gian
- Endpoint chat dạng **SSE**; FE: sidebar chat có streaming
- **Demo:** hỏi "lịch trình ngày 2 của tôi có gì?" → AI trả lời đúng dữ liệu thật trong DB

### Tuần 8 — Bộ tool đầy đủ
- Thêm: `search_places`, `calculate_distance`, `calculate_trip_budget`, `get_saved_places`, `get_user_preferences`
- Giới hạn agent loop: max 8 vòng, timeout, chống gọi lặp (R4)
- Tinh chỉnh system prompt: nói tiếng Việt, ngắn gọn, không bịa địa điểm, luôn nêu rõ nguồn số liệu
- FE: timeline hiển thị tool đang chạy (giống §19 spec) — vừa là UX vừa là bằng chứng khi bảo vệ
- Rate limit AI theo user bằng Redis
- **Demo:** "tìm quán ăn gần khách sạn, giá vừa phải" → AI gọi 2–3 tool, trả gợi ý thật

### Tuần 9 — Propose / Approve / Apply · 🏁 **MILESTONE 2**
- Migration V5: `ai_proposals`
- Tool `propose_itinerary_changes` — **chỉ ghi vào bảng proposal, không đụng activities**
- `POST /api/trips/{id}/ai/apply` nhận `proposalId`, kiểm ownership + status + hạn, apply trong transaction, idempotent
- FE: card diff (REMOVE đỏ / ADD xanh), chênh lệch chi phí và thời gian di chuyển, nút Apply / Cancel
- **Demo:** 🏁 **Điểm khác biệt cốt lõi chạy được.** "Đổi lịch ngày mai" → AI đề xuất → bấm Apply → DB đổi thật.

### Tuần 10 — Tính năng thông minh
- **Sinh itinerary tự động** cho trip mới (chạy nền, có trạng thái tiến trình ở FE) — đây là "wow moment" của demo
- Weather-aware replan (dùng lại luồng proposal của tuần 9)
- `optimize_day_order`: nearest-neighbor + 2-opt viết bằng Java (R6)
- Phân tích ngân sách bằng AI
- **Demo:** wizard → "Generate my trip" → 4 ngày lịch trình đầy đủ có địa điểm thật

### Tuần 11 — Làm cứng + Admin + Test
- Admin (tối giản, đúng nhu cầu bảo vệ): danh sách user, danh sách trip, **bảng tool execution log**, thống kê AI usage
- Test: JUnit cho budget calculation + optimizer + ProposalService; Testcontainers cho repository; test bảo mật — user A không đọc được trip của user B **kể cả qua đường AI**
- Swagger hoàn chỉnh, validate input toàn bộ, chuẩn hoá thông báo lỗi
- FE polish: loading skeleton, empty state, xử lý lỗi, responsive
- **Demo:** đăng nhập admin → xem toàn bộ lịch sử AI đã gọi tool nào

### Tuần 12 — Deploy + Demo + Báo cáo
- Dockerfile cho FE/BE, Nginx reverse proxy, `docker-compose` chạy một lệnh
- Deploy lên VPS, cấu hình biến môi trường, HTTPS
- **Seed dữ liệu demo** và **tập dượt kịch bản §41 ít nhất 3 lần**
- Chụp ảnh màn hình, vẽ sơ đồ, hoàn thiện báo cáo + slide
- Dự phòng cho việc phát sinh (luôn có)

---

## 8. Milestone & Definition of Done

| Mốc | Tuần | Điều kiện hoàn thành |
|---|---|---|
| **M1 — Website không AI** | 5 | Tạo trip → thêm activity → xem map → xem budget. Không lỗi console. Không cần AI. |
| **M2 — Agent có approval** | 9 | Chat gọi ≥3 tool, sinh proposal, apply thay đổi DB, có log tool execution đầy đủ. |
| **M3 — Sẵn sàng bảo vệ** | 12 | Chạy được trên VPS, kịch bản demo trôi 3 lần liên tiếp, báo cáo xong bản nháp. |

**DoD cho mỗi tính năng** (không có ngoại lệ):
- [ ] Có migration Flyway, không dùng `ddl-auto: update`
- [ ] Có validate input + kiểm tra ownership
- [ ] Có ghi vào Swagger
- [ ] FE có xử lý loading / error / empty
- [ ] Đã test bằng tay ít nhất một luồng lỗi (mạng hỏng, dữ liệu rỗng, không có quyền)
- [ ] Đã commit với message rõ ràng

---

## 9. Kế hoạch cắt scope khi trễ

Trễ là chuyện bình thường khi làm một mình. Quan trọng là **biết trước sẽ cắt gì**, để không hoảng ở tuần 10. Cắt theo thứ tự này:

1. Currency / đổi tiền (nếu R3 rắc rối, cắt ngay từ tuần 1)
2. Expense tracking chi tiết → chỉ giữ chi phí ước tính
3. Admin dashboard → **chỉ giữ trang tool execution log** (trang này cần cho bảo vệ, phần còn lại thì không)
4. Tối ưu thứ tự activity → chuyển sang Hướng phát triển
5. Testcontainers → chỉ giữ unit test cho service lõi
6. Deploy VPS → demo trên localhost qua Docker Compose

**Tuyệt đối không cắt:** luồng propose → approve → apply (tuần 9), tool execution log, và sinh itinerary tự động. Ba thứ này *là* đề tài. Không có chúng thì đây chỉ là một website du lịch có gắn chatbot.

---

## 10. Báo cáo: viết song song

| Sau tuần | Viết chương |
|---|---|
| 2 | Ch.1 Tổng quan, Ch.2 Công nghệ — viết được ngay, không cần chờ code |
| 3 | Ch.3 Phân tích: use case, functional/non-functional requirement |
| 5 | Ch.4 Thiết kế: kiến trúc, ERD (xuất từ DB thật), API |
| 9 | Ch.4 phần AI Agent + Tool architecture + Security (dùng lại §5, §6 của file này) |
| 11 | Ch.5 Triển khai, Ch.6 Kiểm thử |
| 12 | Ch.7 Kết quả, Ch.8 Hạn chế & hướng phát triển |

**Chụp ảnh màn hình ngay khi làm xong mỗi tính năng**, đừng đợi tuần 12 — lúc đó dữ liệu demo đã khác và giao diện đã đổi.

Mẹo: mục §5 và §6 của file này (những vấn đề phát hiện trong thiết kế và cách xử lý) là chất liệu tốt cho phần "phân tích thiết kế" — nó cho thấy quá trình suy nghĩ, không chỉ kết quả.

---

## 11. Việc cần làm ngay hôm nay

Theo đúng thứ tự, làm hết trong buổi đầu:

1. `git init` + tạo cấu trúc thư mục ở §4 + commit đầu tiên
2. Lấy **Gemini API key** ở AI Studio + xem trang rate limit của tài khoản. Mở docs Spring AI bản mới nhất → ghi lại tên artifact starter, cú pháp `@Tool`, cách dùng `ToolContext` → lưu vào `docs/ADR-001-spring-ai.md`
3. Tạo Google Cloud project → bật Places API → **đặt quota cap và budget alert** → gọi thử một request Text Search bằng `curl`, xem response và giá của field mask
4. ~~Kiểm tra Frankfurter có VND~~ — ✅ đã làm, **không có**, chốt dùng `open.er-api.com` (R3)
5. ~~Kiểm tra giới hạn dự báo Open-Meteo~~ — ✅ đã làm, **tối đa 16 ngày** (R1)
6. `docker compose up` với postgres + redis, kết nối được từ máy
7. Khởi tạo Spring Boot project (Spring Initializr: Web, Security, JPA, Validation, Flyway, Redis, Actuator) + `npm create vite@latest`

Xong bước 7 là tuần 1 đã bắt đầu đúng hướng.

---

## 12. Quyết định kiến trúc, bất biến và luật nghiệp vụ

> **Đây là nguồn chân lý.** Bộ `docs/ADS/` diễn giải mục này chứ không định nghĩa lại. Sửa quyết định thì sửa ở đây trước, rồi mới cập nhật ADS.

### 12.1 Mười hai quyết định kiến trúc — `QĐ`

Không đảo trong lúc làm. Cột cuối là cái giá phải trả nếu đảo.

| Mã | Quyết định | Hệ quả nặng nhất nếu đảo |
|---|---|---|
| `QĐ-01` | **AI không ghi thẳng vào lịch trình.** Mọi thay đổi đi qua bản đề xuất được người duyệt | Mất toàn bộ điểm khác biệt của đề tài; hệ thống thành chatbot có quyền ghi |
| `QĐ-02` | **LLM không chạm cơ sở dữ liệu.** Chỉ gọi tool; tool gọi dịch vụ | Luật nghiệp vụ tồn tại ở hai chỗ, lệch nhau lúc nào không biết |
| `QĐ-03` | **Danh tính không nằm trong lược đồ tool.** `userId`/`tripId` truyền qua `ToolContext` | Người dùng đọc được dữ liệu người khác chỉ bằng một câu tiếng Việt |
| `QĐ-04` | Đi qua **lớp tương thích OpenAI**, không dùng SDK riêng của nhà cung cấp | Khoá chặt vào một nhà cung cấp; đổi model phải viết lại tầng AI |
| `QĐ-05` | Luật nghiệp vụ đặt ở **tầng dịch vụ**, dùng chung cho REST và tool | Đường đi từ AI bỏ qua kiểm tra mà đường REST vẫn có |
| `QĐ-06` | Vòng lặp agent có **trần cứng**: 8 vòng, 60 giây | Một câu hỏi lỗi đốt hết hạn ngạch và treo kết nối |
| `QĐ-07` | Thời tiết ngoài 16 ngày là **trung bình khí hậu**, phải gắn nhãn nguồn | AI nói "ngày mai mưa 80%" cho một con số không phải dự báo |
| `QĐ-08` | Địa điểm phải **khớp một bản ghi có thật**. LLM không được tạo địa điểm | AI bịa nhà hàng; việc tích hợp Google Places mất hết giá trị |
| `QĐ-09` | Bài toán tối ưu **giải bằng mã**, không giao cho LLM | Kết quả không ổn định, không tái lập, không kiểm chứng được |
| `QĐ-10` | Tiền lưu **số nguyên theo đơn vị nhỏ nhất**, kèm mã tiền tệ | Sai số cộng dồn; đối chiếu ngân sách lệch không giải thích được |
| `QĐ-11` | **API bên ngoài hỏng không làm hỏng trang.** Luôn có đường lui | Một dịch vụ miễn phí chết là cả ứng dụng trắng màn hình |
| `QĐ-12` | Mọi thay đổi lược đồ đi qua **Flyway** | Không tái lập được cơ sở dữ liệu; máy dev và máy chủ lệch nhau |

### 12.2 Mười hai bất biến cưỡng chế ở tầng cơ sở dữ liệu — `DI`

Cưỡng chế bằng ràng buộc, không bằng mã ứng dụng. Mã ứng dụng có thể quên; cơ sở dữ liệu thì không.

| Mã | Bất biến | Cưỡng chế bằng |
|---|---|---|
| `DI-1` | Mỗi chuyến đi thuộc **đúng một** người dùng | `FK NOT NULL` |
| `DI-2` | Trong một chuyến, mỗi số thứ tự ngày xuất hiện **một lần** | `UNIQUE (trip_id, day_number)` |
| `DI-3` | Trong một ngày, mỗi vị trí sắp xếp xuất hiện **một lần** | `UNIQUE (itinerary_day_id, order_index)` |
| `DI-4` | Hoạt động luôn có tên; **địa điểm thì không bắt buộc** | `title NOT NULL`, `place_id NULL` cho phép |
| `DI-5` | Trạng thái đề xuất thuộc tập cố định | `CHECK status IN (PENDING, APPLIED, REJECTED, EXPIRED)` |
| `DI-6` | Một đề xuất thuộc **đúng một** chuyến đi | `FK NOT NULL` |
| `DI-7` | Một người **không lưu trùng** một địa điểm | `UNIQUE (user_id, place_id)` |
| `DI-8` | Một địa điểm ngoài chỉ có **một bản ghi** trong hệ thống | `UNIQUE (provider, external_id)` |
| `DI-9` | Ngày kết thúc **không trước** ngày bắt đầu | `CHECK end_date >= start_date` |
| `DI-10` | Mọi khoản tiền **không âm** | `CHECK amount >= 0` |
| `DI-11` | Mỗi phiếu làm mới chỉ tồn tại **một bản** | `UNIQUE (token_hash)` |
| `DI-12` | Vai trò tin nhắn thuộc tập cố định | `CHECK role IN (USER, ASSISTANT, TOOL, SYSTEM)` |

### 12.3 Hai mươi tám luật tầng dịch vụ — `BR`

Cơ sở dữ liệu không kiểm được. Mỗi luật ứng một ca kiểm thử.

**Xác thực và quyền — `BR-0xx`**

| Mã | Luật |
|---|---|
| `BR-001` | Đăng ký từ chối email đã tồn tại |
| `BR-002` | Mật khẩu băm bằng BCrypt, không bao giờ ghi ra nhật ký |
| `BR-003` | Phiếu làm mới xoay vòng: cấp mới thì thu hồi cũ |
| `BR-004` | Phiếu làm mới đã thu hồi mà bị dùng lại thì thu hồi **cả họ** phiếu của người đó |
| `BR-005` | Một tài khoản mang **một** vai trò |
| `BR-006` | Điểm cuối quản trị từ chối mọi vai trò khác `ADMIN` |

**Sở hữu — `BR-1xx`**

| Mã | Luật |
|---|---|
| `BR-101` | Mọi thao tác trên chuyến đi kiểm tra người gọi là chủ sở hữu |
| `BR-102` | Kiểm tra sở hữu hoạt động truy ngược qua ngày → chuyến đi → người dùng |
| `BR-103` | **Luật này áp dụng y hệt khi lời gọi đến từ tool của AI.** Không có ngoại lệ |
| `BR-104` | Không tìm thấy và không có quyền trả **cùng một mã lỗi**, để không lộ sự tồn tại của dữ liệu |

**Chuyến đi và lịch trình — `BR-2xx`**

| Mã | Luật |
|---|---|
| `BR-201` | Độ dài chuyến đi tối đa 30 ngày |
| `BR-202` | Tạo chuyến đi sinh đủ số ngày trong khoảng, trong **cùng một giao dịch** |
| `BR-203` | Đổi ngày của chuyến đi phải sinh thêm hoặc xoá bớt ngày tương ứng |
| `BR-204` | Sắp lại thứ tự nhận **cả danh sách**, không nhận từng phần tử |
| `BR-205` | Danh sách sắp lại phải chứa đúng tập hoạt động của ngày đó, không thừa không thiếu |
| `BR-206` | Giờ kết thúc không trước giờ bắt đầu |
| `BR-207` | Tiến độ lập kế hoạch = số ngày có từ 2 hoạt động trở lên, chia tổng số ngày |

**Địa điểm — `BR-3xx`**

| Mã | Luật |
|---|---|
| `BR-301` | Kết quả tìm kiếm ghi vào bảng địa điểm theo cặp *(nhà cung cấp, mã ngoài)* |
| `BR-302` | Mã ngoài giữ vĩnh viễn; các trường còn lại là ảnh chụp có mốc thời gian |
| `BR-303` | Hồ sơ chạy `dev` dùng nhà cung cấp giả, không gọi dịch vụ tính phí |
| `BR-304` | Bỏ lưu địa điểm không xoá bản ghi địa điểm |

**Ngân sách — `BR-4xx`**

| Mã | Luật |
|---|---|
| `BR-401` | Cộng tiền chỉ cộng các khoản **cùng mã tiền tệ** |
| `BR-402` | Chi phí ước tính và chi tiêu thực tế là hai đại lượng tách rời, không cộng vào nhau |
| `BR-403` | Cảnh báo ở hai mốc: vượt 90% và vượt 100% ngân sách |

**AI — `BR-5xx`**

| Mã | Luật |
|---|---|
| `BR-501` | Tool nhận `userId`/`tripId` từ ngữ cảnh, **không** từ tham số do mô hình sinh |
| `BR-502` | Vòng lặp agent dừng ở vòng thứ 8 hoặc giây thứ 60, tuỳ cái nào đến trước |
| `BR-503` | Chặn gọi lại cùng một tool với cùng bộ tham số trong một lượt |
| `BR-504` | `search_places` trả tối đa 5 kết quả, mỗi kết quả tối đa 8 trường |
| `BR-505` | Ghi nhật ký mọi lần chạy tool, **kể cả khi lỗi hoặc quá hạn** |
| `BR-506` | Áp dụng đề xuất chỉ nhận mã đề xuất, **không** nhận danh sách thay đổi từ máy khách |
| `BR-507` | Áp dụng kiểm đủ bốn điều: đúng chuyến, đúng người, trạng thái chờ, chưa quá hạn |
| `BR-508` | Áp dụng lần thứ hai là **vô hiệu**, không nhân đôi dữ liệu |
| `BR-509` | Sinh lịch trình tự động loại bỏ mọi địa điểm không khớp bản ghi có thật |
| `BR-510` | Dữ liệu thời tiết đưa vào mô hình luôn kèm nhãn nguồn |

**Vận hành — `BR-6xx`**

| Mã | Luật |
|---|---|
| `BR-601` | Giới hạn tần suất hai tầng: chung cho API, riêng và chặt hơn cho AI |
| `BR-602` | Dịch vụ ngoài lỗi thì trả dữ liệu cũ kèm cờ, hoặc trả rỗng kèm lý do — không ném lỗi ra người dùng |
