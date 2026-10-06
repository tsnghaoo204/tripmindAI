# BE-ROADMAP — Kế hoạch hoàn thiện backend TripMind

**v1.1 · 06/10/2026** · `P0`→`P8` đã xong, xem [BE-CHANGES.md](BE-CHANGES.md) · Chỉ nói về **backend**. Giao diện làm sau khi backend xong.

> File này **thay thế** [`TODO.md`](TODO.md) và [`BE-TODO.md`](BE-TODO.md) làm nơi theo dõi tiến độ backend.
> `BE-TODO.md` vẫn giữ giá trị tham khảo cho luật nghiệp vụ (`BR`, `QĐ`, `DI`) và bẫy kỹ thuật (§6),
> nhưng nhiều giả định của nó đã đổi (Spring Boot 4, refresh token, cấu trúc gói theo tính năng). Chỗ nào hai file nói khác nhau thì **file này đúng**.
>
> Nguồn: [`FEATURE-REVIEW.md`](FEATURE-REVIEW.md) (danh sách đã duyệt) · [`ADS-01`](ADS/ADS-01-Dac-ta-yeu-cau.md) (mã `FR`) · [`PLAN.md`](PLAN.md) §12 (mã `BR` / `QĐ` / `DI`) · [`ADS-21`](ADS/ADS-21-Tich-hop-AI-Agent.md) (công cụ AI) · [`schemas.sql`](../schemas.sql)

---

## 0. Cách dùng

- Mười giai đoạn `P0` → `P9`, xếp theo **thứ tự phụ thuộc**. Làm xong hẳn một giai đoạn rồi sang giai đoạn kế.
- Mỗi việc có mã `Px.y`. Commit tham chiếu mã đó: `feat(P3.4): budget summary`.
- Mỗi giai đoạn có **Xong khi**: điều kiện nghiệm thu chạy được bằng `curl` hoặc test, không phải "đã viết code".
- Làm xong việc nào thì tick `[x]` ở bảng tiến độ §3 và ghi một dòng vào **Nhật ký** §11.
- Tính năng mới (đã duyệt ở `FEATURE-REVIEW.md` phần B) gắn nhãn 🆕.

**DoD chung cho mọi việc:**

- [ ] Đụng lược đồ thì có migration Flyway mới, không sửa migration cũ
- [ ] Validate đầu vào + kiểm sở hữu ở **tầng service**; không phải của mình thì trả `404`, không trả `403` (`BR-104`)
- [ ] Có `@Operation` mô tả trên Swagger
- [ ] Có test đơn vị cho luật nghiệp vụ; thử tay ít nhất một ca lỗi
- [ ] Code theo đúng cấu trúc hiện có: `controllers/` · `services/` · `repositories/` · `entities/` · `domains/requests|responses/` · `enums/`

---

## 1. Hiện trạng (05/10/2026)

### 1.1 Đã có và chạy được

26 điểm cuối, 21 test đơn vị xanh (`mvn test`).

| Nhóm | Điểm cuối |
|---|---|
| Xác thực | `POST /api/auth/register` · `POST /api/auth/login` · `GET /api/auth/me` · `GET /api/auth/health` |
| Hồ sơ | `GET`/`PUT /api/users/profile` · `PUT /api/users/change-password` |
| Điểm đến | `GET /api/destinations` · `GET /api/destinations/popular` · `GET /api/destinations/{id}` |
| Địa điểm | `GET /api/places/search` · `GET /api/places/recommendations/nearby` · `GET /api/places/{provider}/{externalId}` |
| Đã lưu | `POST`/`DELETE /api/places/{placeId}/save` · `GET /api/me/saved-places` |
| Chuyến đi | `POST`/`GET /api/trips` · `GET`/`PUT`/`DELETE /api/trips/{tripId}` |
| Lịch trình | `GET /api/trips/{tripId}/itinerary` · `POST .../itinerary/activities` · `PUT`/`DELETE /api/activities/{id}` · `PUT /api/itinerary-days/{dayId}/reorder` |

### 1.2 Lỗi và thiếu sót tìm thấy khi đọc code

| # | Vấn đề | Ở đâu | Xử lý ở |
|---|---|---|---|
| G1 | **Khoá Gemini và Google Places thật** nằm làm giá trị mặc định trong `application.yml`, đã push lên GitHub | `application.yml` | `P0.1` |
| G2 | Không có Flyway; lược đồ chạy tay. Có **hai** `schemas.sql` khác nhau; entity khớp bản ở thư mục gốc | `schemas.sql` · `resources/db/schemas.sql` | `P0.3` |
| G3 | `POST /api/trips` **nhận** `travelStyle`, `budgetPreference`, `preferences` rồi **bỏ đi**: bảng `trips` không có cột tương ứng | `TripServiceImpl.createTrip` | `P1.1` |
| G4 | Không giới hạn 30 ngày; sửa chuyến không đổi được ngày đi/về | `TripServiceImpl` | `P1.4` · `P1.5` |
| G5 | Không kiểm giờ kết thúc ≥ giờ bắt đầu của hoạt động (`BR-206`) | `ItineraryServiceImpl` | `P2.1` |
| G6 | `reorder` không bắt **mã trùng**: gửi `[1,1]` cho ngày có `[1,2]` lọt qua kiểm tra, rồi vỡ ràng buộc `UNIQUE` lúc commit → `500` | `ItineraryServiceImpl.reorderActivities` | `P2.2` |
| G7 | `getItinerary` trả `403` khi không phải chủ chuyến, lộ ra chuyến đó có tồn tại (`BR-104` yêu cầu `404`) | `ItineraryServiceImpl:43` | `P2.3` |
| G8 | `BudgetService` mới là interface, chưa có code xử lý | `BudgetService.java` | `P3` |
| G9 | Enum `ProposalStatus` thiếu `REVERTED`, trong khi `CHECK` của bảng `ai_proposals` có | `enums/ProposalStatus.java` | `P0.5` |
| G10 | `open-in-view: true`: với SSE chạy tới 60 giây, mỗi lượt chat sẽ giữ một kết nối DB suốt thời gian đó | `application.yml` | `P5.1` |
| G11 | Model `gemini-1.5-flash` đã bị Google ngừng | `application.yml` | `P5.0` |
| G12 | `pom.xml` dùng Spring Boot **3.4.3**, nhưng tài liệu ghi Boot 4.1 + Spring AI 2.0 | `pom.xml` · `ARCHITECTURE.md` | `D1` |

---

## 2. Quyết định

`D1`–`D11` đã chốt hoặc đề xuất chốt. Mục ghi "chờ xác nhận" thì xác nhận trước khi bắt đầu giai đoạn liên quan.

| # | Quyết định | Lý do | Trạng thái |
|---|---|---|---|
| `D1` | **Giữ Spring Boot 3.4 + Spring AI 1.x** (starter `spring-ai-starter-model-openai`, trỏ vào lớp tương thích OpenAI của Gemini). Sửa lại `ARCHITECTURE.md` và `PLAN.md` | Spring AI 2.0 bắt buộc Boot 4. Nâng Boot giữa dự án dễ vỡ Security/JPA mà không thêm giá trị gì cho đề tài | Đã chốt 06/10 |
| `D2` | **Không làm refresh token.** Token truy cập sống 24h. Ghi vào phần hạn chế của báo cáo | `TODO.md` đã chốt | Đã chốt |
| `D3` | **Không quy đổi tỷ giá.** Mọi khoản chi phải cùng mã tiền với chuyến; khác mã thì từ chối (`422 CURRENCY_MISMATCH`) | PLAN xếp tỷ giá đầu danh sách cắt; `BR-401` vẫn được giữ | Đã chốt |
| `D4` | **Flyway bắt đầu từ `V1__baseline.sql`** = `schemas.sql` ở thư mục gốc, bỏ các câu `DROP`. Xoá `resources/db/schemas.sql`. File gốc chỉ còn một dòng trỏ sang thư mục migration | Một nguồn duy nhất; DB đang có sẵn được nhận làm baseline, không phải tạo lại | Đã chốt |
| `D5` | **Không làm:** nhật ký + ảnh (`FR-1011`→`1013`), nhập bằng giọng nói (`FR-1014`), thói quen ước lượng (`FR-1106`) | Đã cắt ở `FEATURE-REVIEW.md` | Đã chốt |
| `D6` | Model mặc định: dòng **Gemini Flash hiện hành** (ví dụ `gemini-2.5-flash`). Kiểm tra lại tên trong AI Studio trước khi ghim | `gemini-1.5-flash` đã bị ngừng | Đã chốt: `gemini-3.8-flash`, đổi qua `LLM_MODEL` |
| `D7` | Giữ **cấu trúc phân tầng** hiện có (`controllers/services/...`), không chuyển sang cấu trúc theo tính năng như `ADS-10` | Code đã viết theo kiểu này; đổi giữa chừng tốn công mà không thêm tính năng | Đã chốt |
| `D8` | **Sở thích lưu theo từng chuyến** (cột mới trên `trips`). Nếu khi tạo chuyến không gửi lên thì lấy mặc định từ `user_preferences` | Mỗi chuyến có thể khác nhau (đi với bạn bè khác đi với gia đình); AI cần đọc theo chuyến | Đã chốt |
| `D9` | **Checklist dùng luồng đề xuất dạng nhẹ**, giống `propose_places`: hệ thống trả danh sách ứng viên kèm lý do, người dùng chọn cái nào thì mới ghi vào DB. **Không** thêm loại mới vào `ai_proposals` | Vẫn giữ nguyên tắc `QĐ-01` (không ghi nếu người dùng chưa bấm) mà không phải mở rộng cơ chế áp dụng/hoàn tác | Đã chốt |
| `D10` | **PDF làm ở frontend** bằng CSS in (`@media print` + `window.print()`). Backend chỉ xuất `.ics` | Trình duyệt tự xử lý font tiếng Việt; làm PDF ở backend phải nhúng font, mất thêm 1–2 ngày | Đã chốt 06/10 |
| `D11` | **Vòng gọi công cụ dùng `call()`, không stream.** Chỉ lượt trả lời cuối được chia thành nhiều sự kiện `token` gửi qua SSE. Tiến trình hiển thị bằng `tool_start` / `tool_end` | Vòng lặp tự điều khiển cần đọc trọn danh sách tool call mỗi vòng. Ghép tool call từ các mảnh stream phức tạp và dễ lỗi | Đã chốt 06/10 |

---

## 3. Bảng tiến độ

| Giai đoạn | Nội dung | Ước tính | Trạng thái |
|---|---|---|---|
| `P0` | Dọn nền: khoá API, Flyway, Docker Compose | 1,5 ngày | [x] |
| `P1` | Chuyến đi: sở thích, 🆕 thông tin nhóm, 30 ngày, đổi ngày, giai đoạn chuyến | 3 ngày | [x] |
| `P2` | Lịch trình: sửa lỗi + 🆕 gợi ý chi phí ước tính | 1,5 ngày | [x] |
| `P3` | Ngân sách, chi tiêu + 🆕 hôm nay còn tiêu được bao nhiêu | 2,5 ngày | [x] |
| `P4` | Thời tiết | 2 ngày | [x] |
| `P5` | Trợ lý AI: nền tảng, công cụ đọc, SSE | 6 ngày | [x] |
| `P6` | Đề xuất → duyệt → áp dụng → hoàn tác → giải trình, sinh lịch trình | 7 ngày | [x] |
| `P7` | 🆕 Checklist · xuất `.ics` · đánh giá địa điểm · nhân bản chuyến | 4,5 ngày | [x] |
| `P8` | Quản trị, giới hạn tần suất, test tích hợp, đóng gói | 2 ngày | [x] |
| `P9` | Mở rộng nếu còn thời gian | — | [ ] |

**Tổng `P0`→`P8` khoảng 30 ngày làm việc ≈ 6 tuần** nếu làm một mình toàn thời gian. Xem §9 về thời hạn và thứ tự cắt.

---

## 4. Lược đồ: danh sách migration

Đặt tại `backend/src/main/resources/db/migration/`.

| Tệp | Nội dung | Giai đoạn |
|---|---|---|
| `V1__baseline.sql` | Toàn bộ `schemas.sql` gốc, bỏ `DROP` | `P0.3` |
| `V2__trip_profile.sql` | `trips` thêm `travel_style`, `budget_preference`, `preferences_json`, `group_profile`; đổi `UNIQUE (trip_id, date)` của `itinerary_days` sang `DEFERRABLE` | `P1.1` |
| `V3__activity_cost_source.sql` | `activities` thêm `estimated_cost_source` | `P2.4` |
| `V4__message_attachments.sql` | `messages` thêm `attachments_json` | `P5` |
| `V5__checklist.sql` | Bảng `trip_checklist_items` | `P7.1` |
| `V6__place_ratings.sql` | Bảng `place_ratings` | `P7.3` |

Các bảng `expenses`, `conversations`, `messages`, `ai_tool_executions`, `ai_proposals` **đã có trong `V1`**, không cần migration riêng. Chỉ còn thiếu entity Java.

---

## 5. Chi tiết từng giai đoạn

### `P0` — Dọn nền (1,5 ngày)

| # | Việc | Ghi chú |
|---|---|---|
| `P0.1` | **Thu hồi** khoá Gemini và Places trên Google Cloud / AI Studio, tạo khoá mới. Xoá mọi giá trị mặc định chứa bí mật trong `application.yml`: `GEMINI_API_KEY`, `GOOGLE_PLACES_API_KEY`, `JWT_SECRET` | Chỉ xoá khỏi code là chưa đủ: khoá vẫn nằm trong lịch sử git từ commit `72d0fc3` |
| `P0.2` | Thêm `.env.example` (chỉ có tên biến, không có giá trị). Trong `application.yml` thêm `spring.config.import: optional:file:.env[.properties]` để Spring đọc thẳng file `.env` | `.env` đã có trong `.gitignore` |
| `P0.3` | Thêm `flyway-core` + `flyway-database-postgresql`. Tạo `V1__baseline.sql`. Cấu hình `spring.flyway.baseline-on-migrate: true`, `baseline-version: 1` để DB đang có dữ liệu được nhận làm mốc, không chạy lại V1 | Thực hiện `D4` |
| `P0.4` | `docker-compose.yml` ở thư mục gốc: `postgres:16` + `redis:7`, có volume và healthcheck | Backend chạy ngoài Compose cho đến `P8.5` |
| `P0.5` | Thêm `REVERTED` vào `ProposalStatus`. Viết **một test** so tập giá trị mọi enum với ràng buộc `CHECK` trong `V1__baseline.sql` (đọc file SQL, tìm `CHECK (... IN (...))`) | `ddl-auto: validate` không kiểm `CHECK`; enum lệch chỉ vỡ khi chạy thật |
| `P0.6` | Thêm `ErrorCode`: `TRIP_TOO_LONG` (422) · `INVALID_TIME_RANGE` (400) · `REORDER_SET_MISMATCH` (422) · `DAYS_HAVE_ACTIVITIES` (422) · `CURRENCY_MISMATCH` (422) · `TRIP_NOT_ENDED` (409) · `RATE_LIMITED` (429) · `UNDO_WINDOW_CLOSED` (409) | Dùng dần ở các giai đoạn sau; khai báo một lần cho gọn |

**Xong khi:** `docker compose up -d` → ứng dụng khởi động với `.env`, bảng `flyway_schema_history` có dòng baseline, `grep -r "AIza" backend/src` không ra gì, test enum ↔ `CHECK` xanh.

---

### `P1` — Chuyến đi (3 ngày)

| # | Việc | Luật |
|---|---|---|
| `P1.1` | `V2__trip_profile.sql`: thêm vào `trips` các cột `travel_style VARCHAR(16)`, `budget_preference VARCHAR(16)`, `preferences_json JSONB NOT NULL DEFAULT '[]'`, `group_profile JSONB NOT NULL DEFAULT '{}'`, kèm `CHECK` giống `user_preferences`. Đổi `uq_itinerary_days_trip_date` thành `UNIQUE (trip_id, date) DEFERRABLE INITIALLY DEFERRED` | G3 · `D8` |
| `P1.2` | `createTrip` lưu sở thích theo chuyến. Trường nào không gửi lên thì lấy từ `user_preferences`. Người dùng chưa có `user_preferences` thì tạo luôn từ chuyến này | `FR-007` · `D8` |
| `P1.3` | `GET`/`PUT /api/me/preferences` | `FR-007` |
| `P1.4` | Từ chối chuyến dài quá 30 ngày → `422 TRIP_TOO_LONG`, áp dụng cả khi tạo lẫn khi sửa | `FR-104` · `BR-201` |
| `P1.5` | `PUT /api/trips/{id}` nhận thêm `startDate`, `endDate`. Ngày thứ *i* luôn có `date = startDate + (i−1)`. Chuyến dài ra thì sinh thêm ngày cuối; ngắn lại thì xoá các ngày cuối. **Nếu ngày sắp xoá còn hoạt động**: trả `422 DAYS_HAVE_ACTIVITIES` kèm danh sách ngày đó, trừ khi gửi `confirmDropDays=true`. Tất cả trong một giao dịch | `FR-106` · `BR-203`. Ràng buộc `DEFERRABLE` ở `P1.1` để dời ngày không vấp `UNIQUE` giữa chừng |
| `P1.6` | **Giai đoạn chuyến** `phase`: `BEFORE` / `DURING` / `AFTER`, suy ra từ ngày hôm nay **theo múi giờ của điểm đến** (`destinations.timezone`), không lưu cột. Có `phase_override` (cột đã có) thì dùng nó, trả kèm `phaseSource: AUTO \| MANUAL`. Thêm `PATCH /api/trips/{id}/phase` để đặt tay hoặc xoá đặt tay | `FR-1001` · `FR-1002`. Cần cho `P3.6` và `P7.3` |
| `P1.7` | `GET /api/trips?status=upcoming\|ongoing\|past` lọc theo `phase` | `FR-107` |
| `P1.8` | `planningProgress` = số ngày có ≥ 2 hoạt động ÷ tổng số ngày, tính ở service, trả trong danh sách và chi tiết chuyến | `FR-108` · `BR-207` |
| `P1.9` | 🆕 **Thông tin nhóm đi**: record `GroupProfile` ánh xạ cột `group_profile`. Gửi lên khi tạo và sửa chuyến, có validate (xem §6.1) | Đã duyệt B2 |
| `P1.10` | Test: 30 ngày (biên 30/31) · rút ngắn chuyến khi ngày cuối còn hoạt động · dời chuyến 1 ngày không vỡ `UNIQUE` · `phase` theo múi giờ (chuyến bắt đầu "hôm nay" ở Tokyo nhưng vẫn là "hôm qua" ở UTC) · sở thích lấy mặc định đúng | |

**Xong khi:** tạo chuyến kèm `travelStyle` + `groupProfile` → `GET` thấy đủ hai trường; rút chuyến 4 ngày xuống 3 ngày khi ngày 4 có hoạt động → `422`, gửi kèm `confirmDropDays=true` → còn 3 ngày; `GET /api/trips?status=upcoming` không chứa chuyến đã qua.

---

### `P2` — Lịch trình (1,5 ngày)

| # | Việc | Luật |
|---|---|---|
| `P2.1` | Thêm và sửa hoạt động: `endTime < startTime` → `400 INVALID_TIME_RANGE` | G5 · `BR-206` |
| `P2.2` | `reorder`: danh sách có mã trùng → `422 REORDER_SET_MISMATCH`. Gộp chung với ca thiếu/thừa mã đang có. Đổi mã lỗi từ `VALIDATION_ERROR` sang mã này | G6 · `BR-205` |
| `P2.3` | `getItinerary` dùng `findByIdAndUserId` như mọi chỗ khác → không phải chủ thì `404` | G7 · `BR-104` |
| `P2.4` | 🆕 `V3__activity_cost_source.sql`: thêm `estimated_cost_source VARCHAR(12)` với `CHECK IN ('USER','PRICE_LEVEL','AI')`, cho phép `NULL` | Để giao diện và AI nói rõ con số ước tính lấy từ đâu |
| `P2.5` | 🆕 `CostEstimator` — **gợi ý chi phí ước tính** (xem §6.2) | Đã duyệt B1 |
| `P2.6` | 🆕 `GET /api/trips/{tripId}/cost-estimate?placeId=&activityType=` trả `{ min, max, suggested, currency, basis, source }` hoặc `{ suggested: null, reason }`. Frontend gọi khi người dùng chọn địa điểm, điền sẵn vào ô chi phí, người dùng sửa được. Lưu hoạt động với `estimatedCostSource = PRICE_LEVEL` nếu giữ nguyên số gợi ý, `USER` nếu đã sửa | Server **không** tự điền ngầm |
| `P2.7` | Test `P2.1`→`P2.3`; test `CostEstimator`: mỗi mức giá × số người, không có mức giá, tiền tệ chưa có bảng giá | |

**Xong khi:** `reorder` với `[1,1]` trả `422` (không còn `500`); người B gọi lịch trình của người A nhận `404`; hỏi gợi ý chi phí cho một quán `price_level = 2`, 2 người, tiền VND → nhận khoảng giá và chuỗi `basis` đọc được.

---

### `P3` — Ngân sách và chi tiêu (2,5 ngày)

| # | Việc | Luật |
|---|---|---|
| `P3.1` | `ExpenseEntity` + `ExpenseRepository`. Bảng đã có ở `V1`. Tạm để `paid_by = NULL` vì chưa làm chia tiền (`P9`) | |
| `P3.2` | `POST`/`GET /api/trips/{id}/expenses` (lọc `category`, `from`, `to`) · `PUT`/`DELETE /api/expenses/{id}`. Kiểm: `amount ≥ 0`; `currency` phải trùng tiền của chuyến (`D3`); `activityId` nếu có phải thuộc chuyến này. **Cho phép ngày chi trước ngày đi** (đặt phòng, mua vé từ sớm) | `FR-403` · `FR-408` · `BR-401` |
| `P3.3` | `BudgetServiceImpl.getTripBudgetSummary`: `estimatedTotal` = tổng `estimated_cost` của hoạt động **trừ hoạt động `SKIPPED`**; `actualTotal` = tổng `expenses.amount`; `remaining` = `budget − max(estimatedTotal, actualTotal)` | `FR-402` · `BR-402` · `FR-1105` |
| `P3.4` | `byCategory`: ước tính và thực chi theo từng hạng mục. Ánh xạ loại hoạt động sang hạng mục: `SIGHTSEEING→ACTIVITIES` · `FOOD→FOOD` · `TRANSPORT→TRANSPORTATION` · `ACCOMMODATION→ACCOMMODATION` · `REST/OTHER→OTHER` | `FR-404` |
| `P3.5` | `warningLevel`: `NONE` (≤ 90%) · `NEAR_LIMIT` (> 90%) · `OVER` (> 100%), tính trên phần lớn hơn giữa ước tính và thực chi. `budget = NULL` → `NONE`, `remaining = NULL` | `FR-406` · `BR-403` |
| `P3.6` | 🆕 **Hôm nay còn tiêu được bao nhiêu**: khối `daily` trong phản hồi ngân sách (xem §6.3) | Đã duyệt B1 |
| `P3.7` | `GET /api/trips/{id}/budget` trả cả tổng hợp lẫn khối `daily` | |
| `P3.8` | Test: ba mốc 89% / 91% / 101% · không có ngân sách · hoạt động `SKIPPED` không bị cộng · chi tiêu khác mã tiền bị từ chối · khối `daily`: trước chuyến / ngày đầu / ngày cuối / sau chuyến / đã tiêu lố / thời điểm sát nửa đêm ở múi giờ điểm đến | |

**Xong khi:** chuyến có ngân sách 8tr, ước tính 7,35tr, thực chi 2,1tr → `remaining = 650000`, `warningLevel = NEAR_LIMIT`. Cùng ngân sách 8tr, đang ở ngày 2 của chuyến 4 ngày, trước hôm nay đã chi 2tr, hôm nay chi 500k → `allowance = 2000000` ((8tr − 2tr) ÷ 3 ngày còn lại), `todayLeft = 1500000`.

> 🏁 **Mốc M1.** Đến đây toàn bộ nghiệp vụ chạy được mà **chưa cần AI**.

---

### `P4` — Thời tiết (2 ngày)

| # | Việc | Luật |
|---|---|---|
| `P4.1` | `OpenMeteoClient` dùng `WebClient`, timeout 5 giây, thử lại 1 lần. Dự báo: `api.open-meteo.com/v1/forecast` (tối đa 16 ngày). Khí hậu: `archive-api.open-meteo.com/v1/archive`, **khác tên miền**, thêm vào cấu hình | `QĐ-07` |
| `P4.2` | `WeatherService.getTripWeather`: ngày trong 16 ngày tới → `FORECAST`; xa hơn → trung bình cùng ngày của 3 năm trước, nhãn `CLIMATE_NORMAL`. Mỗi ngày trả `tempMin`, `tempMax`, `precipitationProbability` (chỉ có với dự báo), `precipitationMm`, `weatherCode`, `source` | `FR-501`→`503` · `BR-510` |
| `P4.3` | Đệm Redis: khoá `weather:{lat}:{lng}:{date}:{source}`, TTL 3 giờ cho dự báo, 7 ngày cho khí hậu | |
| `P4.4` | Open-Meteo lỗi → `200` kèm `days: []` và `reason: "WEATHER_UNAVAILABLE"`, **không ném lỗi** | `FR-505` · `QĐ-11` |
| `P4.5` | `GET /api/trips/{id}/weather` | |
| `P4.6` | Test: ngày thứ 16 / thứ 17 nhận nhãn khác nhau · client lỗi vẫn `200` · đệm trúng thì không gọi ra ngoài | |

**Xong khi:** chuyến bắt đầu sau 3 ngày trả `FORECAST`, chuyến sau 2 tháng trả `CLIMATE_NORMAL`, rút mạng thì vẫn `200` với `days: []`.

---

### `P5` — Trợ lý AI: nền tảng (6 ngày)

| # | Việc | Luật |
|---|---|---|
| `P5.0` | **ADR** `docs/ADR-001-spring-ai.md`: ghim version Spring AI 1.x (BOM `spring-ai-bom`), starter `spring-ai-starter-model-openai`. Cấu hình Gemini qua lớp tương thích OpenAI: `base-url: https://generativelanguage.googleapis.com/v1beta/openai`, `chat.completions-path: /chat/completions`, model theo `D6`. **Gọi thử một câu và một tool call thật**, chép phản hồi vào ADR | `D1` · `D6`. Lớp tương thích bỏ qua tham số lạ mà không báo lỗi |
| `P5.1` | Tắt `open-in-view`. Sửa các chỗ lazy-load trong controller/DTO bằng `JOIN FETCH` hoặc map DTO ngay trong service. Chạy lại toàn bộ test | G10 · làm **trước** khi có SSE |
| `P5.2` | Entity + repository: `ConversationEntity`, `MessageEntity`, `AiToolExecutionEntity` (bảng đã có ở `V1`) | |
| `P5.3` | `ConversationService`: tạo/nạp hội thoại; nạp N lượt gần nhất **không cắt lẻ một cặp tool call ↔ kết quả**; lưu `token_usage` | `FR-607` |
| `P5.4` | `userId` và `tripId` truyền vào công cụ qua `ToolContext`, dựng từ `SecurityContext`. **Không** có trong tham số công cụ mà mô hình nhìn thấy | `QĐ-03` · `BR-501` |
| `P5.5` | Tám công cụ đọc, mỗi công cụ gọi lại **service đã có**: `get_current_trip` (kèm `phase`, sở thích, `groupProfile`) · `get_itinerary` · `get_user_preferences` (kèm địa điểm đã thích / đã chê từ `P7.3`) · `get_saved_places` · `calculate_trip_budget` (kèm khối `daily`) · `get_weather` · `search_places` (tối đa 5 kết quả × 8 trường) · `calculate_distance` | `FR-602` · `FR-603` · `BR-504` |
| `P5.6` | `ToolExecutionLogger`: ghi `ai_tool_executions` **cả khi lỗi hoặc quá hạn**, cắt bớt `result` nếu quá dài | `FR-901` · `BR-505` |
| `P5.7` | `AgentService`: tắt chế độ tự chạy công cụ của Spring AI (`internalToolExecutionEnabled(false)`), tự điều khiển vòng lặp bằng `ToolCallingManager` để **chèn được** giới hạn, nhật ký và sự kiện SSE. Trần: **8 vòng hoặc 60 giây**; chạm trần thì trả lời bằng dữ liệu đang có. Chặn gọi lặp cùng công cụ với cùng tham số trong một lượt | `FR-608` · `BR-502` · `BR-503` · `D11` |
| `P5.8` | Lời nhắc hệ thống dựng lại mỗi lượt: tóm tắt chuyến · **ngày hôm nay theo múi giờ điểm đến** · trả lời tiếng Việt ngắn gọn · cấm bịa địa điểm · luôn nêu nhãn nguồn thời tiết · muốn đổi lịch thì phải gọi công cụ đề xuất · 🆕 ràng buộc của nhóm đi (xem §6.1) | `ADS-21` §5 |
| `P5.9` | `POST /api/trips/{id}/ai/chat` trả `SseEmitter`. Kiểm sở hữu **trước** khi mở luồng. Sự kiện: `tool_start` · `tool_end` (`ms`, `status`) · `token` · `proposal` · `done` · `error`. Phần SSE ở controller, mỗi lần gọi công cụ một giao dịch ngắn, **không** bọc cả luồng trong `@Transactional` | `FR-605` · `FR-606` |
| `P5.10` | `GET /api/trips/{id}/ai/conversations` · `GET /api/conversations/{id}/messages` | `FR-607` |
| `P5.11` | Giới hạn AI 20 lượt/giờ/người bằng Redis, `INCR` + `EXPIRE` gói trong **một script Lua**. Vượt → `429 RATE_LIMITED` kèm `retryAfterSeconds` | `BR-601` |
| `P5.12` | Test bảo mật: người A hỏi *"đọc chuyến số 42 giúp tôi"* (chuyến của B) → công cụ vẫn chỉ đọc chuyến của A. Test vòng lặp dừng ở vòng 8 (giả lập model luôn gọi công cụ) | `BR-103` |

**Xong khi:** `curl -N` gửi "ngày 2 của tôi có gì, trời thế nào?" → thấy `tool_start get_itinerary` → `tool_end` → `tool_start get_weather` → `tool_end` → các `token` → `done`; bảng `ai_tool_executions` có đúng 2 dòng `OK`.

---

### `P6` — Đề xuất, duyệt, hoàn tác, giải trình, sinh lịch trình (7 ngày)

Đây là **lõi đề tài**. `PLAN.md` §9 cấm cắt `P6.1`→`P6.6` và `P6.11`.

| # | Việc | Luật |
|---|---|---|
| `P6.1` | `AiProposalEntity`. Các thao tác `Add` / `Remove` / `Update` / `Reorder` khai báo bằng **sealed interface** (Java 21) để trình biên dịch bắt nhánh thiếu | |
| `P6.2` | Công cụ `propose_itinerary_changes`: **chỉ ghi `ai_proposals`**, không chạm `activities`. Validate từng thao tác; tính `estimated_cost_delta` (hoạt động thêm mới không có chi phí thì lấy từ `CostEstimator`, nguồn `AI`) và `travel_time_delta` (`DistanceService`); lưu `evidence_json` gồm công cụ đã chạy và **ứng viên bị loại kèm lý do**; `expires_at` = tạo + 30 phút | `QĐ-01` · `FR-701`→`703` |
| `P6.3` | `GET /api/proposals/{id}`: trả đủ dữ liệu để vẽ thẻ so sánh trước/sau | `FR-704` |
| `P6.4` | `POST /api/trips/{id}/ai/apply` **chỉ nhận `proposalId`**. Bốn điều kiểm: đúng chuyến · đúng người (`404`) · đang `PENDING` (`409`) · chưa hết hạn (`409`). Kiểm lại từng thao tác với dữ liệu hiện tại (hoạt động đã bị xoá thì bỏ qua và báo trong `skipped`). Một giao dịch: ghi `activities` với `created_by = AI` và `from_proposal_id`; địa điểm mới ghi `places` với `adopted_via = PROPOSAL`; đánh lại `order_index`; lưu `undo_json` (nhật ký thao tác nghịch đảo); `applied_at` | `FR-705`→`709` · `BR-506`→`508` |
| `P6.5` | `POST /api/proposals/{id}/reject` → `REJECTED`, giữ bản ghi | `FR-710` |
| `P6.6` | `@Scheduled` mỗi phút: `PENDING` quá `expires_at` → `EXPIRED` | `FR-707` |
| `P6.7` | `POST /api/proposals/{id}/undo` trong 10 phút sau `applied_at`: chạy `undo_json`. Hoạt động đã bị **sửa tay** sau khi áp dụng (`updated_at > applied_at`) thì **giữ nguyên** và liệt kê trong phản hồi. Địa điểm đã ghi vào `places` không rút lại. Chuyển sang `REVERTED`, không áp dụng lại được. Quá 10 phút → `409 UNDO_WINDOW_CLOSED` | `FR-1201`→`1207` |
| `P6.8` | `GET /api/activities/{id}/explanation`: hoạt động do AI tạo → đề xuất gốc + các dòng `ai_tool_executions` của lượt đó + ứng viên bị loại. Không truy được nguồn thì trả `{ known: false }`, **không dựng lời giải thích** | `FR-1208`→`1210` |
| `P6.9` | Công cụ `optimize_day_order`: láng giềng gần nhất + 2-opt **viết bằng Java**; giữ nguyên hoạt động có `startTime` cố định; cải thiện < 5% thì không đề xuất. Kết quả đi qua `propose_itinerary_changes` dạng `REORDER` | `FR-613` · `QĐ-09` |
| `P6.10` | Công cụ `propose_places`: ứng viên địa điểm kèm lý do, đính vào tin nhắn, **không** ghi `places` | `ADS-21` §2.1 |
| `P6.11` | **Sinh lịch trình cho chuyến mới**: `POST /api/trips/{id}/ai/generate` → `202` + `jobId`, chạy nền trên virtual thread; `GET .../ai/generate/{jobId}` trả `state`, `step`, `progress`. Bước **đối chiếu địa điểm**: tên mô hình đưa ra phải khớp một kết quả `search_places` có thật, không khớp thì **loại** và đếm số bị loại. Số hoạt động mỗi ngày theo `travelStyle` và `groupProfile` (§6.1). Chi phí lấy từ `CostEstimator`. **Không** chọn địa điểm người dùng đã chê (`P7.3`). Chỉ ghi thẳng khi chuyến **đang trống**, ngược lại trả `409` | `FR-609` · `FR-610` · `BR-509` |
| `P6.12` | Test: gửi thẳng danh sách thay đổi bị từ chối (`BR-506`) · bốn ca hỏng của `apply` · áp dụng hai lần → `409`, không nhân đôi · mô hình bịa địa điểm bị loại (`BR-509`) · hoàn tác khi đã sửa tay một hoạt động · hoàn tác quá 10 phút | |

**Xong khi:** 🏁 **Mốc M2.** "Ngày mai mưa thì đổi lịch giúp tôi" → trợ lý gọi ≥ 3 công cụ → có sự kiện `proposal` → `GET` đề xuất thấy khác biệt → `apply` đổi DB thật → `apply` lần hai nhận `409` → `undo` đưa lịch trình về như cũ → `explanation` của hoạt động AI thêm vào liệt kê đúng các công cụ đã chạy.

---

### `P7` — Tính năng mới (4,5 ngày)

#### `P7.1` 🆕 Checklist chuẩn bị (2 ngày)

| # | Việc |
|---|---|
| `P7.1.1` | `V5__checklist.sql`: bảng `trip_checklist_items` (xem §6.4) |
| `P7.1.2` | CRUD: `GET /api/trips/{id}/checklist?kind=PACK\|TODO` · `POST /api/trips/{id}/checklist` (một hoặc nhiều mục) · `PATCH /api/checklist-items/{id}` (sửa tên, tick xong) · `DELETE /api/checklist-items/{id}` |
| `P7.1.3` | `ChecklistSuggester`: **bộ luật bằng mã** sinh ứng viên từ thời tiết, loại hoạt động, độ dài chuyến, điểm đến trong nước hay nước ngoài, thông tin nhóm. Mỗi ứng viên có `reason` truy được về dữ liệu (xem §6.4) |
| `P7.1.4` | `GET /api/trips/{id}/checklist/suggestions`: trả ứng viên, **bỏ những mục đã có** (so tên không phân biệt hoa thường). Không ghi DB. Người dùng chọn rồi gọi `POST` ở `P7.1.2` với `source = SUGGESTED` |
| `P7.1.5` | Công cụ AI `suggest_checklist`: gọi `ChecklistSuggester`, mô hình được bổ sung tối đa 5 mục **có lý do** dựa trên lịch trình; ứng viên đính vào tin nhắn giống `propose_places`, người dùng bấm chọn mới ghi (`D9`), ghi với `source = AI` |
| `P7.1.6` | Test: ngày dự báo mưa ≥ 50% → có "Áo mưa / ô"; điểm đến nước ngoài → có "Hộ chiếu"; có trẻ nhỏ → có mục cho trẻ; mục đã có không bị gợi ý lại |

**Xong khi:** chuyến Đà Nẵng có một ngày dự báo mưa và một hoạt động đi biển → gợi ý có "Áo mưa" (lý do: *Ngày 2 dự báo mưa 70%*) và "Đồ bơi" (lý do: *Ngày 1 có Biển Mỹ Khê*); chọn 2 mục → `GET checklist` thấy 2 mục.

#### `P7.2` 🆕 Xuất lịch trình `.ics` (1 ngày)

| # | Việc |
|---|---|
| `P7.2.1` | `IcsExporter` tự viết theo RFC 5545 (xem §6.5), không cần thư viện |
| `P7.2.2` | `GET /api/trips/{id}/export.ics` → `Content-Type: text/calendar; charset=utf-8`, `Content-Disposition: attachment; filename="<ten-chuyen>.ics"` |
| `P7.2.3` | Test: dòng dài hơn 75 byte được gập đúng (kể cả chữ tiếng Việt nhiều byte) · ký tự `,` `;` `\` và xuống dòng được thoát · giờ quy đổi đúng sang UTC theo múi giờ điểm đến · hoạt động không có giờ gom thành một sự kiện cả ngày |
| `P7.2.4` | PDF: theo `D10` làm ở frontend. Ghi một dòng vào kế hoạch frontend |

**Xong khi:** tải file của một chuyến 3 ngày, nhập vào Google Calendar → đúng giờ địa phương, tiếng Việt không lỗi font; nhập lại lần hai **cập nhật** sự kiện chứ không nhân đôi (`UID` ổn định).

#### `P7.3` 🆕 Đánh giá địa điểm sau chuyến (1 ngày)

| # | Việc |
|---|---|
| `P7.3.1` | `V6__place_ratings.sql`: bảng `place_ratings` (xem §6.6) |
| `P7.3.2` | `GET /api/trips/{id}/review/places`: các địa điểm có trong lịch trình chuyến này, **trừ hoạt động `SKIPPED`**, kèm đánh giá hiện tại nếu có |
| `P7.3.3` | `PUT /api/trips/{id}/places/{placeId}/rating` `{ verdict: LIKE \| DISLIKE, note? }` · `DELETE` cùng đường dẫn. Chỉ cho phép khi `phase = AFTER`, ngược lại `409 TRIP_NOT_ENDED`. Địa điểm phải nằm trong lịch trình của chuyến |
| `P7.3.4` | `GET /api/me/place-ratings` |
| `P7.3.5` | AI dùng đánh giá **bằng mã**, không nhờ mô hình tự nhớ: `search_places` và `P6.11` **lọc bỏ** địa điểm bị `DISLIKE` theo `(provider, external_id)`; địa điểm `LIKE` được gắn cờ `previouslyLiked: true`; `get_user_preferences` trả tối đa 10 địa điểm thích và 10 địa điểm chê gần nhất |
| `P7.3.6` | Test: đánh giá khi chuyến chưa kết thúc → `409` · `search_places` không trả địa điểm đã chê · đánh giá lại thì ghi đè |

**Xong khi:** chê "Quán A" ở chuyến Đà Nẵng tháng 10 → tạo chuyến Đà Nẵng mới và hỏi "gợi ý quán ăn" → "Quán A" không xuất hiện, kể cả khi Google trả nó ở vị trí đầu.

#### `P7.4` 🆕 Nhân bản chuyến cũ (0,5 ngày)

| # | Việc |
|---|---|
| `P7.4.1` | `POST /api/trips/{id}/duplicate` `{ name?, startDate }`. Chuyến mới có cùng số ngày, cùng điểm đến, ngân sách, sở thích, `groupProfile` |
| `P7.4.2` | **Sao chép**: ngày (kèm `note`) · hoạt động (tên, địa điểm, loại, giờ, chi phí ước tính, phương tiện, ghi chú, thứ tự) · checklist (đặt lại `is_done = false`, `due_date` dời theo ngày đi mới). **Đặt lại**: `status = PLANNED`, `actual_*`, `skip_reason` về rỗng, `from_proposal_id = NULL`, `created_by = USER`. **Không sao chép**: chi tiêu, hội thoại, đề xuất, đánh giá |
| `P7.4.3` | **Hoạt động `SKIPPED` ở chuyến cũ vẫn được sao chép**, kèm cờ `wasSkipped` trong phản hồi để giao diện hỏi người dùng có muốn giữ không |
| `P7.4.4` | Test: chuyến mới độc lập với chuyến cũ (sửa một bên không ảnh hưởng bên kia) · không mang theo chi tiêu · giới hạn 30 ngày vẫn đúng |

**Xong khi:** nhân bản chuyến Đà Lạt 3 ngày sang tháng sau → có 3 ngày đủ hoạt động, checklist chưa tick mục nào, ngân sách thực chi bằng 0.

---

### `P8` — Quản trị, giới hạn tần suất, đóng gói (2 ngày)

| # | Việc | Luật |
|---|---|---|
| `P8.1` | `GET /api/admin/tool-executions`: lọc theo người dùng, công cụ, trạng thái, khoảng ngày; phân trang. `@PreAuthorize("hasRole('ADMIN')")`, **chỉ đọc** | `FR-903` · `FR-906` |
| `P8.2` | `GET /api/admin/ai-usage`: số lượt, tổng token, top công cụ, tỉ lệ lỗi theo ngày | `FR-904` |
| `P8.3` | `GET /api/admin/users` · `GET /api/admin/trips` ở mức tổng hợp | `FR-905` |
| `P8.4` | Giới hạn chung 120 request/phút/người (dùng lại script Lua của `P5.11`) | `NFR-07` |
| `P8.5` | `Dockerfile` cho backend; thêm service `backend` vào `docker-compose.yml` | `NFR-12` |
| `P8.6` | Test tích hợp bằng Testcontainers (Postgres + Redis) cho ba luồng phải có khi bảo vệ: `BR-103` · `BR-506` · `BR-509` | |
| `P8.7` | Rà Swagger: mọi điểm cuối có mô tả, có ví dụ phản hồi lỗi | `NFR-08` |

**Xong khi:** `docker compose up` dựng cả hệ thống bằng một lệnh; tài khoản `USER` gọi `/api/admin/**` nhận `403`; ba test tích hợp xanh.

---

### `P9` — Mở rộng nếu còn thời gian

Các mục ◐ ở `FEATURE-REVIEW.md` phần A. Chỉ đụng tới sau khi `P0`→`P8` xong **và** frontend đã chạy được.

| Việc | Mã | Ghi chú |
|---|---|---|
| Đánh dấu hoạt động xong / bỏ, ghi giờ thực tế, tính lệch lịch | `FR-1003`→`1007` | Cột đã có sẵn |
| Dải "Hôm nay" | `FR-1010` | |
| AI đề xuất dời lịch khi trễ | `FR-1008` · `FR-1009` | |
| Tab "Nhìn lại": tổng kết, ước tính so với thực tế theo hạng mục | `FR-1101`→`1105` | Dùng lại `BudgetService` |
| Chia tiền nhóm | `FR-1108`→`1110` | Bảng `trip_participants` đã có; điền `expenses.paid_by` |
| Ràng buộc cứng của chuyến | `FR-1211` · `FR-1212` | Cột `constraints_json` đã có |
| Đổi lịch khi thời tiết xấu | `FR-612` | Dùng lại `P6.2` + `P4` |
| Ghi chi tiêu bằng câu chữ (`parse_expense`) | `ADS-21` | |
| Chấm điểm ứng viên bằng mã (`rank_candidates`) | `ADS-21` | |
| Cho đánh giá địa điểm ngay trong lúc đang đi | mở rộng `P7.3` | |

---

## 6. Thiết kế chi tiết các tính năng mới

### 6.1 Thông tin nhóm đi

**Dữ liệu** (`trips.group_profile`, JSONB):

```json
{
  "children": 1,
  "seniors": 0,
  "dietary": ["VEGETARIAN"],
  "limitedMobility": false,
  "note": "Bé 4 tuổi, cần ngủ trưa"
}
```

| Trường | Kiểu | Validate |
|---|---|---|
| `children` | số trẻ dưới 12 tuổi | 0 ≤ x ≤ `travelers` |
| `seniors` | số người trên 65 tuổi | 0 ≤ x ≤ `travelers`; `children + seniors ≤ travelers` |
| `dietary` | tập con của `VEGETARIAN` · `VEGAN` · `HALAL` · `NO_SEAFOOD` · `NO_PORK` | không trùng |
| `limitedMobility` | có người đi lại khó khăn | |
| `note` | ghi chú tự do | ≤ 200 ký tự |

**Dùng ở đâu**, theo thứ tự ưu tiên **mã trước, mô hình sau**:

| Nơi dùng | Cách dùng |
|---|---|
| `search_places` | Tìm quán ăn mà `dietary` có `VEGETARIAN`/`VEGAN` → thêm "chay" vào truy vấn; `HALAL` → thêm "halal". Làm bằng mã, không trông vào mô hình |
| Sinh lịch trình `P6.11` | Số hoạt động tối đa mỗi ngày: `RELAXED` 3 · `BALANCED` 4 · `FAST_PACED` 6; **trừ 1** nếu có `children > 0`, `seniors > 0` hoặc `limitedMobility` |
| `calculate_distance` | Chặng đi bộ > 1 km mà nhóm có trẻ nhỏ / người già / người đi lại khó → trả kèm `warning: "LONG_WALK"` |
| Checklist `P7.1` | Có trẻ: đồ dùng cho trẻ, thuốc hạ sốt trẻ em · Có người già: thuốc dùng hằng ngày |
| Lời nhắc hệ thống `P5.8` | Một dòng tóm tắt: *"Nhóm 4 người, 1 trẻ nhỏ, 1 người ăn chay. Tránh lịch dày và chặng đi bộ dài."* |

### 6.2 Gợi ý chi phí ước tính

**Đầu vào:** `place.price_level` (0–4, đã lưu khi lấy từ Google), loại hoạt động, `travelers`, tiền của chuyến.

**Bảng giá theo người**, đặt trong `application.yml` để sửa không cần build lại. Bắt đầu với VND:

| `price_level` | Nghĩa (Google) | `FOOD` (VND/người) | Loại khác (VND/người) |
|---|---|---|---|
| 0 | Miễn phí | 0 | 0 |
| 1 | Rẻ | 30k – 100k | 0 – 100k |
| 2 | Vừa phải | 100k – 300k | 100k – 300k |
| 3 | Đắt | 300k – 700k | 300k – 800k |
| 4 | Rất đắt | 700k – 2tr | 800k – 3tr |

**Quy tắc:**

1. `suggested` = điểm giữa khoảng × `travelers`, làm tròn **xuống** tới nghìn đồng.
2. **Không có `price_level` → không đoán.** Trả `suggested: null, reason: "NO_PRICE_DATA"`. Đúng nguyên tắc "không biết thì nói là không biết".
3. Tiền của chuyến chưa có bảng giá (ví dụ JPY) → `reason: "UNSUPPORTED_CURRENCY"`. Bổ sung bảng USD nếu có chuyến nước ngoài để demo.
4. `basis` là chuỗi đọc được, giao diện hiện nguyên văn: *"Mức giá $$ trên Google · 100k–300k/người × 2 người"*.
5. Mọi con số gợi ý lưu kèm `estimated_cost_source`. Khi bảo vệ, chỉ ra được con số nào người dùng tự nhập, con số nào hệ thống gợi ý, con số nào AI đặt.

### 6.3 Hôm nay còn tiêu được bao nhiêu

"Hôm nay" luôn tính **theo múi giờ của điểm đến**.

| Ký hiệu | Nghĩa |
|---|---|
| `B` | Ngân sách chuyến |
| `S_before` | Tổng chi có `expense_date` **trước hôm nay**, gồm cả khoản chi trước ngày đi |
| `S_today` | Tổng chi của hôm nay |
| `N` | Số ngày còn lại **tính cả hôm nay** = `end_date − hôm nay + 1` |

```
allowance = max(0, (B − S_before) ÷ N)      ← cố định suốt cả ngày, không nhảy theo từng khoản chi
todayLeft = allowance − S_today              ← có thể âm
```

Lấy `S_before` thay vì tổng đã chi để **con số `allowance` đứng yên trong ngày**. Nếu dùng tổng đã chi thì mỗi lần ghi một khoản, hạn mức của chính hôm nay lại co lại. Người dùng sẽ thấy khó hiểu.

| Tình huống | Phản hồi |
|---|---|
| `phase = BEFORE` | `mode: "PLAN"`, `allowance = (B − tổng đã chi trước) ÷ tổng số ngày`: "bình quân mỗi ngày được tiêu" |
| `phase = DURING` | `mode: "TODAY"`, đủ `allowance`, `spentToday`, `todayLeft`, `status` |
| `phase = AFTER` | `daily: null`, `reason: "TRIP_ENDED"` |
| `B = NULL` | `daily: null`, `reason: "NO_BUDGET"` |
| `B − S_before ≤ 0` | `allowance = 0`, `status: "OVER"`, kèm `overBy` |

`status`: `ON_TRACK` (`S_today` ≤ 90% `allowance`) · `NEAR` (> 90%) · `OVER` (> 100%).

Thêm `averageSpentPerDay` = `S_before` ÷ số ngày đã đi, để giao diện hiện được *"Mấy hôm trước bạn tiêu trung bình 1,8tr/ngày"*.

### 6.4 Checklist chuẩn bị

**Bảng `trip_checklist_items`:**

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `id` | `BIGINT` identity | |
| `trip_id` | `BIGINT` FK → `trips` `ON DELETE CASCADE` | |
| `kind` | `VARCHAR(8)` | `PACK` (đồ mang theo) · `TODO` (việc cần làm) |
| `title` | `VARCHAR(120) NOT NULL` | |
| `category` | `VARCHAR(16)` | `CLOTHING` · `TOILETRIES` · `HEALTH` · `DOCUMENTS` · `ELECTRONICS` · `MONEY` · `BOOKING` · `KIDS` · `OTHER` |
| `due_date` | `DATE` NULL | Chỉ cho `TODO`, ví dụ check-in online trước ngày bay |
| `is_done` | `BOOLEAN NOT NULL DEFAULT false` | |
| `source` | `VARCHAR(10)` | `USER` · `SUGGESTED` · `AI` |
| `reason` | `VARCHAR(200)` | Vì sao được gợi ý, ví dụ *"Ngày 2 dự báo mưa 70%"* |
| `order_index` | `SMALLINT` | |
| `created_at`, `updated_at` | `TIMESTAMPTZ` | |

Chỉ mục duy nhất `(trip_id, kind, lower(title))` để không thêm trùng một mục hai lần.

**Bộ luật `ChecklistSuggester`** (khởi đầu, dễ thêm sau):

| Điều kiện | Gợi ý | Loại |
|---|---|---|
| Có ngày `precipitationProbability ≥ 50%` hoặc khí hậu `precipitationMm ≥ 5` | Áo mưa / ô | `PACK` |
| Có ngày `tempMax ≥ 32°C` | Kem chống nắng, mũ, kính râm | `PACK` |
| Có ngày `tempMin ≤ 15°C` | Áo khoác ấm | `PACK` |
| Có hoạt động ở bãi biển (loại địa điểm `beach` hoặc tên chứa "biển", "beach") | Đồ bơi | `PACK` |
| Chuyến ≥ 4 ngày | Sạc dự phòng, túi giặt đồ | `PACK` |
| Điểm đến ngoài Việt Nam (`destinations.country`) | Hộ chiếu, đổi ngoại tệ, SIM/eSIM, ổ cắm chuyển đổi | `PACK` + `TODO` |
| Có hoạt động `TRANSPORT` tên chứa "bay", "sân bay", "flight" | Check-in online, `due_date` = ngày đó − 1 | `TODO` |
| Có hoạt động `ACCOMMODATION` | Xác nhận lại đặt phòng, `due_date` = ngày đi − 2 | `TODO` |
| `children > 0` | Đồ dùng cho trẻ, thuốc hạ sốt trẻ em | `PACK` |
| `seniors > 0` | Thuốc dùng hằng ngày | `PACK` |
| Luôn có | Giấy tờ tùy thân, thuốc cá nhân, sạc điện thoại | `PACK` |

Thời tiết lỗi thì **bỏ qua các luật thời tiết**, trả thêm `skippedRules: ["WEATHER"]`. Không đoán.

### 6.5 Xuất `.ics`

- Mỗi hoạt động **có `startTime`** → một `VEVENT`. Giờ địa phương quy đổi sang UTC (`DTSTART:20261020T013000Z`) theo `destinations.timezone`, nên không cần khối `VTIMEZONE`. Không có `endTime` → mặc định 1 giờ.
- Hoạt động **không có giờ** của cùng một ngày gom thành **một** sự kiện cả ngày (`DTSTART;VALUE=DATE`), tên *"Ngày 2 · Chợ Hàn → Bảo tàng Chăm → Cầu Rồng"*. Như vậy lịch không bị rải đầy sự kiện cả ngày.
- `UID:activity-{id}@tripmind` và `UID:day-{dayId}@tripmind`, cố định, nên nhập lại thì **cập nhật** chứ không nhân đôi.
- `LOCATION` = địa chỉ; `GEO` = `lat;lng`; `DESCRIPTION` = ghi chú + chi phí ước tính.
- Xuống dòng bằng `CRLF`. Gập dòng ở **75 byte** (đếm byte UTF-8, không đếm ký tự). Không cắt giữa một ký tự nhiều byte. Thoát `\` `;` `,` và xuống dòng.

### 6.6 Đánh giá địa điểm

**Bảng `place_ratings`:**

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `id` | `BIGINT` identity | |
| `user_id` | FK → `users` `ON DELETE CASCADE` | |
| `place_id` | FK → `places` `ON DELETE CASCADE` | |
| `trip_id` | FK → `trips` `ON DELETE SET NULL` | Chuyến lúc đánh giá; xoá chuyến vẫn giữ đánh giá |
| `verdict` | `VARCHAR(8)` `CHECK IN ('LIKE','DISLIKE')` | |
| `note` | `VARCHAR(200)` | |
| `created_at`, `updated_at` | `TIMESTAMPTZ` | |

`UNIQUE (user_id, place_id)`: **mỗi người một ý kiến cho một chỗ**, đánh giá lại thì ghi đè. Người dùng không có hai ý kiến khác nhau về cùng một quán.

---

## 7. Điểm cuối mới

| Nhóm | Điểm cuối | Giai đoạn |
|---|---|---|
| Sở thích | `GET`/`PUT /api/me/preferences` | `P1` |
| Chuyến đi | `GET /api/trips?status=` · `PATCH /api/trips/{id}/phase` · `POST /api/trips/{id}/duplicate` | `P1` · `P7` |
| Chi phí | `GET /api/trips/{id}/cost-estimate` | `P2` |
| Ngân sách | `GET /api/trips/{id}/budget` · `POST`/`GET /api/trips/{id}/expenses` · `PUT`/`DELETE /api/expenses/{id}` | `P3` |
| Thời tiết | `GET /api/trips/{id}/weather` | `P4` |
| Trợ lý | `POST /api/trips/{id}/ai/chat` · `GET /api/trips/{id}/ai/conversations` · `GET /api/conversations/{id}/messages` | `P5` |
| Đề xuất | `GET /api/proposals/{id}` · `POST /api/trips/{id}/ai/apply` · `POST /api/proposals/{id}/reject` · `POST /api/proposals/{id}/undo` · `GET /api/activities/{id}/explanation` | `P6` |
| Sinh lịch trình | `POST /api/trips/{id}/ai/generate` · `GET /api/trips/{id}/ai/generate/{jobId}` | `P6` |
| Checklist | `GET`/`POST /api/trips/{id}/checklist` · `GET /api/trips/{id}/checklist/suggestions` · `PATCH`/`DELETE /api/checklist-items/{id}` | `P7` |
| Xuất file | `GET /api/trips/{id}/export.ics` | `P7` |
| Đánh giá | `GET /api/trips/{id}/review/places` · `PUT`/`DELETE /api/trips/{id}/places/{placeId}/rating` · `GET /api/me/place-ratings` | `P7` |
| Quản trị | `GET /api/admin/{tool-executions,ai-usage,users,trips}` | `P8` |

---

## 8. Công cụ AI cuối cùng

| Công cụ | Loại | Giai đoạn |
|---|---|---|
| `get_current_trip` · `get_itinerary` · `get_user_preferences` · `get_saved_places` · `calculate_trip_budget` · `get_weather` · `search_places` · `calculate_distance` | Đọc | `P5` |
| `optimize_day_order` | Đọc (tính bằng mã) | `P6` |
| `propose_itinerary_changes` | Ghi **chỉ vào `ai_proposals`** | `P6` |
| `propose_places` · `suggest_checklist` | Ứng viên đính vào tin nhắn, không ghi DB | `P6` · `P7` |

Không công cụ nào ghi thẳng vào `activities`, `expenses`, `places` hay `trip_checklist_items`. Ngoại lệ duy nhất là sinh lịch trình cho **chuyến đang trống** (`P6.11`).

---

## 9. Thời hạn và thứ tự cắt

Theo `PLAN.md`, 12 tuần tính từ 04/09/2026 kết thúc khoảng **27/11/2026**. Từ hôm nay còn khoảng **7,5 tuần**. Trong đó phải có cả frontend, deploy và báo cáo.

**Gợi ý lịch:**

| Tuần | Ngày | Việc |
|---|---|---|
| 1 | 05/10 – 11/10 | `P0` · `P1` · `P2` |
| 2 | 12/10 – 18/10 | `P3` · `P4` → 🏁 M1 |
| 3 | 19/10 – 25/10 | `P5` |
| 4 | 26/10 – 01/11 | `P6.1` → `P6.8` |
| 5 | 02/11 – 08/11 | `P6.9` → `P6.12` → 🏁 M2 · `P7` |
| 6 – 7 | 09/11 – 22/11 | Frontend · `P8` |
| 8 | 23/11 – 27/11 | Deploy · demo · báo cáo |

Frontend chỉ còn khoảng 2 tuần, **rất căng**. Trễ thì cắt theo thứ tự sau, từ trên xuống:

1. `P8.2`, `P8.3`: thống kê AI và danh sách người dùng/chuyến cho admin. Giữ `P8.1` (nhật ký công cụ)
2. `P7.1.5`: phần AI của checklist. Giữ bộ luật và CRUD
3. `P7.3.5`: AI lọc theo đánh giá. Giữ CRUD đánh giá
4. `P6.9`: tối ưu thứ tự trong ngày
5. `P4.2` nhánh khí hậu: chuyến xa hơn 16 ngày trả `reason: "TOO_FAR_FOR_FORECAST"`
6. `P6.8`: giải trình

**Không bao giờ cắt:** `P5` · `P6.1`→`P6.7` · `P6.11` · `P5.6` (nhật ký công cụ). Ba thứ này *là* đề tài.

---

## 10. Không làm

| Việc | Lý do |
|---|---|
| Refresh token, đăng xuất phía server | `D2` |
| Quy đổi tỷ giá | `D3` |
| Nhật ký cảm nhận, ảnh, nhập giọng nói, thói quen ước lượng | `D5` |
| PDF ở backend | `D10` |
| Kiểm tra lịch trình · Khoá hoạt động · Nơi lưu trú theo đêm · Danh sách chờ xếp lịch · Thời gian di chuyển thực (OSRM) | Không được chọn ở `FEATURE-REVIEW.md` phần B. Có thể đưa vào "Hướng phát triển" của báo cáo |
| Chuyến nhiều điểm đến · Cẩm nang điểm đến do AI viết · Cùng lập kế hoạch / chia sẻ link | `FEATURE-REVIEW.md` phần B3 |

---

## 11. Nhật ký

Mỗi lần xong một việc, thêm **một dòng** ở dưới cùng.

| Ngày | Mã | Ghi chú |
|---|---|---|
| 05/10/2026 | — | Lập kế hoạch v1.0. Hiện trạng: xong giai đoạn 1–3 của `TODO.md` cũ, 21 test xanh |
| 06/10/2026 | `P0`→`P8` | Xong trên nhánh `feat/be-complete`, 82 test xanh. Chi tiết và chỗ khác kế hoạch: [BE-CHANGES.md](BE-CHANGES.md) |
