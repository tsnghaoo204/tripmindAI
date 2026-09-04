# BE-TODO — Kế hoạch dựng backend TripMind

**v0.1 · 04/09/2026** · Chỉ nói về **backend**. Frontend có lộ trình riêng ở [`PLAN.md`](PLAN.md) §7.

> Nguồn: [`PLAN.md`](PLAN.md) §12 (nguồn chân lý) · [`ARCHITECTURE.md`](ARCHITECTURE.md) · [`ADS-10`](ADS/ADS-10-Kien-truc-phan-mem.md) · [`ADS-20`](ADS/ADS-20-Thiet-ke-CSDL.md) · [`ADS-21`](ADS/ADS-21-Tich-hop-AI-Agent.md) · [`ADS-30`](ADS/ADS-30-Hop-dong-API.md)
>
> File này **không định nghĩa** luật mới. Mọi mục đều truy ngược về mã `QĐ` / `DI` / `BR` / `FR`. Thấy lệch thì sửa `PLAN.md` §12 trước.

---

## 0. Cách dùng

- Chín gói `BE-0` → `BE-8`, xếp theo **thứ tự phụ thuộc**, không phải theo tầng. Làm xong hẳn một gói rồi sang gói kế.
- Mỗi việc có mã `BE-x.y` để commit tham chiếu: `feat(BE-2.3): reorder activities`.
- Mỗi gói có **Xong khi** — điều kiện nghiệm thu bằng `curl`, không phải "đã viết code".
- Cột `Luật` là mã phải được cưỡng chế bởi việc đó. Không có mã = việc hạ tầng.

**DoD chung cho mọi việc** (từ `PLAN.md` §8, bỏ phần FE):

- [ ] Có migration Flyway nếu đụng lược đồ — không `ddl-auto: update` (`QĐ-12`)
- [ ] Có validate đầu vào + kiểm sở hữu ở **tầng dịch vụ** (`BR-101`, `QĐ-05`)
- [ ] Có mô tả OpenAPI (`NFR-08`)
- [ ] Đã thử tay ít nhất một luồng lỗi: không quyền · dữ liệu rỗng · dịch vụ ngoài chết
- [ ] Commit với message rõ ràng

---

## 1. Sáu điều phải chốt trước khi gõ dòng code đầu

Đây là chỗ các tài liệu **nói khác nhau**. Không chốt thì đến `V3` sẽ phải viết lại migration.

| # | Chỗ lệch | Bên A | Bên B | Đề xuất chốt |
|---|---|---|---|---|
| `Q1` | Giá trị `ai_proposals.status` | `ADS-20` §2.13: `CHO_DUYET` · `DA_AP_DUNG` · `BI_TU_CHOI` · `HET_HAN` | `PLAN` §5.1 + §12.2 `DI-5` + `ARCHITECTURE` §7: `PENDING` · `APPLIED` · `REJECTED` · `EXPIRED` | **Chọn tiếng Anh.** `PENDING` · `APPLIED` · `REJECTED` · `EXPIRED`. Chuẩn hóa toàn bộ enum trong DB, API payload và mã nguồn backend sang tiếng Anh chuẩn công nghiệp. UI chịu trách nhiệm hiển thị i18n/tiếng Việt. **Cập nhật lại ADS-20, ADS-30 sang tiếng Anh**. |
| `Q2` | Giá trị `activities.activity_type` | `ADS-20` §2.7: `THAM_QUAN` · `AN_UONG` · `DI_LAI` · `LUU_TRU` · `NGHI` · `KHAC` | `PLAN` §5.2: `SIGHTSEEING` · `FOOD` · `TRANSPORT` · `ACCOMMODATION` · `REST` · `OTHER` | **Chọn tiếng Anh.** `SIGHTSEEING` · `FOOD` · `TRANSPORT` · `ACCOMMODATION` · `REST` · `OTHER`. Cập nhật lại ADS-20, ADS-30 sang tiếng Anh. |
| `Q3` | Nhãn nguồn thời tiết | `ADS-30` §8: `DU_BAO` · `TRUNG_BINH_KHI_HAU` | `ARCHITECTURE` §6 + `PLAN` §6 R1: `FORECAST` · `CLIMATE_NORMAL` | **Chọn tiếng Anh.** `FORECAST` · `CLIMATE_NORMAL`. Trả về qua API payload bằng tiếng Anh, UI hiển thị nhãn thân thiện tương ứng. |
| `Q4` | Áp dụng đề xuất lần thứ hai | `ARCHITECTURE` §7 luồng 9: "no-op, không nhân đôi" (ngụ ý `200`) | `ADS-30` §10.2: `409 PROPOSAL_NOT_PENDING` | **`409`.** Hợp đồng API thắng. Vẫn thoả `BR-508` — "vô hiệu" không bắt buộc phải `200` |
| `Q5` | **Thứ tự khoá ngoại `activities.place_id` → `places`** | `PLAN` §7: tuần 3 làm `activities`, tuần 4 mới làm `places` | `ADS-20` §2.7: `place_id` FK → `places` `RESTRICT` | **Tạo `places` + `saved_places` sớm hơn một bậc** (xem §2). Bảng rỗng không tốn gì; tách FK ra migration sau thì tốn một file `ALTER` và dễ quên |
| `Q6` | Đánh số migration | `PLAN` §7 chỉ đặt số cho `V1`–`V5`; `places`, `saved_places`, `expenses` **không có số** | — | Dùng dãy `V1`–`V7` ở §2 bên dưới |

> Các enum kỹ thuật thống nhất 100% bằng **tiếng Anh**. Đồng bộ hóa `ADS-20`, `ADS-30`, `ADS-02` theo chuẩn tiếng Anh của `PLAN.md`.

---

## 2. Thứ tự migration

Tên tệp đặt ở `backend/src/main/resources/db/migration/`. `ddl-auto: validate` (`QĐ-12`, `NFR-09`).

| Tệp | Bảng | Bất biến cưỡng chế | Gói |
|---|---|---|---|
| `V1__auth.sql` | `users` · `refresh_tokens` | `DI-11` `UNIQUE(token_hash)` · `CHECK role IN` | `BE-1` |
| `V2__trip.sql` | `destinations` · `user_preferences` · `trips` | `DI-1` · `DI-9` · `DI-10` · `UNIQUE(user_id)` trên preferences | `BE-2` |
| `V3__place.sql` | `places` · `saved_places` | `DI-7` · `DI-8` | `BE-2` |
| `V4__itinerary.sql` | `itinerary_days` · `activities` | `DI-2` · `DI-3` (hoãn) · `DI-4` · `DI-10` | `BE-3` |
| `V5__budget.sql` | `expenses` | `DI-10` · `CHECK category IN` | `BE-5` |
| `V6__ai_core.sql` | `conversations` · `messages` · `ai_tool_executions` | `DI-12` · `CHECK status IN` | `BE-6` |
| `V7__proposal.sql` | `ai_proposals` | `DI-5` · `DI-6` | `BE-8` |
| `R__seed_destinations.sql` | ~15 điểm đến VN + châu Á | — | `BE-2` |

`V3` đứng trước `V4` là hệ quả của `Q5`. Gieo điểm đến để **lặp lại được** (`R__`, dùng `INSERT ... ON CONFLICT DO NOTHING` theo `(country, name)`), không phải `V__` — để sửa danh sách không cần migration mới.

**`DI-3` viết thế nào:**

```sql
ALTER TABLE activities
  ADD CONSTRAINT uq_activity_day_order UNIQUE (itinerary_day_id, order_index)
  DEFERRABLE INITIALLY DEFERRED;
```

Phải là **table constraint**, không phải `CREATE UNIQUE INDEX` — PostgreSQL chỉ hoãn được ràng buộc, không hoãn được chỉ mục. Đây là điều kiện để `BR-204` sắp lại thứ tự trong một giao dịch mà không cần gán số âm tạm.

---

## 3. Lộ trình chín gói

### `BE-0` — Bộ khung và hạ tầng

Không có tính năng nào. Mục tiêu: `docker compose up` rồi `GET /actuator/health` trả `200`.

| # | Việc | Ghi chú |
|---|---|---|
| `BE-0.1` | `docker-compose.yml`: `postgres:16` + `redis:7`, có volume, có healthcheck | Kết nối được từ máy chủ |
| `BE-0.2` | Khởi tạo `backend/` bằng Spring Initializr: Web, Security, Data JPA, Validation, Flyway, Data Redis, Actuator | Java 21, Maven |
| `BE-0.3` | **Xác minh và ghim version thật** của Spring Boot 4.1.x + Spring AI 2.0.x trên Maven Central | `ADS-10` §6 rủi ro 7 — tên artifact đã đổi giữa Spring AI 1.x và 2.x. Ghi kết quả vào `docs/ADR-001-spring-ai.md` |
| `BE-0.4` | `application.yml` + `application-dev.yml` + `application-prod.yml`; mọi bí mật qua biến môi trường (`NFR-06`) | Danh sách biến: `ARCHITECTURE` §9 |
| `BE-0.5` | `.env.example` rỗng giá trị; `.env` vào `.gitignore` | |
| `BE-0.6` | Bật `spring.threads.virtual.enabled: true`, `spring.jpa.hibernate.ddl-auto: validate` | `ADS-10` §5.3 |
| `BE-0.7` | `common/`: `ApiError` (`code` · `message` · `details` · `timestamp`), `GlobalExceptionHandler`, `ErrorCode` enum | Hình dạng chốt ở `ADS-30` §1.1 |
| `BE-0.8` | Ngoại lệ miền: `NotFoundException` · `BusinessRuleException` · `ConflictException` · `RateLimitedException` · `UpstreamUnavailableException` | Ánh xạ sang `404` · `422` · `409` · `429` · `502` |
| `BE-0.9` | springdoc-openapi + Swagger UI, có `securityScheme` bearer | `NFR-08` |
| `BE-0.10` | Cấu hình Jackson: ngày `YYYY-MM-DD`, giờ `HH:mm`, mốc thời gian ISO-8601 có múi giờ | `ADS-30` §1 |
| `BE-0.11` | `RedisConfig` + `CacheService` bọc `GET`/`SET`/TTL | Khoá và TTL: `ADS-10` §5.4 |
| `BE-0.12` | Cấu trúc gói theo `ADS-10` §5.2, mỗi gói tự chứa controller · service · repository · entity · dto | |
| `BE-0.13` | Testcontainers cho Postgres + Redis, một test tích hợp trống chạy được | Nền cho mọi test sau |

**Xong khi:** `docker compose up` → ứng dụng khởi động, Flyway chạy `V1` rỗng, Swagger mở được, một test Testcontainers xanh.

---

### `BE-1` — Xác thực và phiên

Migration `V1`. Toàn bộ `FR-0xx`.

| # | Việc | Luật |
|---|---|---|
| `BE-1.1` | `V1__auth.sql`: `users` · `refresh_tokens` đúng `ADS-20` §2.1–2.2 | `DI-11` |
| `BE-1.2` | Entity + repository `User`, `RefreshToken` | |
| `BE-1.3` | `POST /api/auth/register` — từ chối email trùng, băm BCrypt | `BR-001` · `BR-002` |
| `BE-1.4` | `POST /api/auth/login` — phát cặp phiếu, lưu **băm SHA-256** của phiếu làm mới kèm `family_id` | `BR-002` |
| `BE-1.5` | `JwtService`: ký/kiểm HS256, `access` 15 phút, `refresh` 7 ngày, khoá ≥ 256 bit từ biến môi trường | `NFR-06` |
| `BE-1.6` | `JwtAuthenticationFilter` + `SecurityConfig`: không session, `permitAll` cho `/api/auth/**` và Swagger, còn lại `authenticated` | |
| `BE-1.7` | `POST /api/auth/refresh` — **xoay vòng**: cấp mới, thu hồi cũ trong một giao dịch | `BR-003` |
| `BE-1.8` | Phát hiện dùng lại phiếu đã thu hồi → thu hồi **cả họ** `family_id` → `401` | `BR-004` |
| `BE-1.9` | `POST /api/auth/logout` — thu hồi phiếu làm mới được gửi lên | `FR-004` |
| `BE-1.10` | RBAC: `USER` / `ADMIN`, một tài khoản một vai trò, `@PreAuthorize("hasRole('ADMIN')")` sẵn sàng | `BR-005` · `BR-006` |
| `BE-1.11` | `CurrentUser` — helper đọc `userId` từ `SecurityContext`. **Nguồn danh tính duy nhất của cả hệ thống, kể cả công cụ AI** | `QĐ-03` |
| `BE-1.12` | `GET /api/me` · `PATCH /api/me` | `FR-006` |
| `BE-1.13` | Kiểm tra: `password_hash` không xuất hiện trong bất kỳ DTO, log, hay phản hồi lỗi nào | `BR-002` · `NFR-05` |
| `BE-1.14` | Test: `BR-001` · `BR-003` · `BR-004` (dùng lại phiếu thu hồi giết cả họ) | |

**Xong khi:** `curl` đăng ký → đăng nhập → gọi `/api/me` bằng phiếu → đợi hết hạn → làm mới → phiếu cũ dùng lại bị `401` và mọi phiên của người đó chết.

---

### `BE-2` — Chuyến đi, điểm đến, sở thích, địa điểm

Migration `V2` + `V3` + `R__seed`. `FR-1xx`, một phần `FR-3xx`.

| # | Việc | Luật |
|---|---|---|
| `BE-2.1` | `V2__trip.sql` + `V3__place.sql` + `R__seed_destinations.sql` | `Q5` · `Q6` |
| `BE-2.2` | Entity `Destination` · `Trip` · `UserPreferences` · `Place` · `SavedPlace`; tiền là `long` + `currency`, **không `double`** | `QĐ-10` · `DI-10` |
| `BE-2.3` | `TripService.create` — một giao dịch: `insert trips` + sinh **đủ** `itinerary_days` + upsert `user_preferences` | `BR-202` |
| `BE-2.4` | Validate tạo chuyến: `endDate >= startDate` → `400 INVALID_DATE_RANGE`; độ dài ≤ 30 ngày → `422 TRIP_TOO_LONG`; `travelers >= 1`; điểm đến tồn tại → `404` | `DI-9` · `BR-201` |
| `BE-2.5` | `OwnershipGuard` — hàm dùng chung, ném `NotFoundException` khi **không có** *hoặc* **không phải của mình**. Không bao giờ `403` | `BR-101` · `BR-104` |
| `BE-2.6` | `GET /api/trips?status=upcoming\|past` phân trang; trạng thái **suy ra từ ngày hôm nay**, không lưu cột | `ADS-20` §2.5 |
| `BE-2.7` | `planningProgress` = số ngày có ≥ 2 hoạt động / tổng số ngày, **tính ở service, không lưu cột** | `BR-207` |
| `BE-2.8` | `PUT /api/trips/{id}` — đổi khoảng ngày thì sinh thêm hoặc xoá bớt ngày tương ứng, trong một giao dịch | `BR-203` |
| `BE-2.9` | `DELETE /api/trips/{id}` — cascade xuống ngày, hoạt động, chi tiêu, hội thoại; **không** đụng `places` | `FR-109` · `BR-304` |
| `BE-2.10` | `GET`/`PUT /api/me/preferences` | `FR-007` |
| `BE-2.11` | `PlaceSearchProvider` (interface) + `MockPlaceProvider` đọc JSON, bật ở profile `dev` | `BR-303` |
| `BE-2.12` | `PlaceService.search` — đệm Redis `places:{hash}` TTL 1h → gọi provider → **upsert theo `(provider, external_id)`** → trả | `DI-8` · `BR-301` · `BR-302` |
| `BE-2.13` | `GooglePlacesProvider` với **field mask tối thiểu**: `id`, `displayName`, `location`, `formattedAddress`, `rating`, `priceLevel`, `types` | `PLAN` §6 R2 |
| `BE-2.14` | Provider lỗi → `200` kèm `{ "results": [], "reason": "PROVIDER_UNAVAILABLE" }`, **không** ném lỗi | `QĐ-11` · `BR-602` |
| `BE-2.15` | `POST`/`DELETE /api/places/{id}/save`, `GET /api/me/saved-places`; lưu trùng → `409 PLACE_ALREADY_SAVED`; bỏ lưu **giữ** bản ghi địa điểm | `DI-7` · `BR-304` |
| `BE-2.16` | Test: `BR-101` (người A không đọc được chuyến người B) · `BR-201` · `BR-202` · `BR-207` · `DI-8` | |

**Xong khi:** tạo chuyến 4 ngày qua một lời gọi → thấy đủ 4 `itinerary_days` trong DB → tìm địa điểm ở profile `dev` không tốn một lời gọi trả phí nào → người dùng khác gọi cùng `tripId` nhận `404`.

---

### `BE-3` — Lịch trình

Migration `V4`. `FR-2xx`. Đây là gói mà `DI-3` bắt đầu có ý nghĩa.

| # | Việc | Luật |
|---|---|---|
| `BE-3.1` | `V4__itinerary.sql` — `activities.title NOT NULL`, `place_id NULL`, `activity_type`, `created_by`, ràng buộc `UNIQUE` hoãn | `DI-2` · `DI-3` · `DI-4` |
| `BE-3.2` | `GET /api/trips/{id}/itinerary` — **một truy vấn**, `JOIN FETCH` ngày → hoạt động → địa điểm, sắp theo `day_number` rồi `order_index` | `NFR-04` |
| `BE-3.3` | `POST /api/trips/{id}/itinerary/activities` — nhận `dayNumber`, `order_index = max + 1`, `created_by = USER` | `FR-204` |
| `BE-3.4` | Validate hoạt động: ngày thuộc chuyến · giờ kết thúc ≥ giờ bắt đầu · địa điểm tồn tại nếu có gửi | `BR-206` |
| `BE-3.5` | `PUT`/`DELETE /api/activities/{id}` — kiểm sở hữu **truy ngược** hoạt động → ngày → chuyến → người | `BR-102` |
| `BE-3.6` | `PUT /api/itinerary-days/{dayId}/reorder` — nhận **cả danh sách**, gán lại `order_index` từ 0 trong một giao dịch | `BR-204` |
| `BE-3.7` | Danh sách sắp lại phải khớp **đúng** tập hoạt động của ngày → thiếu/thừa/trùng → `422 REORDER_SET_MISMATCH` | `BR-205` |
| `BE-3.8` | `DistanceService` — Haversine, trả mét và giây theo tốc độ trung bình của phương tiện | `QĐ-09` |
| `BE-3.9` | Test: `BR-102` · `BR-204` · `BR-205` · `BR-206`; test hiệu năng khẳng định `GET itinerary` không sinh truy vấn lặp | `NFR-04` |

**Xong khi:** dựng thủ công lịch trình 3 ngày qua API → `reorder` một ngày 4 hoạt động → `order_index` liên tục từ 0, không có lỗ → gửi danh sách thiếu một mã nhận `422`.

---

### `BE-4` — Dịch vụ ngoài

Không có migration. `FR-5xx`. Một khuôn dùng cho cả ba dịch vụ.

| # | Việc | Luật |
|---|---|---|
| `BE-4.1` | `ExternalCallTemplate`: đệm Redis → `WebClient` hạn giờ 5s → thử lại 2 lần có giãn → ngắt mạch Resilience4j → **thất bại thì trả rỗng kèm lý do, không ném lỗi** | `QĐ-11` · `BR-602` |
| `BE-4.2` | `OpenMeteoClient` + `WeatherService`; khoá `weather:{lat}:{lng}:{ngày}` TTL 3h | |
| `BE-4.3` | **Rẽ nhánh 16 ngày**: trong hạn → Forecast API, nhãn `DU_BAO`; ngoài hạn → Archive API lấy trung bình cùng kỳ, nhãn `TRUNG_BINH_KHI_HAU` | `QĐ-07` · `FR-502` |
| `BE-4.4` | `GET /api/trips/{id}/weather` — **mọi phần tử bắt buộc có `source`**; Open-Meteo chết → `200` kèm `days: []` và `reason` | `FR-503` · `BR-510` |
| `BE-4.5` | `CurrencyProvider` (interface) + `ErApiCurrencyProvider` gọi `open.er-api.com`; khoá `currency:{từ}:{sang}` TTL 12h | `PLAN` §6 R3 |
| `BE-4.6` | Test: ngắt mạng giả lập → điểm cuối thời tiết vẫn `200` · nhãn nguồn đúng ở hai phía biên 16 ngày | `BR-602` |

**Xong khi:** chuyến trong 16 ngày trả `DU_BAO`, chuyến 2 tháng nữa trả `TRUNG_BINH_KHI_HAU`, tắt mạng ra ngoài thì điểm cuối vẫn `200` với `days: []`.

---

### `BE-5` — Ngân sách và chi tiêu

Migration `V5`. `FR-4xx`.

| # | Việc | Luật |
|---|---|---|
| `BE-5.1` | `V5__budget.sql` — `expenses` có `activity_id` nullable `ON DELETE SET NULL` | `ADS-20` §2.10 |
| `BE-5.2` | `BudgetService.calculate` — `estimatedTotal` từ `activities.estimated_cost`, `actualTotal` từ `expenses.amount`, **hai đại lượng tách rời** | `BR-402` |
| `BE-5.3` | `remaining` = ngân sách − **phần lớn hơn** giữa hai tổng | `ADS-30` §7 |
| `BE-5.4` | Chỉ cộng khoản **cùng `currency`**; khác mã tiền thì không gộp im lặng | `BR-401` |
| `BE-5.5` | `warningLevel`: `NONE` · `NEAR_LIMIT` (>90%) · `OVER` (>100%) | `BR-403` |
| `BE-5.6` | `byCategory` gom theo sáu hạng mục | `FR-404` |
| `BE-5.7` | CRUD `/api/trips/{id}/expenses` + `/api/expenses/{id}`, lọc theo hạng mục và khoảng ngày | `FR-403` |
| `BE-5.8` | Test đơn vị cho `BudgetService`: `BR-401` · `BR-402` · `BR-403` ba mốc 89% / 91% / 101% | |

**Xong khi:** một chuyến có ước tính 7,35tr và thực tế 2,1tr trên ngân sách 8tr trả đúng `remaining` 650k và `warningLevel: NEAR_LIMIT`.

> 🏁 **Mốc M1 phía backend.** Đến đây toàn bộ website chạy được **không cần AI** — mốc chống rủi ro quan trọng nhất của `PLAN.md` §0.

---

### `BE-6` — Nền tảng trợ lý và ba công cụ đọc

Migration `V6`. `FR-6xx` phần lõi. Gói nặng nhất.

| # | Việc | Luật |
|---|---|---|
| `BE-6.1` | `V6__ai_core.sql` — `conversations` · `messages` (có `tool_calls_json`, `tool_call_id`, `token_usage`) · `ai_tool_executions` (có `trip_id`, `user_id`) | `DI-12` |
| `BE-6.2` | Cấu hình `spring-ai-starter-openai` trỏ `base-url` Gemini; ba biến `LLM_BASE_URL` · `GEMINI_API_KEY` · `LLM_MODEL_FAST`/`DEEP` | `QĐ-04` |
| `BE-6.3` | **Kiểm chứng cấu hình bằng phản hồi thật.** Lớp tương thích bỏ qua tham số lạ trong im lặng — cấu hình sai vẫn chạy mà vô tác dụng | `ADS-21` §1.4 |
| `BE-6.4` | `ChatModelRouter.forTask(TaskType)` — `TRO_LY` → flash, `SINH_LICH_TRINH`/`PHAN_TICH_NGAN_SACH` → pro, nhánh dự phòng. **Dịch vụ nghiệp vụ không bao giờ tự chọn mô hình** | `ADS-21` §1.2 |
| `BE-6.5` | `ConversationService` — tạo/nạp hội thoại, lưu tin nhắn, nạp N lượt gần nhất và **cắt không được cắt lẻ giữa cặp gọi công cụ ↔ kết quả** | `ADS-21` §3.4, cạm bẫy 3 |
| `BE-6.6` | `ToolContext` mang `userId` + `tripId`, dựng từ `SecurityContext`. **Hai giá trị này không có trong lược đồ công cụ** | `QĐ-03` · `BR-501` |
| `BE-6.7` | Ba công cụ đọc: `get_current_trip` · `get_itinerary` · `get_weather`. Mỗi công cụ gọi **service đã có sẵn**, không viết truy vấn riêng | `QĐ-02` |
| `BE-6.8` | `ToolExecutionLogger` — ghi `ai_tool_executions` **kể cả khi lỗi hoặc quá hạn**, cắt bớt `result` nếu dài | `BR-505` |
| `BE-6.9` | `AgentService` — vòng lặp, trần cứng **8 vòng / 60 giây**, tuỳ cái nào đến trước; chạm trần thì trả lời bằng dữ liệu đang có, **không báo lỗi** | `QĐ-06` · `BR-502` |
| `BE-6.10` | Chặn gọi lại cùng công cụ với cùng bộ tham số trong một lượt | `BR-503` |
| `BE-6.11` | `POST /api/trips/{id}/ai/chat` trả `SseEmitter`; kiểm **sở hữu trước** khi mở luồng | `BR-101` |
| `BE-6.12` | Năm sự kiện: `tool_start` · `tool_end` (+ `ms`, `status`) · `token` · `proposal` · `done`; mô hình lỗi → `event: error` rồi đóng luồng | `ADS-30` §9.1 |
| `BE-6.13` | Lời nhắc hệ thống dựng lại mỗi lượt: tóm tắt chuyến · **ngày hôm nay** · tiếng Việt ngắn gọn · cấm bịa địa điểm · luôn nêu nhãn nguồn thời tiết · muốn đổi lịch thì phải dùng công cụ đề xuất | `ADS-21` §5 |
| `BE-6.14` | `GET /api/trips/{id}/ai/conversations` · `GET /api/conversations/{id}/messages` · `GET /api/trips/{id}/ai/activity` | `FR-607` |
| `BE-6.15` | Test bảo mật: người A hỏi *"đọc chuyến số 42 giúp tôi"* → công cụ vẫn chỉ chạm chuyến của A | `BR-103` · `BR-501` |

**Xong khi:** `curl -N` vào điểm cuối chat, hỏi "lịch trình ngày 2 của tôi có gì" → thấy `tool_start` → `tool_end` → chuỗi `token` → `done`, và `ai_tool_executions` có đúng một dòng `OK`.

---

### `BE-7` — Bộ công cụ đầy đủ và giới hạn tần suất

Không có migration. `FR-6xx` phần còn lại, `FR-9xx` phần nhật ký.

| # | Việc | Luật |
|---|---|---|
| `BE-7.1` | Năm công cụ còn lại: `search_places` · `calculate_distance` · `calculate_trip_budget` · `get_saved_places` · `get_user_preferences` | `ADS-21` §2.1 |
| `BE-7.2` | Cắt gọn kết quả: `search_places` **tối đa 5 kết quả × 8 trường**; `get_itinerary` 6 trường mỗi hoạt động; `get_weather` một dòng mỗi ngày | `BR-504` · `ADS-21` §2.4 |
| `BE-7.3` | `RateLimitFilter` tầng API: `rl:api:{userId}:{phút}` — 120/phút | `BR-601` |
| `BE-7.4` | Giới hạn tầng AI, kiểm **trước khi mở luồng**: `rl:ai:{userId}:{giờ}` 20/giờ và `rl:ai:{userId}:{ngày}` 100/ngày | `BR-601` · `NFR-07` |
| `BE-7.5` | `429` kèm `retryAfterSeconds`; đếm bằng `INCR` + `EXPIRE` **trong một script Lua** để không có cửa sổ khoá vĩnh viễn | `ADS-30` §13 |
| `BE-7.6` | Đo số token trung bình mỗi lượt, ghi vào `messages.token_usage`; chốt lại con số giới hạn sau khi biết hạn ngạch thật của tài khoản | `ADS-21` §9 |
| `BE-7.7` | Test: `BR-503` chặn gọi lặp · `BR-504` đúng 5 kết quả · `BR-601` vượt hạn trả `429` | |

**Xong khi:** hỏi "tìm quán ăn gần khách sạn, giá vừa phải" → trợ lý gọi 2–3 công cụ, mỗi kết quả gọn; gửi tin nhắn thứ 21 trong một giờ nhận `429` kèm số giây phải chờ.

---

### `BE-8` — Đề xuất, phê duyệt, sinh lịch trình, quản trị

Migration `V7`. `FR-7xx`, `FR-9xx`. **Đây là đề tài** — `PLAN.md` §9 cấm cắt phần này.

| # | Việc | Luật |
|---|---|---|
| `BE-8.1` | `V7__proposal.sql` — `ai_proposals` với `status` theo `Q1`, `expires_at` = tạo + 30 phút | `DI-5` · `DI-6` · `FR-707` |
| `BE-8.2` | Bốn thao tác `ADD` · `REMOVE` · `UPDATE` · `REORDER` khai báo **sealed interface** Java 21 để trình biên dịch bắt nhánh thiếu | `ARCHITECTURE` §1.5 |
| `BE-8.3` | Công cụ `propose_itinerary_changes` — **chỉ ghi `ai_proposals`, tuyệt đối không đụng `activities`** | `QĐ-01` |
| `BE-8.4` | Lúc dựng đề xuất: validate từng thao tác, tính `estimated_cost_delta` và `travel_time_delta` | `FR-703` |
| `BE-8.5` | `GET /api/proposals/{id}` trả dữ liệu đủ để giao diện vẽ thẻ khác biệt | `FR-704` |
| `BE-8.6` | `POST /api/trips/{id}/ai/apply` **chỉ nhận `proposalId`** — không nhận danh sách thay đổi từ máy khách | `BR-506` · `FR-705` |
| `BE-8.7` | Bốn điều kiểm: đúng chuyến (`404`) · đúng người (`404`, không phải `403`) · trạng thái chờ (`409`) · chưa hết hạn (`409`) | `BR-507` · `BR-104` |
| `BE-8.8` | **Kiểm lại từng thao tác lúc áp dụng, không tin `changes_json` cũ**: `REMOVE`/`UPDATE` trỏ tới hoạt động đã xoá → bỏ qua và báo trong `skipped`; `REORDER` chứa mã không còn hoặc địa điểm đã biến mất → **huỷ cả đề xuất** | `ADS-21` §4.4 |
| `BE-8.9` | Áp dụng trong **một giao dịch**, đánh lại `order_index` của ngày bị ảnh hưởng, đổi trạng thái, trả lịch trình mới + danh sách `skipped` | `FR-709` · `ADS-30` §10.3 |
| `BE-8.10` | Gọi lần hai → `409 PROPOSAL_NOT_PENDING`, không nhân đôi dữ liệu | `BR-508` · `Q4` |
| `BE-8.11` | `POST /api/proposals/{id}/reject` — chuyển trạng thái, **không xoá bản ghi** | `FR-710` |
| `BE-8.12` | `POST /api/trips/{id}/ai/generate` → `202` + `jobId`, chạy nền trên luồng ảo; `GET .../generate/{jobId}` trả `state` · `step` · `progress` | `FR-609` · `ADS-10` §5.3 |
| `BE-8.13` | **Bước đối chiếu địa điểm** trong luồng sinh: tên mô hình nhắc tới phải khớp một bản ghi có thật, không khớp thì **loại**; đếm tỉ lệ bị loại | `QĐ-08` · `BR-509` |
| `BE-8.14` | Sinh lịch trình **ghi thẳng** vào `activities` với `created_by = AI` — ngoại lệ duy nhất của `QĐ-01`, chỉ áp dụng cho chuyến đang trống | `ADS-10` §3.4 |
| `BE-8.15` | `optimize_day_order` — láng giềng gần nhất + 2-opt **viết bằng Java**, giữ nguyên hoạt động bị neo giờ; cải thiện < 5% thì không đề xuất gì | `QĐ-09` · `FR-613` |
| `BE-8.16` | Bốn điểm cuối quản trị `@PreAuthorize("hasRole('ADMIN')")`, **chỉ đọc**: `users` · `trips` · `tool-executions` (lọc theo người/công cụ/trạng thái/khoảng ngày) · `ai-usage` | `BR-006` · `FR-903`–`FR-906` |
| `BE-8.17` | Tác vụ dọn `ai_proposals` quá hạn: `PENDING` + quá `expires_at` → `EXPIRED` | `ADS-20` §5 |
| `BE-8.18` | Test: `BR-506` · `BR-507` bốn ca hỏng riêng biệt · `BR-508` áp dụng hai lần · `BR-509` mô hình bịa địa điểm bị loại · `ProposalService` áp dụng khi lịch trình đã đổi | |

**Xong khi:** 🏁 **Mốc M2.** "Ngày mai mưa thì đổi lịch giúp tôi" → trợ lý gọi ≥ 3 công cụ → sinh đề xuất → `GET` thấy khác biệt → `POST apply` đổi DB thật → gọi lại lần hai nhận `409` và dữ liệu không nhân đôi.

---

## 4. Bản đồ điểm cuối → gói

| Nhóm | Điểm cuối | Gói |
|---|---|---|
| Xác thực | `POST /api/auth/{register,login,refresh,logout}` | `BE-1` |
| Hồ sơ | `GET`/`PATCH /api/me` · `GET`/`PUT /api/me/preferences` | `BE-1` · `BE-2` |
| Chuyến đi | `GET`/`POST /api/trips` · `GET`/`PUT`/`DELETE /api/trips/{id}` | `BE-2` |
| Lịch trình | `GET /api/trips/{id}/itinerary` · `POST .../activities` · `PUT`/`DELETE /api/activities/{id}` · `PUT /api/itinerary-days/{dayId}/reorder` | `BE-3` |
| Địa điểm | `GET /api/places` · `GET /api/places/{id}` · `POST`/`DELETE /api/places/{id}/save` · `GET /api/me/saved-places` | `BE-2` |
| Ngân sách | `GET /api/trips/{id}/budget` · CRUD `expenses` | `BE-5` |
| Thời tiết | `GET /api/trips/{id}/weather` | `BE-4` |
| Trợ lý | `POST /api/trips/{id}/ai/chat` · `GET .../ai/conversations` · `GET /api/conversations/{id}/messages` · `GET .../ai/activity` | `BE-6` |
| Sinh lịch trình | `POST /api/trips/{id}/ai/generate` · `GET .../generate/{jobId}` | `BE-8` |
| Đề xuất | `GET /api/proposals/{id}` · `POST /api/trips/{id}/ai/apply` · `POST /api/proposals/{id}/reject` | `BE-8` |
| Quản trị | `GET /api/admin/{users,trips,tool-executions,ai-usage}` | `BE-8` |

Không có webhook. Không có điểm cuối nào cho dịch vụ ngoài gọi vào — `ADS-30` §12.

---

## 5. Ma trận kiểm thử — 28 luật `BR`

Mỗi luật một ca kiểm thử. Cột cuối là gói phải làm xong ca đó.

| Luật | Kiểm gì | Gói |
|---|---|---|
| `BR-001` `BR-002` | Email trùng bị từ chối · băm không lộ ra ngoài | `BE-1` |
| `BR-003` `BR-004` | Xoay vòng phiếu · dùng lại phiếu thu hồi giết cả họ | `BE-1` |
| `BR-005` `BR-006` | Một vai trò · điểm cuối quản trị từ chối `USER` | `BE-1` · `BE-8` |
| `BR-101` `BR-102` `BR-104` | Sở hữu chuyến · truy ngược qua ngày · không quyền trả `404` | `BE-2` · `BE-3` |
| `BR-103` | **Đường AI chịu cùng luật sở hữu** | `BE-6` |
| `BR-201` `BR-202` `BR-203` | 30 ngày · sinh ngày trong một giao dịch · đổi khoảng ngày | `BE-2` |
| `BR-204` `BR-205` `BR-206` `BR-207` | Sắp lại cả danh sách · khớp đúng tập · giờ hợp lệ · tiến độ | `BE-3` |
| `BR-301`–`BR-304` | Upsert theo cặp định danh · ảnh chụp · nhà cung cấp giả ở `dev` · bỏ lưu giữ địa điểm | `BE-2` |
| `BR-401`–`BR-403` | Cùng mã tiền · ước tính tách thực tế · hai mốc cảnh báo | `BE-5` |
| `BR-501` | Danh tính không nằm trong lược đồ công cụ | `BE-6` |
| `BR-502` `BR-503` | Trần 8 vòng / 60 giây · chặn gọi lặp | `BE-6` |
| `BR-504` | 5 kết quả × 8 trường | `BE-7` |
| `BR-505` | Nhật ký ghi **cả khi lỗi** | `BE-6` |
| `BR-506`–`BR-508` | Chỉ nhận mã · bốn điều kiểm · lần hai vô hiệu | `BE-8` |
| `BR-509` `BR-510` | Loại địa điểm bịa · nhãn nguồn thời tiết đi tới cùng | `BE-8` · `BE-4` |
| `BR-601` `BR-602` | Hai tầng giới hạn · dịch vụ ngoài lỗi không ném ra người dùng | `BE-7` · `BE-4` |

**Ba ca không được thiếu khi bảo vệ:** `BR-103` (người A không đọc được chuyến người B **qua đường AI**) · `BR-506` (gửi thẳng danh sách thay đổi bị từ chối) · `BR-509` (địa điểm bịa bị loại).

---

## 6. Bẫy kỹ thuật

Mười cái đầu lấy từ `ARCHITECTURE.md` §10. Sáu cái sau là bẫy triển khai của riêng backend.

1. Công cụ nhận `userId`/`tripId` từ ngữ cảnh, **không** từ tham số mô hình sinh
2. Dịch vụ kiểm sở hữu **kể cả** khi được gọi từ công cụ
3. Công cụ trả gói gọn — không đưa nguyên phản hồi Google Places vào mô hình
4. Vòng lặp luôn có trần vòng và trần thời gian
5. Dịch vụ ngoài chết không làm chết trang
6. Tiền dùng `BIGINT`, không `double`
7. Truy vấn lịch trình `JOIN FETCH`, tránh truy vấn lặp
8. `apply` phải vô hiệu ở lần gọi thứ hai
9. Mọi thay đổi lược đồ qua Flyway
10. Cấu hình Gemini sai **không báo lỗi** — kiểm chứng bằng phản hồi thật
11. **`DEFERRABLE` phải là table constraint**, không dùng `CREATE UNIQUE INDEX` — nếu không thì `reorder` sẽ vỡ giữa giao dịch
12. **`SseEmitter` không được nằm trong `@Transactional`** — giữ giao dịch mở 60 giây sẽ cạn bể kết nối. Luồng sự kiện ở controller, giao dịch ở service, mỗi lời gọi công cụ một giao dịch ngắn
13. **`JOIN FETCH` nhiều tập hợp cùng lúc** ném `MultipleBagFetchException` — dùng `Set`, hoặc tách hai truy vấn, hoặc `@BatchSize`
14. **`INCR` + `EXPIRE` hai lệnh rời có cửa sổ chết** — tiến trình chết giữa hai lệnh để lại khoá không hạn dùng, người dùng bị khoá vĩnh viễn. Gói vào một script Lua
15. **MapStruct + Lombok cần khai báo đúng thứ tự** `annotationProcessorPaths` trong `maven-compiler-plugin`, sai thứ tự thì mapper sinh ra rỗng
16. **`ddl-auto: validate` không kiểm ràng buộc `CHECK`** — enum lệch giữa Java và cột chỉ vỡ lúc chạy thật. Viết một test khẳng định tập giá trị của enum khớp `CHECK` trong migration

---

## 7. Làm ngay hôm nay

Theo đúng thứ tự:

1. **Chốt sáu điều ở §1** rồi sửa ngược lên `PLAN.md` §12 và §5 — trước khi viết bất kỳ migration nào
2. `BE-0.3` — mở tài liệu chính thức Spring AI, ghi lại **tên artifact thật**, cú pháp `@Tool`, cách truyền `ToolContext` vào `docs/ADR-001-spring-ai.md`. Đừng copy từ blog
3. Lấy khoá Gemini ở AI Studio, xem **trang hạn ngạch của chính tài khoản mình**, ghi con số vào `ADR-001`
4. Tạo Google Cloud project → bật Places API → **đặt trần hạn ngạch và cảnh báo chi phí** → gọi thử một lời gọi Text Search bằng `curl`, xem giá của field mask
5. `BE-0.1` → `BE-0.6` — bộ khung chạy được
6. `BE-0.7` → `BE-0.13` — tầng dùng chung, có test Testcontainers xanh

Xong bước 6 thì `BE-1` bắt đầu được ngay.

---

## 8. Tài liệu liên quan

| Tài liệu | Nội dung |
|---|---|
| [`PLAN.md`](PLAN.md) §12 | **Nguồn chân lý** — `QĐ` · `DI` · `BR` |
| [`PLAN.md`](PLAN.md) §9 | Thứ tự cắt scope khi trễ — ba thứ tuyệt đối không cắt |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) §9 | Bảng biến môi trường đầy đủ |
| [`ADS-20`](ADS/ADS-20-Thiet-ke-CSDL.md) | Từng cột, từng ràng buộc |
| [`ADS-21`](ADS/ADS-21-Tich-hop-AI-Agent.md) | Công cụ · vòng lặp · đề xuất |
| [`ADS-30`](ADS/ADS-30-Hop-dong-API.md) | Điểm cuối · mã lỗi · phân quyền |
