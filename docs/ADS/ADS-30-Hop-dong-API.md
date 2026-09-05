# ADS-30 — Hợp đồng API

**TripMind** · v2.0 · 05/09/2026

> **Bản v2.0 đảo `DI-8`** (tra cứu không còn ghi vào cơ sở dữ liệu, §6) và thêm bốn nhóm
> điểm cuối: người tham gia và chia tiền, giai đoạn chuyến đi cùng trạng thái hoạt động,
> nhật ký và ảnh, hoàn tác đề xuất. Nguyên tắc chi phối không đổi.

Tài liệu này chốt điểm cuối, mã lỗi và phân quyền. Mô tả OpenAPI sinh tự động là nguồn chân lý về hình dạng dữ liệu; tài liệu này nói về **quy ước và ý định**.

> **Nguyên tắc chi phối: mọi điểm cuối làm đổi lịch trình đều đi qua tầng dịch vụ, và tầng dịch vụ kiểm sở hữu** — `QĐ-05`, `BR-101`. Điểm cuối áp dụng đề xuất là điểm cuối duy nhất mà AI có thể gián tiếp làm đổi dữ liệu, và nó chỉ chạy sau một cú bấm của người dùng.

---

## 1. Nguyên tắc chung

| Quy ước | Chi tiết |
|---|---|
| Tiền tố | `/api` |
| Định dạng | JSON, `UTF-8` |
| Xác thực | `Authorization: Bearer <phiếu truy cập>` cho mọi điểm cuối trừ §2 |
| Phân trang | `?page=0&size=20`, trả `{ content, page, size, totalElements }` |
| Sắp xếp | `?sort=field,asc\|desc` |
| Ngày | `YYYY-MM-DD` |
| Giờ | `HH:mm` |
| Mốc thời gian | ISO-8601 kèm múi giờ |
| Tiền | **Số nguyên** đơn vị nhỏ nhất + trường `currency` kèm theo — `QĐ-10` |

### 1.1 Hình dạng lỗi

```json
{
  "code": "TRIP_NOT_FOUND",
  "message": "Không tìm thấy chuyến đi",
  "details": null,
  "timestamp": "2026-09-03T10:15:00+07:00"
}
```

| HTTP | Khi nào | Mã tiêu biểu |
|---|---|---|
| `400` | Dữ liệu vào sai | `VALIDATION_FAILED` · `INVALID_DATE_RANGE` |
| `401` | Thiếu phiếu, phiếu hỏng, phiếu hết hạn | `UNAUTHENTICATED` · `TOKEN_EXPIRED` |
| `403` | Sai vai trò | `FORBIDDEN_ROLE` |
| `404` | Không tồn tại **hoặc không thuộc về người gọi** | `TRIP_NOT_FOUND` · `ACTIVITY_NOT_FOUND` |
| `409` | Xung đột trạng thái | `PROPOSAL_NOT_PENDING` · `PROPOSAL_EXPIRED` · `PLACE_ALREADY_SAVED` |
| `422` | Đúng cú pháp nhưng vi phạm luật nghiệp vụ | `TRIP_TOO_LONG` · `REORDER_SET_MISMATCH` |
| `429` | Vượt giới hạn tần suất | `RATE_LIMITED` kèm `retryAfterSeconds` |
| `507` | Hết chỗ lưu, hoặc kho ảnh không dùng được | `STORAGE_FULL` · `PHOTO_STORE_UNAVAILABLE` |
| `502` | Dịch vụ ngoài lỗi **và không có đường lui** | `UPSTREAM_UNAVAILABLE` |
| `500` | Lỗi không lường trước | `INTERNAL_ERROR` |

> **`404` cho cả hai trường hợp "không có" và "không phải của bạn"** — `BR-104`. Trả `403` sẽ tiết lộ rằng chuyến đi đó tồn tại.

> **Dịch vụ ngoài lỗi thường trả `200` kèm dữ liệu rỗng và lý do**, không trả `502` — `QĐ-11`. `502` chỉ dùng khi điểm cuối không còn gì để trả.

---

## 2. Xác thực

```http
POST   /api/auth/register     { email, password, name }
POST   /api/auth/login        { email, password }
POST   /api/auth/refresh      { refreshToken }
POST   /api/auth/logout       { refreshToken }
```

Trả về:

```json
{ "accessToken": "...", "refreshToken": "...", "expiresIn": 900 }
```

| Luật | Hành vi |
|---|---|
| `BR-003` | Mỗi lần làm mới cấp phiếu mới **và thu hồi phiếu cũ** |
| `BR-004` | Dùng lại phiếu đã thu hồi → thu hồi **cả họ**, trả `401` |
| `NFR-05` | Mật khẩu không bao giờ xuất hiện trong phản hồi hay nhật ký |

---

## 3. Hồ sơ và sở thích

```http
GET    /api/me
PATCH  /api/me                      { name?, avatarUrl? }
GET    /api/me/preferences
PUT    /api/me/preferences          { travelStyle, budgetPreference, preferences[] }
```

---

## 4. Chuyến đi

```http
GET    /api/trips?status=upcoming|past&page=&size=
POST   /api/trips
GET    /api/trips/{id}
PUT    /api/trips/{id}
DELETE /api/trips/{id}
```

Thân yêu cầu tạo — gộp cả sáu bước của trình hướng dẫn thành **một lời gọi**:

```json
{
  "destinationId": 12,
  "name": "Đà Nẵng tháng 10",
  "startDate": "2026-10-20",
  "endDate": "2026-10-23",
  "travelers": 2,
  "budget": 8000000,
  "currency": "VND",
  "travelStyle": "BALANCED",
  "budgetPreference": "MODERATE",
  "preferences": ["FOOD", "BEACH", "PHOTOGRAPHY"]
}
```

| Kiểm | Lỗi |
|---|---|
| `endDate >= startDate` — `DI-9` | `400 INVALID_DATE_RANGE` |
| Độ dài ≤ 30 ngày — `BR-201` | `422 TRIP_TOO_LONG` |
| `travelers >= 1` | `400 VALIDATION_FAILED` |
| Điểm đến tồn tại | `404 DESTINATION_NOT_FOUND` |

Tạo chuyến **sinh luôn đủ số ngày** trong cùng giao dịch — `BR-202`.

Phản hồi có trường tính sẵn:

```json
{ "id": 77, "planningProgress": 0.72, "daysCount": 4, ... }
```

`planningProgress` = số ngày có từ 2 hoạt động, chia tổng số ngày — `BR-207`.

---

## 5. Lịch trình

```http
GET    /api/trips/{id}/itinerary
POST   /api/trips/{id}/itinerary/activities
PUT    /api/activities/{id}
DELETE /api/activities/{id}
PUT    /api/itinerary-days/{dayId}/reorder
```

`GET` trả cả cây trong **một lời gọi** — ngày, hoạt động, địa điểm — dùng nạp kèm để không sinh truy vấn lặp (`NFR-04`).

### 5.1 Sắp lại thứ tự

```json
PUT /api/itinerary-days/91/reorder
{ "activityIds": [512, 88, 515, 7] }
```

| Quy ước | Lý do |
|---|---|
| Gửi **cả danh sách**, không gửi từng phần tử | Tránh trạng thái trung gian vi phạm `DI-3` — `BR-204` |
| Danh sách phải khớp **đúng** tập hoạt động của ngày | Thiếu hoặc thừa → `422 REORDER_SET_MISMATCH` — `BR-205` |
| Vị trí mới = chỉ số trong mảng, đếm từ 0 | |

### 5.2 Tạo hoạt động

```json
{
  "dayNumber": 2,
  "title": "Ăn trưa",
  "activityType": "FOOD",
  "placeId": null,
  "startTime": "12:00",
  "endTime": "13:00",
  "estimatedCost": 300000
}
```

**`placeId` được phép rỗng** — `DI-4`. `title` thì không.

---

## 6. Địa điểm

```http
GET    /api/places?q=&lat=&lng=&radius=
GET    /api/places/{id}
POST   /api/places/{id}/save
DELETE /api/places/{id}/save
GET    /api/me/saved-places
```

| Hành vi | Chi tiết |
|---|---|
| Tìm kiếm tra đệm Redis trước | Khoá theo băm của truy vấn, hạn 1 giờ |
| **Kết quả KHÔNG ghi vào bảng địa điểm** | **Đảo `DI-8`.** Tra cứu chỉ đi vào bộ đệm — `ADS-20` §1.3 |
| Mỗi kết quả kèm `dbId` | `null` nghĩa là chỗ này chưa nằm trong cơ sở dữ liệu của bạn |
| Phản hồi kèm `cached: true/false` | Nói rõ số liệu lấy lại từ đệm hay vừa hỏi nhà cung cấp |
| Nhà cung cấp lỗi | `200` kèm `{ "results": [], "reason": "PROVIDER_UNAVAILABLE" }` |
| Lưu trùng | `409 PLACE_ALREADY_SAVED` |
| Bỏ lưu | Xoá liên kết, **giữ** bản ghi địa điểm — `BR-304` |

**Địa điểm chỉ vào cơ sở dữ liệu qua bốn đường, cả bốn đều là cú bấm của người dùng:**

```http
POST   /api/places/{externalId}/save                 → adopted_via = SAVED
POST   /api/trips/{id}/itinerary/activities          → ITINERARY  (khi thân có placeExternalId)
POST   /api/trips/{id}/ai/apply                      → PROPOSAL   (đề xuất mang địa điểm mới)
POST   /api/trips/{id}/ai/generate                   → AI_GENERATE
```

Không có điểm cuối nào ghi địa điểm mà không do người dùng khởi động. `GET /api/places`
là điểm cuối **chỉ đọc theo đúng nghĩa**: gọi bao nhiêu lần cũng không sinh dòng nào.

Phản hồi tìm kiếm hiển thị kèm dòng ghi nguồn theo yêu cầu của nhà cung cấp.

---

## 7. Ngân sách và chi tiêu

```http
GET    /api/trips/{id}/budget
GET    /api/trips/{id}/expenses?category=&from=&to=
POST   /api/trips/{id}/expenses
PUT    /api/expenses/{id}
DELETE /api/expenses/{id}
```

`GET budget` trả:

```json
{
  "budget": 8000000,
  "currency": "VND",
  "estimatedTotal": 7350000,
  "actualTotal": 2100000,
  "remaining": 650000,
  "byCategory": { "ACCOMMODATION": 2000000, "FOOD": 1500000, "...": 0 },
  "warningLevel": "NONE"
}
```

| Quy ước | Luật |
|---|---|
| `estimatedTotal` và `actualTotal` là hai đại lượng tách rời | `BR-402` |
| `remaining` = ngân sách − phần lớn hơn giữa hai cái | |
| `warningLevel` ∈ `NONE` · `NEAR_LIMIT` (>90%) · `OVER` (>100%) | `BR-403` |
| Chỉ cộng khoản cùng `currency` | `BR-401` |

---

## 8. Thời tiết

```http
GET /api/trips/{id}/weather
```

```json
{
  "days": [
    { "date": "2026-09-10", "tempMax": 31, "precipitationProbability": 0.8,
      "weatherCode": 61, "source": "FORECAST" },
    { "date": "2026-11-15", "tempMax": 26, "precipitationProbability": 0.45,
      "weatherCode": null, "source": "CLIMATE_NORMAL" }
  ]
}
```

**Trường `source` là bắt buộc trên mọi phần tử** — `QĐ-07`. Giao diện phải hiển thị khác nhau cho hai giá trị; gộp chung là nói sai sự thật.

Open-Meteo lỗi → `200` kèm `days: []` và `reason`.

---

## 9. Trợ lý AI

### 9.1 Hỏi trợ lý — luồng sự kiện

```http
POST /api/trips/{id}/ai/chat
Accept: text/event-stream

{ "message": "Ngày mai mưa thì đổi lịch giúp tôi", "conversationId": 33 }
```

Không trả JSON một lần. Trả **luồng sự kiện**:

```text
event: tool_start   data: {"tool":"get_weather","label":"Đang kiểm tra thời tiết..."}
event: tool_end     data: {"tool":"get_weather","ms":320,"status":"OK"}
event: token        data: {"text":"Ngày mai "}
event: proposal     data: {"proposalId":1042}
event: done         data: {"messageId":889,"conversationId":33}
```

| Quy ước | Luật |
|---|---|
| Kiểm sở hữu chuyến **trước** khi mở luồng | `BR-101` |
| Kiểm giới hạn tần suất tầng AI trước khi mở luồng | `BR-601` |
| Vòng lặp dừng ở vòng 8 hoặc giây 60 | `BR-502` |
| Chạm trần thì trả lời bằng dữ liệu đang có, **không báo lỗi** | |
| Nhà cung cấp mô hình lỗi | `event: error` kèm lý do, đóng luồng |

### 9.2 Lịch sử hội thoại

```http
GET /api/trips/{id}/ai/conversations
GET /api/conversations/{id}/messages?page=&size=
GET /api/trips/{id}/ai/activity          # nhật ký chạy công cụ của chuyến này
```

### 9.3 Sinh lịch trình tự động

```http
POST /api/trips/{id}/ai/generate      → 202 { "jobId": "..." }
GET  /api/trips/{id}/ai/generate/{jobId}  → { "state": "RUNNING", "step": "Đang tìm địa điểm...", "progress": 0.4 }
```

Chạy nền vì mất 30–90 giây. Trạng thái công việc nằm trong bộ nhớ tiến trình — khởi động lại thì mất, người dùng bấm lại.

---

## 10. Đề xuất và phê duyệt

```http
GET  /api/proposals/{id}
POST /api/trips/{id}/ai/apply      { "proposalId": 1042 }
POST /api/proposals/{id}/reject
```

### 10.1 Thân yêu cầu áp dụng

```json
{ "proposalId": 1042 }
```

**Chỉ có mã.** Không nhận danh sách thay đổi từ máy khách — `BR-506`. Nhận danh sách thì bất kỳ ai cũng gửi được một gói tuỳ ý và bỏ qua toàn bộ AI lẫn mọi kiểm tra.

### 10.2 Bốn điều kiểm

| # | Kiểm | Lỗi |
|---|---|---|
| 1 | Đề xuất thuộc đúng chuyến trong đường dẫn | `404 PROPOSAL_NOT_FOUND` |
| 2 | Chuyến thuộc người gọi | `404 PROPOSAL_NOT_FOUND` |
| 3 | Trạng thái là `PENDING` | `409 PROPOSAL_NOT_PENDING` |
| 4 | Chưa quá hạn | `409 PROPOSAL_EXPIRED` |

Đủ bốn thì áp dụng trong một giao dịch, trả về lịch trình mới.

**Gọi lần hai:** trạng thái đã là `APPLIED` → `409 PROPOSAL_NOT_PENDING`. Không nhân đôi dữ liệu — `BR-508`.

### 10.3 Phản hồi khi có thao tác bị bỏ qua

```json
{
  "applied": 3,
  "skipped": [ { "op": "REMOVE", "activityId": 512, "reason": "ACTIVITY_ALREADY_DELETED" } ],
  "itinerary": { }
}
```

Lịch trình có thể đã đổi từ lúc dựng đề xuất — `ADS-21` §4.4.

---

## 10a. Hoàn tác một đề xuất đã áp dụng

```http
POST /api/trips/{id}/ai/undo        { "proposalId": 1042 }
GET  /api/trips/{id}/ai/undo/{proposalId}/preview
```

Điểm cuối xem trước **chạy khô**: không ghi gì, chỉ trả về đúng những việc sắp xảy ra để
hộp xác nhận nói thật.

### 10a.1 Năm điều kiểm

| # | Kiểm | Lỗi |
|---|---|---|
| 1 | Đề xuất thuộc đúng chuyến, chuyến thuộc người gọi | `404 PROPOSAL_NOT_FOUND` |
| 2 | Trạng thái là `APPLIED` | `409 PROPOSAL_NOT_APPLIED` |
| 3 | Còn trong cửa sổ 10 phút | `409 UNDO_WINDOW_CLOSED` |
| 4 | Không bị đề xuất áp dụng sau chặn | `409 UNDO_BLOCKED_BY_LATER` kèm `details.blockedBy` |
| 5 | Có nhật ký nghịch đảo | `409 UNDO_NOT_AVAILABLE` |

Điều kiểm 4 là **LIFO có nới** — `ADS-21` §4.5.

### 10a.2 Phản hồi

```json
{
  "reverted": 2,
  "undoSkipped": [
    { "title": "Bảo tàng Điêu khắc Chăm", "reason": "ACTIVITY_EDITED_AFTER_APPLY" }
  ],
  "itinerary": { }
}
```

`undoSkipped` đối xứng với `skipped` của điểm cuối áp dụng, và giao diện phải hiện đầy đủ.
Lùi dở dang mà nói rõ còn hơn lùi sạch mà nuốt phần người dùng đã sửa tay.

Đề xuất đã hoàn tác chuyển `REVERTED` và **không áp dụng lại được** — điều kiểm `PENDING`
giữ nguyên.

## 10b. Giai đoạn chuyến đi, trạng thái hoạt động, nhật ký

```http
PUT    /api/trips/{id}/phase              { "phase": "DURING"|null, "simulatedDay": 2 }
PUT    /api/activities/{id}/status        { "status": "DOING", "actualStart": "15:20", "skipReason": null }
GET    /api/trips/{id}/drift?day=1
PUT    /api/trips/{id}/constraints        [{ "kind": "NO_EARLY", "value": "08:00" }]

GET    /api/trips/{id}/journal
PUT    /api/trips/{id}/journal            { "dayNumber": 1, "activityId": 504|null, "note": "…", "mood": "GOOD" }
DELETE /api/journal/{id}
POST   /api/trips/{id}/photos             multipart
DELETE /api/photos/{id}

GET    /api/trips/{id}/participants
POST   /api/trips/{id}/participants       { "name": "Minh Thư" }
DELETE /api/participants/{id}
GET    /api/trips/{id}/settlement
GET    /api/trips/{id}/accuracy
GET    /api/me/bias
```

| Quy ước | Chi tiết |
|---|---|
| `phase` bỏ trống | Quay về suy từ ngày. Phản hồi luôn kèm `phaseIsManual` để giao diện nói rõ |
| `status = DOING` | Hoạt động `DOING` trước đó tự chuyển `DONE`; phản hồi trả `autoClosed` |
| `GET drift` | **Chỉ tính.** Muốn dời giờ thì hỏi trợ lý, nó dựng đề xuất |
| `source` của drift | `ACTUAL` (đã xong muộn) khác `NOW` (đang làm và đã quá giờ). Giao diện phải nói khác nhau |
| Xoá người tham gia | `409 PARTICIPANT_HAS_EXPENSES` nếu họ đang đứng tên khoản chi |
| `POST photos` | `507 PHOTO_STORE_UNAVAILABLE` nếu kho ảnh không dùng được — **nói thẳng, không nhận rồi làm mất** |
| `GET accuracy` · `GET bias` | Hạng mục thiếu một trong hai vế trả `enough:false`, **không** trả 0 hay vô cực |

## 11. Quản trị

Mọi điểm cuối dưới đây yêu cầu vai trò `ADMIN` — `BR-006`.

```http
GET /api/admin/users?page=&size=
GET /api/admin/trips?page=&size=
GET /api/admin/tool-executions?userId=&tool=&status=&from=&to=&page=&size=
GET /api/admin/ai-usage?from=&to=
```

| Quy ước | Chi tiết |
|---|---|
| Quản trị **không sửa được nội dung chuyến đi của ai** | Chỉ có điểm cuối đọc — `FR-906` |
| `tool-executions` là trang quan trọng nhất | Bằng chứng trợ lý thực sự gọi công cụ |
| `ai-usage` trả số lượt, số token, công cụ hay dùng | Nguồn: `messages.token_usage` và `ai_tool_executions` |

---

## 12. Không có webhook

Không có điểm cuối nào để dịch vụ ngoài gọi vào. Mọi tích hợp là TripMind chủ động gọi ra rồi chờ trả lời, có hạn giờ — `ADS-10` §1.1.

Hệ quả: không cần xác thực chữ ký đến, không cần hàng đợi nhận, không có bề mặt tấn công từ phía đó.

---

## 13. Giới hạn tần suất

| Tầng | Khoá Redis | Hạn mức | Lỗi |
|---|---|---|---|
| API thường | `rl:api:{userId}:{phút}` | 120 / phút | `429 RATE_LIMITED` |
| AI theo giờ | `rl:ai:{userId}:{giờ}` | 20 / giờ | `429 RATE_LIMITED` |
| AI theo ngày | `rl:ai:{userId}:{ngày}` | 100 / ngày | `429 RATE_LIMITED` |

Phản hồi `429` kèm `retryAfterSeconds`. Con số chốt lại sau khi biết hạn ngạch thật của tài khoản nhà cung cấp mô hình — `ADS-21` §9.

---

## 14. Tài liệu liên quan

| Tài liệu | Nội dung |
|---|---|
| [`../PLAN.md`](../PLAN.md) §12 | **Nguồn chân lý** — `QĐ` · `DI` · `BR` |
| [`ADS-01`](ADS-01-Dac-ta-yeu-cau.md) §4 | Yêu cầu chức năng mà mỗi điểm cuối phục vụ |
| [`ADS-20`](ADS-20-Thiet-ke-CSDL.md) | Cột nào nằm sau trường nào |
| [`ADS-21`](ADS-21-Tich-hop-AI-Agent.md) | Chi tiết trợ lý và đề xuất |
| [`ADS-40`](ADS-40-Luong-giao-dien.md) | Màn hình nào gọi điểm cuối nào |
