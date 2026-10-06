# BE-CHANGES — Những gì đã làm ở backend

**06/10/2026** · Nhánh `feat/be-complete` · Kế hoạch gốc: [BE-ROADMAP.md](BE-ROADMAP.md)

File này để đọc lại khi review: mỗi giai đoạn làm gì, API nào mới, chỗ nào khác kế hoạch, và việc gì còn phải làm tay.

---

## 1. Tóm tắt

| | Trước | Sau |
|---|---|---|
| Điểm cuối API | 26 | **64** |
| Test tự động | 21 (chỉ unit) | **82** (unit + tích hợp trên PostgreSQL 16 và Redis thật bằng Testcontainers) |
| Migration | chạy tay `schemas.sql` | Flyway `V1` → `V6` |
| Khoá API trong mã nguồn | có (Gemini, Places, JWT) | **không còn**, đọc từ `.env` |

Đã làm hết `P0` → `P8` của roadmap. `P9` (mở rộng) chưa làm. Commit trên nhánh `feat/be-complete`:

| Commit | Giai đoạn |
|---|---|
| `chore(P0): flyway baseline, secrets via .env, docker compose, error codes` | P0 |
| `feat(P1): trip preferences, group profile, 30-day limit, date resize, trip phase` | P1 |
| `feat(P2): itinerary validation fixes, adopt Google places, cost estimate from price level` | P2 |
| `feat(P3): expenses CRUD, budget summary with warnings and daily allowance` | P3 |
| `feat(P4): trip weather from Open-Meteo with forecast/climate labels and graceful failure` | P4 |
| `feat(P5): AI assistant over SSE with controlled tool loop, audit log, rate limit` | P5 |
| `feat(P6): AI proposals with apply/undo/explain, day-order optimizer, itinerary generation` | P6 |
| `feat(P7): checklist with rule-based suggestions, ics export, place ratings, trip duplication` | P7 |
| `feat(P8): read-only admin APIs, API rate limit filter, admin bootstrap, backend Dockerfile` | P8 |

---

## 2. Việc bạn phải làm tay

1. **Thu hồi khoá Google Places cũ** trên Google Cloud và tạo khoá mới. Khoá này vẫn nằm trong lịch sử git (commit `72d0fc3`). Hiện nó **vẫn còn dùng được**: mình đã dùng nó để chạy thử, đặt trong `.env` cục bộ (không commit).
2. **Tạo khoá Gemini mới.** Khoá cũ đã bị Google đánh dấu là lộ và chặn (`403 Your API key was reported as leaked`). Vì vậy **trợ lý AI chưa được chạy thử với mô hình thật**. Mọi test AI dùng một mô hình giả trả lời theo kịch bản.
3. **Kiểm tra lại tên model.** Mặc định đang là `gemini-3.8-flash`. Tên này lấy từ thông báo lỗi của chính API Gemini (`gemini-2.5-flash ... no longer available to new users. Please ... use models/gemini-3.8-flash`). Muốn đổi thì đặt `LLM_MODEL` trong `.env`.
4. Khi có khoá mới: thử một câu hỏi thật qua `POST /api/trips/{id}/ai/chat` để kiểm lớp tương thích OpenAI của Gemini. Rủi ro lớn nhất là định dạng tool call (mã `tool_call_id`) của Gemini khác OpenAI.

---

## 3. Cách chạy

```bash
cp .env.example .env          # điền DB_PASSWORD, JWT_SECRET, GOOGLE_PLACES_API_KEY, GEMINI_API_KEY
cd backend && mvn spring-boot:run           # hoặc: docker compose up -d --build
```

- **CSDL cũ** (đã chạy tay `schemas.sql`): Flyway tự nhận làm mốc V1 rồi chỉ chạy V2 → V6. Đã thử trên một bản sao DB `tripmind` của bạn: chạy đúng. Bản sao đã xoá sau khi thử; DB gốc chưa bị đụng tới.
- **CSDL trống**: Flyway chạy cả V1 → V6.
- **Thiếu `JWT_SECRET` hoặc ngắn hơn 32 ký tự** thì ứng dụng dừng ngay lúc khởi động, không chạy với khoá mặc định.
- **Không có `GEMINI_API_KEY`**: mọi thứ vẫn chạy, chỉ API trợ lý trả `503 AI_NOT_CONFIGURED`.
- **Tài khoản admin**: đăng ký như người dùng thường, đặt email vào `ADMIN_EMAILS`, khởi động lại.
- **Swagger**: `http://localhost:8088/swagger-ui.html`.

**Test:** `cd backend && mvn test` (cần Docker để Testcontainers dựng PostgreSQL và Redis).

---

## 4. Theo từng giai đoạn

### P0 — Dọn nền

- Gỡ mọi khoá khỏi `application.yml`. Spring đọc thẳng file `.env` qua `spring.config.import`. Có `.env.example`.
- Flyway: `V1__baseline.sql` là toàn bộ `schemas.sql` cũ (bỏ các câu `DROP`). File `schemas.sql` ở gốc giờ chỉ còn là lời trỏ sang thư mục migration; bản cũ trong `resources/db/` đã xoá.
- `docker-compose.yml`: postgres 16, redis 7, và service `backend`.
- Lỗi trả về có thêm `code` (giao diện rẽ nhánh theo mã) và `details`, đúng hình dạng ADS-30 §1.1. Thêm khoảng 20 mã lỗi.
- Test đối chiếu **mọi enum Java với ràng buộc `CHECK` trong migration**, vì `ddl-auto: validate` không kiểm phần này. Test này đã bắt được enum `ProposalStatus` thiếu `REVERTED`.

### P1 — Chuyến đi

- Sở thích lưu **theo chuyến** (V2). Trường nào bỏ trống thì lấy mặc định của tài khoản. Chuyến đầu tiên tự trở thành mặc định.
- 🆕 **Thông tin nhóm đi** (`groupProfile`): số trẻ nhỏ, người lớn tuổi, chế độ ăn, đi lại khó khăn. Có kiểm `trẻ + người già ≤ số người`.
- Từ chối chuyến dài quá 30 ngày (`422 TRIP_TOO_LONG`). Giới hạn đặt trong cấu hình.
- **Đổi ngày đi/về**: chỉ gửi ngày đi thì dời cả chuyến, giữ nguyên số ngày; gửi ngày về thì kéo dài hoặc rút ngắn. Rút ngắn mà ngày bị bỏ còn hoạt động thì trả `422 DAYS_HAVE_ACTIVITIES`, phải gửi `confirmDropDays=true`. Ràng buộc `UNIQUE (trip_id, date)` được hoãn tới lúc commit để dời ngày không vấp trùng.
- **Giai đoạn chuyến** `BEFORE / DURING / AFTER`, tính theo **múi giờ điểm đến**, đặt tay được qua `PUT /api/trips/{id}/phase`.
- `GET /api/trips?status=upcoming|ongoing|past`, `planningProgress` (%).
- API trả DTO thay vì entity, nhờ đó tắt được `open-in-view`.

### P2 — Lịch trình

- Sửa ba lỗi tìm thấy khi review: gửi mã trùng khi sắp lại gây `500` (giờ trả `422 REORDER_SET_MISMATCH`); chưa kiểm giờ kết thúc ≥ giờ bắt đầu; xem lịch trình của người khác trả `403` (giờ trả `404`).
- **Thêm kết quả tìm kiếm Google vào lịch trình hoặc danh sách đã lưu** bằng `placeExternalId`. Trước đây chỉ nhận mã trong DB, nên kết quả Google thực tế không dùng được. Kết quả tìm kiếm được đệm 1 giờ trong Redis để khi chọn không tốn thêm một lượt gọi Place Details.
- Chi phí bỏ trống giữ là `null` (chưa biết), không tự thành `0` (miễn phí) như trước.
- 🆕 **Gợi ý chi phí ước tính** `GET /api/trips/{id}/cost-estimate`: khoảng giá theo `price_level` × số người, kèm câu giải thích. Bảng giá VND/USD đặt trong cấu hình. Không có mức giá thì không đoán. Nguồn của mỗi con số lưu ở `estimated_cost_source` (V3).

### P3 — Ngân sách

- CRUD chi tiêu. Khác tiền của chuyến thì từ chối (`422 CURRENCY_MISMATCH`), vì không quy đổi tỷ giá. Cho ghi khoản chi trước ngày đi (đặt phòng từ sớm).
- Tổng hợp: ước tính so với thực chi theo 6 hạng mục; hoạt động đã bỏ không tính vào ước tính; cảnh báo 90% / 100%.
- 🆕 **Hôm nay còn tiêu được bao nhiêu**: hạn mức = (ngân sách − đã chi **trước hôm nay**) ÷ số ngày còn lại. Hạn mức đứng yên suốt ngày, ghi thêm khoản chi thì chỉ phần "còn lại hôm nay" giảm. Trước chuyến thì hiện bình quân mỗi ngày. "Hôm nay" theo múi giờ điểm đến.

### P4 — Thời tiết

- Open-Meteo, mỗi ngày gắn nhãn nguồn: `FORECAST` (tối đa 15 ngày tới), `CLIMATE_NORMAL` (trung bình 3 năm cùng kỳ), `OBSERVED` (ngày đã qua).
- Dịch vụ lỗi thì vẫn `200`, kèm `reason: WEATHER_UNAVAILABLE` hoặc `PARTIAL`. Kết quả đệm 3 giờ trong Redis. Không giữ kết nối DB trong lúc chờ API ngoài.

### P5 — Trợ lý AI

- Spring AI 1.0.9. Chỉ dùng module OpenAI, không dùng starter, để thiếu khoá thì ứng dụng vẫn khởi động. Gemini qua lớp tương thích OpenAI. Giữ Spring Boot 3.4 (quyết định D1).
- **Vòng lặp tự điều khiển**: tối đa 8 vòng hoặc 60 giây; chạm trần thì trả lời bằng dữ liệu đang có. Chặn gọi lặp cùng công cụ với cùng tham số. **Mọi lần chạy công cụ đều ghi nhật ký**, kể cả khi lỗi hoặc quá hạn.
- `userId` và `tripId` truyền vào công cụ qua `ToolContext`, không có trong tham số mà mô hình thấy. Test chứng minh: mô hình "xin" đọc chuyến của người khác vẫn chỉ nhận chuyến của người hỏi (BR-103).
- 8 công cụ đọc, cùng `propose_places` (thẻ địa điểm để người dùng chọn, không ghi DB).
- SSE: `tool_start`, `tool_end`, `token`, `proposal`, `places`, `checklist`, `done`, `error`. Mọi kiểm tra (sở hữu chuyến, đã cấu hình chưa, giới hạn 20 lượt/giờ) chạy **trước** khi mở luồng, nên lỗi trả đúng mã HTTP dạng JSON.
- Nhóm đi được dùng **bằng mã**: tìm quán ăn khi nhóm có người ăn chay thì tự thêm "chay" vào truy vấn; chặng đi bộ > 1 km với nhóm có trẻ nhỏ thì cảnh báo `LONG_WALK`.

### P6 — Đề xuất, duyệt, hoàn tác (lõi đề tài)

- `propose_itinerary_changes` (ADD, REMOVE, UPDATE, REORDER) **chỉ ghi `ai_proposals`**. Từng thao tác được kiểm ngay lúc dựng; thao tác hỏng bị loại kèm lý do. Địa điểm phải có thật: có trong DB hoặc vừa được `search_places` trả về. Mã do mô hình bịa ra bị loại `PLACE_NOT_VERIFIED` (BR-509). Mỗi đề xuất có tính chênh lệch chi phí và chênh lệch phút di chuyển.
- **Áp dụng** chỉ nhận `proposalId` (BR-506). Khoá dòng đề xuất nên bấm hai lần cùng lúc cũng không nhân đôi (BR-508). Kiểm đúng chuyến, đúng người, đang chờ, chưa hết 30 phút. Thao tác trỏ tới hoạt động đã mất thì bỏ qua và báo trong `skipped`; danh sách sắp lại lỗi thời thì huỷ cả đề xuất (`409 PROPOSAL_STALE`).
- **Hoàn tác** trong 10 phút, có bản xem trước nói đúng việc sắp xảy ra. Hoạt động người dùng **đã sửa tay** sau khi áp dụng thì **giữ nguyên** và báo lại; địa điểm đã ghi DB thì không rút lại. Chặn theo kiểu LIFO có nới: chỉ chặn khi một đề xuất áp dụng sau đụng cùng hoạt động.
- **Giải trình** `GET /api/activities/{id}/explanation`: đề xuất gốc, lý do, công cụ đã chạy, ứng viên bị loại. Không truy được nguồn thì trả `known: false`, không dựng lời giải thích.
- `optimize_day_order`: láng giềng gần nhất + 2-opt viết bằng Java. Giữ nguyên hoạt động đầu ngày, hoạt động có giờ cố định và hoạt động không có toạ độ; cải thiện dưới 5% thì không đề xuất.
- **Sinh lịch trình tự động** (chạy nền, có tiến trình). Mô hình chỉ đưa tên và truy vấn; mỗi chỗ phải tìm ra địa điểm thật có tên khớp ≥ 50% số từ, ngược lại bị loại và đếm lại. Số hoạt động mỗi ngày theo phong cách đi, giảm 1 nếu nhóm cần nhịp chậm. Kết quả ghi bằng **đúng luồng đề xuất → áp dụng**, nên hoạt động sinh ra cũng giải trình và hoàn tác được.
- Tác vụ chạy mỗi phút chuyển đề xuất quá hạn sang `EXPIRED`.

### P7 — Tính năng mới

- 🆕 **Checklist chuẩn bị** (V5): đồ mang theo (PACK) và việc cần làm (TODO, có hạn). Bộ luật bằng mã: mưa, nóng, lạnh, đi biển, chuyến bay, khách sạn, chuyến dài, đi nước ngoài, có trẻ nhỏ, người lớn tuổi. Mỗi gợi ý có lý do truy được, ví dụ *"Ngày 1 dự báo mưa 98%"*. Thời tiết lỗi thì báo `skippedRules: [WEATHER]`, không đoán. Ngưỡng đặt trong cấu hình. Công cụ AI `suggest_checklist` thêm tối đa 5 mục có lý do; người dùng chọn thì mới ghi.
- 🆕 **Xuất `.ics`**: hoạt động có giờ thành sự kiện (giờ đổi sang UTC theo múi giờ điểm đến); hoạt động không giờ gom thành một sự kiện cả ngày; `UID` cố định nên nhập lại thì cập nhật. Gập dòng 75 byte đúng chuẩn. PDF để frontend in (quyết định D10).
- 🆕 **Đánh giá địa điểm** (V6): thích / không thích, chỉ mở khi chuyến đã xong. `search_places` và bước sinh lịch trình **lọc bỏ chỗ đã chê bằng mã**. Chỗ đã thích được gắn cờ `previouslyLiked`.
- 🆕 **Nhân bản chuyến**: giữ hoạt động (đưa về trạng thái chưa làm) và checklist (bỏ tick, dời hạn theo ngày mới); không mang theo chi tiêu, hội thoại, đề xuất. Báo lại các hoạt động từng bị bỏ ở chuyến cũ.

### P8 — Quản trị và đóng gói

- 4 API quản trị **chỉ đọc**: nhật ký công cụ (lọc theo người, công cụ, trạng thái, ngày), thống kê AI (lượt, token, tỉ lệ lỗi, top công cụ, theo ngày), danh sách người dùng, danh sách chuyến ở mức tổng hợp (không lộ nội dung lịch trình).
- Giới hạn chung 120 request/phút/người. Bộ đếm Redis dùng `INCR` + `EXPIRE` trong một script Lua. Redis chết thì cho qua.
- `Dockerfile` cho backend.

---

## 5. Chỗ khác với BE-ROADMAP

| Roadmap | Thực tế | Vì sao |
|---|---|---|
| V4 checklist, V5 ratings | V4 `message_attachments`, V5 checklist, V6 ratings | Cần cột `attachments_json` cho tin nhắn trợ lý (đính đề xuất, thẻ địa điểm, checklist) |
| Hoàn tác `POST /api/proposals/{id}/undo` | `POST /api/trips/{id}/ai/undo` + `GET .../undo/{proposalId}/preview` | Theo đúng hợp đồng ADS-30 §10a |
| Đặt giai đoạn `PATCH /api/trips/{id}/phase` | `PUT` | Theo ADS-30 §10b |
| Thời tiết 2 nhãn | 3 nhãn (thêm `OBSERVED`) | Chuyến đã/đang đi có những ngày đã qua; gọi chúng là "dự báo" thì sai |
| Lịch sử hội thoại gồm cả các cặp tool call | Chỉ câu hỏi + câu trả lời cuối | Không bao giờ cắt lẻ một cặp tool call; kết quả công cụ vẫn còn đầy đủ trong nhật ký |
| — | Sinh lịch trình đi qua luồng đề xuất → áp dụng | Để hoạt động sinh tự động cũng giải trình và hoàn tác được (FR-1208) |
| — | Thêm `POST /api/me/saved-places` (lưu theo mã Google) | Trước đó không lưu được kết quả tìm kiếm chưa có trong DB |
| — | Đổi tiền của chuyến khi đã có chi tiêu thì bị chặn | Không quy đổi tỷ giá, nên cộng lẫn hai loại tiền là sai |

---

## 6. Kiểm thử

**82 test**, tất cả đều qua. Các nhóm chính:

| Nhóm | Kiểm gì |
|---|---|
| `TripApiTest` | sở thích, nhóm đi, 30 ngày, dời ngày trên Postgres thật, rút ngắn cần xác nhận, 404 khi không phải chủ, giai đoạn |
| `ItineraryApiTest` | mã trùng khi sắp lại → 422, nạp địa điểm Google, giờ sai, chi phí null |
| `BudgetApiTest`, `BudgetServiceImplTest` | ba mốc 89 / 91 / 101%, hạn mức theo ngày, sát nửa đêm theo múi giờ, khác tiền |
| `WeatherServiceTest` | biên ngày 16/17, dịch vụ lỗi một phần hoặc toàn bộ |
| `AiAssistantApiTest` | luồng SSE, **BR-103**, dừng ở vòng 8, chặn gọi lặp, lỗi trả trước khi mở luồng |
| `ProposalApiTest` | áp dụng hai lần, **BR-506 / 507 / 508 / 509**, hoàn tác giữ phần sửa tay, xem trước, hết cửa sổ 10 phút, LIFO, đề xuất lỗi thời, hết hạn |
| `GenerationApiTest` | địa điểm thật được giữ, địa điểm bịa bị loại, giải trình, chuyến không trống → 409 |
| `TripExtrasApiTest` | checklist, đánh giá lọc kết quả tìm kiếm, `.ics`, nhân bản |
| `AdminApiTest` | quản trị đọc được, người thường bị 403, giới hạn 120/phút |
| `EnumCheckConstraintTest` | enum Java khớp mọi `CHECK` trong migration |

**Chạy thử bằng tay với dịch vụ thật** (ứng dụng chạy trên bản sao DB của bạn):

- Flyway nhận DB cũ làm baseline, áp V2 → V6 thành công.
- Open-Meteo trả dự báo thật 3 ngày ở Đà Nẵng (mưa 98%).
- Google Places tìm được "Cầu Rồng" và nạp vào lịch trình với `adopted_via = ITINERARY`.
- Gợi ý chi phí cho quán mức giá $$, 2 người: 400k.
- Checklist gợi ý áo mưa (*"Ngày 1 dự báo mưa 98%"*) và đồ cho trẻ.
- File `.ics` đúng giờ UTC, tiếng Việt không lỗi.
- Trợ lý trả `503` khi chưa có khoá.
- Nhân bản chuyến, lọc chuyến sắp tới đều đúng.
- Log ứng dụng không có lỗi nào.
- Image Docker build được và khởi động được trên một DB trống (Flyway chạy V1 → V6).

**Chưa kiểm:** trợ lý với mô hình Gemini thật (xem §2).

---

## 7. Chưa làm (P9 và ngoài phạm vi)

- **P9**: dải "Hôm nay", lệch lịch và đề xuất dời lịch khi trễ, tab Nhìn lại (tổng kết), chia tiền nhóm, ràng buộc cứng của chuyến, đổi lịch khi trời xấu (đã làm được qua chat, chưa có nút riêng), `parse_expense`, `rank_candidates`.
- **Đã có cột, chưa có API riêng**: `actual_start`, `actual_end` và trạng thái hoạt động sửa qua `PUT /api/activities/{id}`. Có kiểm phải có lý do khi bỏ và chỉ một hoạt động đang làm.
- **Theo quyết định**: refresh token (D2), quy đổi tỷ giá (D3), nhật ký/ảnh/giọng nói (D5), PDF ở backend (D10).
