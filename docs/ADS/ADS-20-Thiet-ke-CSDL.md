# ADS-20 — Thiết kế cơ sở dữ liệu

**TripMind** · v1.0 · 03/09/2026

Tài liệu này mô tả từng bảng, từng cột, và bất biến nào được cưỡng chế ở đâu. Lược đồ thật do các tệp di trú Flyway trong `backend/src/main/resources/db/migration/` định nghĩa; tài liệu này diễn giải chứ không định nghĩa.

> **Nguyên tắc chi phối: bất biến cấu trúc cưỡng chế ở cơ sở dữ liệu, luật nghiệp vụ cưỡng chế ở tầng dịch vụ.** Mã ứng dụng có thể quên; ràng buộc thì không. Nhưng ràng buộc không nói được câu "chuyến đi tối đa 30 ngày" bằng thông báo người dùng đọc hiểu — nên luật đó ở tầng dịch vụ.

---

## 1. Nguyên tắc

### 1.1 Mười ba bảng, ba nhóm

| Nhóm | Bảng | Xoá theo |
|---|---|---|
| Thuộc người dùng | `users` · `user_preferences` · `saved_places` · `conversations` | Người dùng |
| Thuộc chuyến đi | `trips` · `itinerary_days` · `activities` · `expenses` · `ai_proposals` · `messages` · `ai_tool_executions` | Chuyến đi (hoặc hội thoại) |
| Dùng chung | `destinations` · `places` | **Không xoá theo ai** |

Bảng dùng chung không có chủ. Xoá chuyến đi hay xoá người dùng **không được** xoá địa điểm — `BR-304`.

### 1.2 Quy ước chung cho cả mười ba bảng

| Quy ước | Chi tiết |
|---|---|
| Khoá chính | `BIGINT GENERATED ALWAYS AS IDENTITY`, tên cột `id` |
| Mốc thời gian | `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`; bảng có sửa thì thêm `updated_at` |
| Tiền | `BIGINT` theo **đơn vị nhỏ nhất** + cột `currency CHAR(3)` — `QĐ-10` |
| Toạ độ | `NUMERIC(9,6)` cho vĩ độ, `NUMERIC(9,6)` cho kinh độ |
| Chuỗi có tập giá trị | `VARCHAR` + `CHECK`, **không dùng kiểu `ENUM` của PostgreSQL** |
| Dữ liệu tự do | `JSONB`, không dùng `TEXT` chứa JSON |
| Xoá lan | `ON DELETE CASCADE` cho quan hệ sở hữu; `ON DELETE RESTRICT` cho tham chiếu tới bảng dùng chung |
| Di trú | Mọi thay đổi qua Flyway. `spring.jpa.hibernate.ddl-auto: validate` — `QĐ-12` |

**Vì sao `CHECK` thay vì `ENUM`:** thêm một giá trị vào `ENUM` của PostgreSQL cần lệnh `ALTER TYPE` không chạy được trong giao dịch ở một số phiên bản. `CHECK` sửa dễ và đọc rõ trong tệp di trú.

---

## 2. Lược đồ và từ điển dữ liệu

### 2.1 `users`

Tài khoản. Gốc của mọi quyền sở hữu.

| Cột | Kiểu | Ràng buộc | Nghĩa |
|---|---|---|---|
| `id` | `BIGINT` | PK | |
| `email` | `VARCHAR(255)` | `NOT NULL`, **`UNIQUE`** | Định danh đăng nhập |
| `password_hash` | `VARCHAR(72)` | `NOT NULL` | BCrypt. **Không bao giờ ghi ra nhật ký** — `BR-002` |
| `name` | `VARCHAR(120)` | `NOT NULL` | Họ tên hiển thị |
| `avatar_url` | `TEXT` | | Có thể rỗng |
| `role` | `VARCHAR(16)` | `NOT NULL`, `CHECK IN ('USER','ADMIN')` | Một tài khoản một vai trò — `BR-005` |
| `is_active` | `BOOLEAN` | `NOT NULL DEFAULT true` | Khoá tài khoản không xoá dữ liệu |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | `NOT NULL` | |

**Chỉ mục:** `UNIQUE (email)`

### 2.2 `refresh_tokens` (Bỏ khỏi CSDL quan hệ)

> **Quyết định thiết kế:** Không tạo bảng `refresh_tokens` trong PostgreSQL. Token được quản lý qua Redis (với cơ chế TTL tự động hết hạn) hoặc token phi trạng thái (stateless JWT), giúp giảm tải I/O cho cơ sở dữ liệu chính.


### 2.3 `user_preferences`

Sở thích mặc định, dùng lại cho chuyến sau và cho trợ lý cá nhân hoá.

| Cột | Kiểu | Ràng buộc | Nghĩa |
|---|---|---|---|
| `id` | `BIGINT` | PK | |
| `user_id` | `BIGINT` | `NOT NULL`, **`UNIQUE`**, FK → `users` `CASCADE` | Một người một bản ghi |
| `travel_style` | `VARCHAR(16)` | `CHECK IN ('RELAXED','BALANCED','FAST_PACED')` | |
| `budget_preference` | `VARCHAR(16)` | `CHECK IN ('BUDGET','MODERATE','LUXURY')` | |
| `preferences_json` | `JSONB` | `NOT NULL DEFAULT '[]'` | Mảng mã sở thích: ăn uống, biển, thiên nhiên… |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | `NOT NULL` | |

### 2.4 `destinations`

Điểm đến. Bảng dùng chung, gieo sẵn.

| Cột | Kiểu | Ràng buộc | Nghĩa |
|---|---|---|---|
| `id` | `BIGINT` | PK | |
| `name` | `VARCHAR(160)` | `NOT NULL` | |
| `country` | `VARCHAR(80)` | `NOT NULL` | |
| `latitude` / `longitude` | `NUMERIC(9,6)` | `NOT NULL` | Tâm điểm đến, dùng cho thời tiết |
| `timezone` | `VARCHAR(64)` | `NOT NULL` | Ví dụ `Asia/Ho_Chi_Minh` |
| `metadata` | `JSONB` | | Ảnh, mô tả ngắn |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` | |

**Chỉ mục:** `(country, name)`

### 2.5 `trips`

Chuyến đi. Trung tâm của mô hình.

| Cột | Kiểu | Ràng buộc | Nghĩa |
|---|---|---|---|
| `id` | `BIGINT` | PK | |
| `user_id` | `BIGINT` | `NOT NULL`, FK → `users` `CASCADE` | **Chủ sở hữu** — `DI-1` |
| `destination_id` | `BIGINT` | `NOT NULL`, FK → `destinations` `RESTRICT` | Không xoá điểm đến đang có chuyến |
| `name` | `VARCHAR(160)` | `NOT NULL` | |
| `start_date` / `end_date` | `DATE` | `NOT NULL`, `CHECK end_date >= start_date` | — `DI-9` |
| `travelers` | `SMALLINT` | `NOT NULL`, `CHECK travelers >= 1` | |
| `budget` | `BIGINT` | `CHECK budget >= 0` | Đơn vị nhỏ nhất — `DI-10` |
| `currency` | `CHAR(3)` | `NOT NULL DEFAULT 'VND'` | |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | `NOT NULL` | |

**Chỉ mục:** `(user_id, start_date DESC)` — dùng cho bảng điều khiển

**Không có cột `status`.** Trạng thái chuyến đi suy ra từ ngày hôm nay so với khoảng ngày — `ADS-02` §2.3. Lưu thành cột thì phải có tác vụ chạy nền cập nhật, và sẽ sai mỗi lần tác vụ đó lỡ nhịp.

### 2.6 `itinerary_days`

Một ngày trong chuyến.

| Cột | Kiểu | Ràng buộc | Nghĩa |
|---|---|---|---|
| `id` | `BIGINT` | PK | |
| `trip_id` | `BIGINT` | `NOT NULL`, FK → `trips` `CASCADE` | |
| `day_number` | `SMALLINT` | `NOT NULL`, `CHECK day_number >= 1` | Đếm **từ 1** |
| `date` | `DATE` | `NOT NULL` | Ngày dương lịch tương ứng |
| `note` | `TEXT` | | Ghi chú cho cả ngày |

**Chỉ mục:** **`UNIQUE (trip_id, day_number)`** — `DI-2`; `UNIQUE (trip_id, date)`

Sinh tự động khi tạo chuyến, trong cùng giao dịch — `BR-202`.

### 2.7 `activities`

Hoạt động trong một ngày. **Bảng này khác đề cương gốc ở ba chỗ.**

| Cột | Kiểu | Ràng buộc | Nghĩa |
|---|---|---|---|
| `id` | `BIGINT` | PK | |
| `itinerary_day_id` | `BIGINT` | `NOT NULL`, FK → `itinerary_days` `CASCADE` | |
| **`title`** | `VARCHAR(200)` | **`NOT NULL`** | **Thêm mới.** Tên hiển thị khi không có địa điểm |
| **`place_id`** | `BIGINT` | **`NULL` cho phép**, FK → `places` `RESTRICT` | **Đổi.** "Ăn trưa" không gắn địa điểm nào — `DI-4` |
| **`activity_type`** | `VARCHAR(16)` | `NOT NULL`, `CHECK IN ('SIGHTSEEING','FOOD','TRANSPORT','ACCOMMODATION','REST','OTHER')` | **Thêm mới** |
| **`created_by`** | `VARCHAR(8)` | `NOT NULL`, `CHECK IN ('USER','AI')` | **Thêm mới.** Thống kê AI đóng góp bao nhiêu |
| `start_time` / `end_time` | `TIME` | | Không kèm ngày — ngày ở bản ghi cha |
| `estimated_cost` | `BIGINT` | `CHECK estimated_cost >= 0` | Đơn vị nhỏ nhất — `DI-10` |
| `transportation_mode` | `VARCHAR(16)` | | Cách tới đây từ điểm trước |
| `notes` | `TEXT` | | |
| `order_index` | `SMALLINT` | `NOT NULL`, `CHECK order_index >= 0` | Đếm **từ 0**, liên tục |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | `NOT NULL` | |

**Chỉ mục:** **`UNIQUE (itinerary_day_id, order_index) DEFERRABLE INITIALLY DEFERRED`** — `DI-3`

**Vì sao hoãn kiểm tra ràng buộc:** sắp lại thứ tự phải gán lại nhiều dòng trong một giao dịch, và trạng thái giữa chừng chắc chắn vi phạm tính duy nhất. Hoãn tới lúc chốt giao dịch thì làm được mà không cần chiêu trò gán số âm tạm thời — `BR-204`.

### 2.8 `places`

Địa điểm có thật. Bảng dùng chung.

| Cột | Kiểu | Ràng buộc | Nghĩa |
|---|---|---|---|
| `id` | `BIGINT` | PK | |
| `provider` | `VARCHAR(24)` | `NOT NULL` | `GOOGLE` · `MOCK` |
| `external_id` | `VARCHAR(255)` | `NOT NULL` | Mã ở nhà cung cấp. **Giữ vĩnh viễn** — `BR-302` |
| `name` | `VARCHAR(255)` | `NOT NULL` | |
| `category` | `VARCHAR(64)` | | |
| `latitude` / `longitude` | `NUMERIC(9,6)` | `NOT NULL` | |
| `rating` | `NUMERIC(2,1)` | `CHECK rating BETWEEN 0 AND 5` | |
| `price_level` | `SMALLINT` | `CHECK price_level BETWEEN 0 AND 4` | |
| `address` | `TEXT` | | |
| `opening_hours` | `JSONB` | | |
| `metadata` | `JSONB` | | |
| `fetched_at` | `TIMESTAMPTZ` | `NOT NULL` | **Mốc của ảnh chụp** |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` | |

**Chỉ mục:** **`UNIQUE (provider, external_id)`** — `DI-8`; `(latitude, longitude)`

**Mọi cột trừ `provider` và `external_id` là ảnh chụp tại `fetched_at`.** Nhà cung cấp đổi tên hay đổi giờ mở cửa thì lần tra sau ghi đè, giữ nguyên hai cột định danh. Hệ thống **không biết** địa điểm còn hoạt động hay đã đóng cửa — `ADS-01` §6.2.

### 2.9 `saved_places`

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | `BIGINT` | PK |
| `user_id` | `BIGINT` | `NOT NULL`, FK → `users` `CASCADE` |
| `place_id` | `BIGINT` | `NOT NULL`, FK → `places` `RESTRICT` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` |

**Chỉ mục:** **`UNIQUE (user_id, place_id)`** — `DI-7`

Xoá dòng ở đây **không** xoá địa điểm — `BR-304`.

### 2.10 `expenses`

Chi tiêu thực tế.

| Cột | Kiểu | Ràng buộc | Nghĩa |
|---|---|---|---|
| `id` | `BIGINT` | PK | |
| `trip_id` | `BIGINT` | `NOT NULL`, FK → `trips` `CASCADE` | |
| **`activity_id`** | `BIGINT` | **`NULL` cho phép**, FK → `activities` `SET NULL` | **Thêm mới.** Nối chi tiêu với hoạt động đã lên kế hoạch |
| `category` | `VARCHAR(16)` | `NOT NULL`, `CHECK IN ('ACCOMMODATION','FOOD','TRANSPORTATION','ACTIVITIES','SHOPPING','OTHER')` | |
| `amount` | `BIGINT` | `NOT NULL`, `CHECK amount >= 0` | — `DI-10` |
| `currency` | `CHAR(3)` | `NOT NULL` | |
| `description` | `VARCHAR(255)` | | |
| `expense_date` | `DATE` | `NOT NULL` | |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` | |

**Chỉ mục:** `(trip_id, expense_date)`

Bảng này chứa **chi tiêu thực tế**. Chi phí ước tính nằm ở `activities.estimated_cost`. Hai đại lượng không cộng vào nhau — `BR-402`.

### 2.11 `conversations`

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | `BIGINT` | PK |
| `user_id` | `BIGINT` | `NOT NULL`, FK → `users` `CASCADE` |
| `trip_id` | `BIGINT` | `NOT NULL`, FK → `trips` `CASCADE` |
| `title` | `VARCHAR(200)` | |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | `NOT NULL` |

**Chỉ mục:** `(trip_id, updated_at DESC)`

Hội thoại luôn gắn một chuyến đi. Không có chat tự do ngoài ngữ cảnh chuyến đi.

### 2.12 `messages`

Tin nhắn. **Bảng này khác đề cương gốc ở ba cột** — thiếu chúng thì không dựng lại được lịch sử hội thoại có công cụ ở lượt sau.

| Cột | Kiểu | Ràng buộc | Nghĩa |
|---|---|---|---|
| `id` | `BIGINT` | PK | |
| `conversation_id` | `BIGINT` | `NOT NULL`, FK → `conversations` `CASCADE` | |
| `role` | `VARCHAR(12)` | `NOT NULL`, `CHECK IN ('SYSTEM','USER','ASSISTANT','TOOL')` | — `DI-12` |
| `content` | `TEXT` | | Rỗng khi trợ lý chỉ yêu cầu gọi công cụ |
| **`tool_calls_json`** | `JSONB` | | **Thêm mới.** Yêu cầu gọi công cụ của trợ lý |
| **`tool_call_id`** | `VARCHAR(64)` | | **Thêm mới.** Nối kết quả về đúng yêu cầu, khi `role = TOOL` |
| **`token_usage`** | `JSONB` | | **Thêm mới.** Số token vào/ra, cho thống kê quản trị |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` | |

**Chỉ mục:** `(conversation_id, created_at)`

### 2.13 `ai_proposals`

**Bảng này hoàn toàn không có trong đề cương gốc.** Đề cương có điểm cuối áp dụng và luồng phê duyệt nhưng không có chỗ lưu đề xuất — nghĩa là điểm cuối đó buộc phải nhận danh sách thay đổi do máy khách gửi lên, và bất kỳ ai cũng gửi được một gói tuỳ ý, bỏ qua toàn bộ AI lẫn mọi kiểm tra. Khi đó "người dùng phê duyệt" chỉ còn là trang trí.

| Cột | Kiểu | Ràng buộc | Nghĩa |
|---|---|---|---|
| `id` | `BIGINT` | PK | |
| `trip_id` | `BIGINT` | `NOT NULL`, FK → `trips` `CASCADE` | — `DI-6` |
| `conversation_id` | `BIGINT` | `NOT NULL`, FK → `conversations` `CASCADE` | |
| `message_id` | `BIGINT` | FK → `messages` `SET NULL` | Tin nhắn đã sinh ra đề xuất |
| `summary` | `TEXT` | `NOT NULL` | Lời diễn giải cho người đọc |
| `changes_json` | `JSONB` | `NOT NULL` | Danh sách thao tác — xem §2.13.1 |
| `estimated_cost_delta` | `BIGINT` | | Có thể âm |
| `travel_time_delta` | `INTEGER` | | Giây. Có thể âm |
| `status` | `VARCHAR(16)` | `NOT NULL`, `CHECK IN ('PENDING','APPLIED','REJECTED','EXPIRED')` | — `DI-5` |
| `expires_at` | `TIMESTAMPTZ` | `NOT NULL` | Tạo lúc nào + 30 phút |
| `applied_at` | `TIMESTAMPTZ` | | |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` | |

**Chỉ mục:** `(trip_id, status, created_at DESC)`

#### 2.13.1 Hình dạng của `changes_json`

```json
{
  "operations": [
    { "op": "REMOVE", "activityId": 512 },
    { "op": "ADD", "dayNumber": 2, "orderIndex": 3,
      "title": "Bảo tàng Điêu khắc Chăm",
      "placeId": 88, "startTime": "14:30", "estimatedCost": 60000 },
    { "op": "UPDATE", "activityId": 515, "startTime": "16:30" },
    { "op": "REORDER", "dayNumber": 2, "activityIds": [510, 88, 515] }
  ]
}
```

Bốn thao tác, khai báo dạng phân cấp kín trong mã Java để trình biên dịch bắt lỗi nếu quên xử lý một nhánh.

**`changes_json` là bản ghi tại thời điểm ra quyết định.** Nếu người dùng sửa tay lịch trình rồi mới bấm áp dụng, thao tác có thể trỏ tới hoạt động đã bị xoá. Lúc áp dụng phải kiểm lại từng thao tác, không tin bản ghi cũ — `BR-507`.

### 2.14 `ai_tool_executions`

Nhật ký chạy công cụ. Ghi cả khi lỗi — `BR-505`.

| Cột | Kiểu | Ràng buộc | Nghĩa |
|---|---|---|---|
| `id` | `BIGINT` | PK | |
| `conversation_id` | `BIGINT` | FK → `conversations` `CASCADE` | |
| `message_id` | `BIGINT` | FK → `messages` `SET NULL` | |
| **`trip_id`** | `BIGINT` | FK → `trips` `SET NULL` | **Thêm mới.** Cho quản trị lọc |
| **`user_id`** | `BIGINT` | FK → `users` `SET NULL` | **Thêm mới.** Cho quản trị lọc và thống kê |
| `tool_name` | `VARCHAR(64)` | `NOT NULL` | |
| `arguments` | `JSONB` | | Tham số mô hình sinh ra |
| `result` | `JSONB` | | **Cắt bớt nếu dài**, không lưu nguyên khối lớn |
| `status` | `VARCHAR(12)` | `NOT NULL`, `CHECK IN ('OK','ERROR','TIMEOUT')` | |
| `error_message` | `TEXT` | | |
| `execution_time_ms` | `INTEGER` | `NOT NULL` | |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` | |

**Chỉ mục:** `(user_id, created_at DESC)` · `(tool_name, created_at DESC)` · `(conversation_id, created_at)`

Bảng này phục vụ bốn việc: gỡ lỗi, kiểm toán, thống kê cho quản trị, và **chứng minh khi bảo vệ rằng trợ lý thực sự gọi công cụ** chứ không tự bịa câu trả lời.

### 2.15 Bảng cân nhắc rồi bỏ

| Bảng trong đề cương | Quyết định | Lý do |
|---|---|---|
| `weather_cache` | **Bỏ, thay bằng Redis** | Dữ liệu thời tiết vứt được, có hạn dùng tự nhiên, không cần bền vững. Để ở PostgreSQL thì phải tự dọn rác. Redis hết hạn tự xoá |

---

## 3. Bất biến dữ liệu

### 3.1 Mười hai bất biến cưỡng chế ở cơ sở dữ liệu

| Mã | Bất biến | Cưỡng chế bằng | Bảng |
|---|---|---|---|
| `DI-1` | Chuyến đi thuộc đúng một người | `FK NOT NULL` | `trips` |
| `DI-2` | Một chuyến không có hai ngày cùng số | `UNIQUE (trip_id, day_number)` | `itinerary_days` |
| `DI-3` | Một ngày không có hai hoạt động cùng vị trí | `UNIQUE (day_id, order_index)` hoãn | `activities` |
| `DI-4` | Hoạt động có tên; địa điểm không bắt buộc | `title NOT NULL` · `place_id NULL` | `activities` |
| `DI-5` | Trạng thái đề xuất thuộc tập cố định | `CHECK` | `ai_proposals` |
| `DI-6` | Đề xuất thuộc đúng một chuyến | `FK NOT NULL` | `ai_proposals` |
| `DI-7` | Không lưu trùng địa điểm | `UNIQUE (user_id, place_id)` | `saved_places` |
| `DI-8` | Một địa điểm ngoài một bản ghi | `UNIQUE (provider, external_id)` | `places` |
| `DI-9` | Ngày kết thúc không trước ngày bắt đầu | `CHECK` | `trips` |
| `DI-10` | Tiền không âm | `CHECK` | `trips` · `activities` · `expenses` |
| `DI-11` | Phiếu làm mới duy nhất | `UNIQUE (token_hash)` | `refresh_tokens` |
| `DI-12` | Vai trò tin nhắn thuộc tập cố định | `CHECK` | `messages` |

### 3.2 Cố ý **không** cưỡng chế ở cơ sở dữ liệu

| Luật | Để ở đâu | Vì sao |
|---|---|---|
| Chuyến tối đa 30 ngày | Dịch vụ — `BR-201` | Chính sách sản phẩm, đổi được. `CHECK` thì phải chạy di trú |
| Số ngày khớp khoảng ngày | Dịch vụ — `BR-202`, `BR-203` | Cần giao dịch nhiều bước, ràng buộc không diễn tả được |
| Giờ hoạt động không chồng nhau | Dịch vụ — cảnh báo | Chồng giờ đôi khi hợp lệ. Cảnh báo tốt hơn cấm |
| Vị trí sắp xếp liên tục, không có lỗ | Dịch vụ — `BR-204` | `UNIQUE` chặn trùng, không chặn được lỗ |
| Tổng chi phí không vượt ngân sách | Dịch vụ — `BR-403` | **Vượt ngân sách là chuyện thật.** Phải cho phép rồi cảnh báo |
| Chỉ cộng tiền cùng mã tiền tệ | Dịch vụ — `BR-401` | Là luật tính toán, không phải ràng buộc lưu trữ |

---

## 4. Truy vết bảng sang màn hình

| Bảng | Màn hình dùng |
|---|---|
| `users` · `refresh_tokens` | Đăng nhập · Hồ sơ |
| `user_preferences` | Tạo chuyến bước 5–6 · Trợ lý cá nhân hoá |
| `destinations` | Tạo chuyến bước 1 |
| `trips` | Bảng điều khiển · Tổng quan chuyến đi |
| `itinerary_days` · `activities` | Lịch trình · Bản đồ · Ngân sách (phần ước tính) |
| `places` | Tìm địa điểm · Chi tiết · Bản đồ |
| `saved_places` | Địa điểm đã lưu |
| `expenses` | Ngân sách (phần thực tế) |
| `conversations` · `messages` | Trợ lý |
| `ai_proposals` | Thẻ khác biệt trong Trợ lý |
| `ai_tool_executions` | Dòng công cụ trong Trợ lý · Màn quản trị |

---

## 5. Việc còn lại

| Việc | Khi nào |
|---|---|
| Viết tệp di trú `V1`–`V5` theo đúng thứ tự ở `PLAN.md` §7 | Tuần 1, 2, 3, 7, 9 |
| Gieo khoảng 15 điểm đến Việt Nam và châu Á | Tuần 2 |
| Đo lại kế hoạch truy vấn của trang lịch trình sau khi có dữ liệu thật | Tuần 5 |
| Quyết định có cần chỉ mục không gian cho `places` hay không | Sau khi biết số lượng địa điểm thật |
| Đặt tác vụ dọn `ai_proposals` quá hạn | Tuần 11 |

---

## 6. Tài liệu liên quan

| Tài liệu | Nội dung |
|---|---|
| [`../PLAN.md`](../PLAN.md) §12.2 | **Nguồn chân lý của `DI`** |
| [`../PLAN.md`](../PLAN.md) §5 | Vì sao lược đồ khác đề cương gốc |
| [`ADS-02`](ADS-02-Tu-dien-Mo-hinh-mien.md) §3.1 | Ánh xạ từ ngữ sang cột |
| [`ADS-10`](ADS-10-Kien-truc-phan-mem.md) §4 | Luật nào đặt ở tầng nào |
| [`ADS-30`](ADS-30-Hop-dong-API.md) | Cột nào lộ ra ngoài qua điểm cuối nào |
