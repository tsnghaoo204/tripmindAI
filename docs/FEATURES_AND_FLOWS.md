# TripMind AI — Kiến trúc Tính năng và Luồng Hoạt động (Workflows)

Tài liệu mô tả chi tiết danh mục tính năng và sơ đồ luồng hoạt động (workflows) cho nền tảng **TripMind AI — Lập kế hoạch và Quản lý chuyến du lịch thông minh**.

---

## 1. Triết lý thiết kế cốt lõi

Khác với các ứng dụng du lịch truyền thống thường gò bó người dùng vào một "thời khóa biểu công sở" từng phút (09:00 - 10:30 - 11:45), **TripMind AI** xây dựng theo triết lý **Du lịch thư giãn & Linh hoạt**:

1. **Lịch trình theo Cụm địa điểm & Khoảng cách di chuyển (Location Clustering & Routing):**
   - Không ép buộc giờ giấc cố định. Các hoạt động trong ngày được sắp xếp theo thứ tự **tiện đường đi nhất**, gom cụm theo buổi (Sáng / Chiều / Tối).
   - Luôn hiển thị **khoảng cách (km)** và **thời gian di chuyển ước tính (phút)** giữa 2 điểm liên tiếp để người dùng chủ động lộ trình, không bị chạy lòng vòng.
2. **Gợi ý "Khung giờ vàng" (Ideal Timing / Best Time to Visit):**
   - Thay vì ép giờ hành chính, mỗi địa điểm chỉ gắn kèm gợi ý thời điểm đẹp nhất:
     - *Ngắm cảnh / Check-in:* Khung giờ bình minh (05:00 - 05:45), hoàng hôn (16:30 - 17:45), tránh nắng gắt trưa hè.
     - *Ăn uống / Mua sắm:* Khung giờ quán mở cửa, tránh giờ cao điểm đông đúc/xếp hàng.
     - *Sự kiện:* Giờ diễn ra hoạt động đặc biệt (Cầu Rồng phun lửa, chợ đêm...).
3. **Giải trình AI minh bạch (Explainable AI — "Vì sao AI chọn chỗ này?"):**
   - AI không phải là "hộp đen". Mỗi đề xuất của AI đều giải trình 3 yếu tố: **Tiện đường** (cách điểm trước bao xa) + **Khung giờ vàng** (vì sao nên ghé lúc này) + **Đánh giá uy tín & giờ mở cửa**.
4. **Hoàn tác an toàn (Undo Proposal):**
   - Sau khi duyệt thay đổi do AI đề xuất, người dùng luôn có nút **↶ Hoàn tác** để khôi phục lại lịch trình nguyên trạng ban đầu chỉ với 1 chạm.

---

## 2. Bảng tổng hợp phân hệ tính năng

| Phân hệ | Tính năng chi tiết | Trạng thái hiện tại |
| :--- | :--- | :--- |
| **1. Xác thực & Tài khoản** | • Đăng ký tài khoản (mã hóa mật khẩu BCrypt, chống trùng email)<br>• Đăng nhập & cấp JWT Access Token (không dùng refresh token)<br>• Phân quyền RBAC (`ROLE_USER`, `ROLE_ADMIN`) bằng Spring Security<br>• Quản lý Profile (tên, avatar) và Đổi mật khẩu |  **Đã hoàn thành** |
| **2. Điểm đến & Khám phá địa điểm** | • Danh mục điểm đến nội bộ kết hợp tra cứu toàn cầu qua Google Places<br>• Tìm kiếm địa điểm, xem chi tiết (rating, review, giờ mở cửa)<br>• Gợi ý địa điểm lân cận theo tọa độ điểm đến<br>• Lưu danh sách địa điểm yêu thích (`saved_places`) | 🟡 **Đã xong tra cứu**<br>⏳ Chờ API lưu địa điểm |
| **3. Khởi tạo & Quản lý chuyến đi** | • Wizard tạo chuyến đi (Điểm đến, ngày đi/về, số người, ngân sách, phong cách)<br>• Tự động tính số ngày và khởi tạo lịch trình rỗng (`itinerary_days`)<br>• Xem danh sách chuyến đi của người dùng đã đăng nhập |  **Đã hoàn thành** |
| **4. Lịch trình linh hoạt & Cụm địa điểm** | • Xem danh sách địa điểm theo từng ngày theo thứ tự tối ưu cung đường<br>• **Khoảng cách di chuyển**: Hiển thị cự ly (km) và thời gian đi giữa các điểm<br>• **Gợi ý Khung giờ vàng**: Bình minh, hoàng hôn, giờ mở cửa quán ăn, tránh đông đúc<br>• Sắp xếp lại thứ tự hoạt động trong ngày (Kéo thả / Reorder API) | ⏳ **Kế hoạch tiếp theo** |
| **5. Ngân sách & Chi tiêu thực tế** | • Phân bổ ngân sách theo 6 hạng mục (Lưu trú, Ăn uống, Di chuyển, Vé, Mua sắm, Khác)<br>• Ghi nhận chi tiêu thực tế phát sinh trong chuyến đi (`expenses`)<br>• So sánh chi phí dự kiến vs thực tế, cảnh báo vượt hạn mức (`NEAR_LIMIT`, `OVER`) | ⏳ **Kế hoạch tiếp theo** |
| **6. Dịch vụ ngoài (Weather & FX)** | • Dự báo thời tiết từng ngày trong chuyến theo tọa độ điểm đến (Open-Meteo)<br>• Tự động quy đổi ngoại tệ theo tỷ giá thời gian thực | ⏳ **Kế hoạch tiếp theo** |
| **7. Trợ lý AI (AI Agent)** | • Chatbot ngữ cảnh chuyến đi (Chat Server-Sent Events - SSE Stream qua Gemini)<br>• **Tool Calling**: AI tự gọi API nội bộ đọc thời tiết, lịch trình, khoảng cách<br>• **Giải trình AI**: Nêu rõ lý do chọn điểm (tiện đường, giờ vàng, rating)<br>• **Proposal & Apply**: AI đề xuất đổi lịch trình, người dùng duyệt mới lưu vào DB<br>• **Hoàn tác an toàn (Undo)**: Khôi phục lại lịch trình trước khi duyệt đề xuất | ⏳ **Kế hoạch tiếp theo** |

---

## 3. Sơ đồ luồng hoạt động chi tiết (Mermaid Workflows)

### 3.1. Luồng Xác thực & Phân quyền (Authentication & Authorization)
*Đã hoàn thành trong mã nguồn backend.*

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng
    participant Client as Frontend / Web App
    participant Auth as AuthController / AuthService
    participant JWT as JwtTokenProvider / Filter
    participant DB as PostgreSQL (users)

    User->>Client: Nhập email & password
    Client->>Auth: POST /api/auth/login
    Auth->>DB: Tìm user theo email & so khớp mật khẩu BCrypt
    alt Mật khẩu hợp lệ & isActive = true
        Auth->>JWT: Sinh JWT Token (chứa userId, email, role)
        Auth-->>Client: 200 OK (accessToken, expiresInMs, user info)
        Client->>Client: Lưu accessToken vào localStorage / App State
    else Mật khẩu không đúng
        Auth-->>Client: 401 Unauthorized ("Invalid email or password")
    end

    Note over Client, JWT: Khi gọi các API yêu cầu đăng nhập (Trips, Profile...)
    Client->>JWT: Request kèm Header: "Authorization: Bearer <accessToken>"
    JWT->>JWT: Kiểm tra chữ ký bí mật & thời hạn token
    JWT->>JWT: Trích xuất userId, nạp UserPrincipal vào SecurityContext
    JWT->>Auth: Chuyển tiếp request đến Controller với danh tính người dùng thực
```

---

### 3.2. Luồng Khởi tạo chuyến đi & Tự động sinh ngày (Trip Creation)
*Đã hoàn thành trong mã nguồn backend.*

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng
    participant Client as Web App
    participant Dest as DestinationService
    participant Google as Google Places API
    participant Trip as TripService / Controller
    participant DB as PostgreSQL

    User->>Client: Nhập tên thành phố (vd: "Đà Nẵng")
    Client->>Dest: Tra cứu điểm đến
    alt Điểm đến chưa có trong bảng destinations
        Dest->>Google: Text Search / Place Details
        Google-->>Dest: Tọa độ (lat, lng), quốc gia, múi giờ
        Dest->>DB: INSERT điểm đến mới vào bảng destinations
    end

    User->>Client: Nhập khoảng ngày (01/10 - 03/10), số người, ngân sách
    Client->>Trip: POST /api/trips (kèm Bearer Token)
    Trip->>Trip: Trích xuất userId từ token, tính số ngày (3 ngày)
    Trip->>DB: INSERT trips
    loop Lặp từ ngày 1 đến ngày N
        Trip->>DB: INSERT itinerary_days (dayNumber = 1, 2, 3)
    end
    Trip-->>Client: 201 Created (TripEntity kèm danh sách ngày trống)
```

---

### 3.3. Luồng Lịch trình linh hoạt: Cụm địa điểm, Khoảng cách & Khung giờ vàng
*Kế hoạch tiếp theo — Tập trung vào sự thoải mái của người dùng.*

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng
    participant UI as Giao diện Lịch trình
    participant Activity as ActivityService
    participant Distance as DistanceService (Haversine)
    participant DB as PostgreSQL (activities / places)

    User->>UI: Chọn địa điểm thêm vào Ngày 1 (vd: Bán đảo Sơn Trà)
    UI->>Activity: POST /api/trips/{id}/itinerary/activities
    Note over Activity, DB: Tự động trích xuất Khung giờ vàng & tính khoảng cách
    Activity->>Activity: Trích xuất gợi ý khung giờ: "16:00 - 17:30 (Đón hoàng hôn)"
    Activity->>Distance: Tính khoảng cách từ địa điểm liền trước (vd: 3.2 km, ~8 phút đi xe)
    Activity->>DB: Lưu activity kèm orderIndex, distanceMeters, idealTimingTip
    Activity-->>UI: 201 Created (Thẻ hoạt động kèm cự ly di chuyển & Khung giờ đẹp)

    Note over User, DB: Khi người dùng kéo thả (Drag & Drop) đổi thứ tự địa điểm
    User->>UI: Kéo đổi thứ tự để tiện đường hơn
    UI->>Activity: PUT /api/itinerary-days/{dayId}/reorder (mảng activityIds mới)
    Activity->>Distance: Tính lại khoảng cách giữa các điểm theo thứ tự mới
    Activity->>DB: Cập nhật lại orderIndex và distance trong 1 Transaction
    Activity-->>UI: 200 OK (Cung đường đã được tối ưu lại)
```

---

### 3.4. Luồng Theo dõi Ngân sách & Cảnh báo chi tiêu (Budget & Expenses)
*Kế hoạch tiếp theo.*

```mermaid
flowchart TD
    A[Bắt đầu theo dõi ngân sách] --> B[Ngân sách tổng của chuyến đi: budget]
    B --> C[Tổng chi phí dự kiến từ activities: estimatedTotal]
    B --> D[Tổng thực chi ghi nhận từ bảng expenses: actualTotal]
    C & D --> E["remaining = budget - MAX(actualTotal, estimatedTotal)"]
    E --> F{Tỉ lệ đã chi / Ngân sách}
    F -->|Đã chi <= 90%| G["warningLevel: NONE (Chi tiêu an toàn)"]
    F -->|90% < Đã chi <= 100%| H["warningLevel: NEAR_LIMIT (Cảnh báo sắp hết ngân sách)"]
    F -->|Đã chi > 100%| I["warningLevel: OVER (Cảnh báo vượt ngân sách)"]
    G & H & I --> J[Hiển thị biểu đồ phân bổ 6 hạng mục lên giao diện]
```

---

### 3.5. Luồng Trọng tâm: AI Agent Trò chuyện, Giải trình & Hoàn tác an toàn (Proposal, Explain & Undo)
*Kế hoạch tiếp theo — Cốt lõi AI Agent của đề tài.*

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng
    participant UI as Khung Chat AI Sidebar
    participant Agent as AgentService (Spring AI / Gemini)
    participant Tools as Bộ AI Tools (Weather, Routing, Places)
    participant DB as CSDL (proposals / activities)

    User->>UI: "Chiều nay đi biển ngắm hoàng hôn thì ghé quán ăn nào gần đó tiện đường?"
    UI->>Agent: POST /api/trips/{id}/ai/chat (SSE Stream)
    Agent->>Agent: Phân tích ngữ cảnh: Cần tìm quán ăn gần biển + khung giờ chiều tối
    
    rect rgb(240, 248, 255)
        Note over Agent, Tools: Vòng lặp Tool Calling tự động
        Agent->>Tools: search_places("Hải sản ngon gần biển Mỹ Khê", Đà Nẵng)
        Tools-->>Agent: Trả về: Quán Bé Mặn (Cách bãi biển 450m, mở 09:00 - 23:00, 4.4★)
        Agent->>Tools: calculate_distance(Biển Mỹ Khê -> Quán Bé Mặn)
        Tools-->>Agent: 450 mét, đi bộ 6 phút
        Agent->>Tools: propose_itinerary_changes(ADD Quán Bé Mặn, note: "Ghé sau khi ngắm hoàng hôn")
        Tools->>DB: INSERT ai_proposals (status = PENDING, lưu JSON diff & lý do giải trình)
        Tools-->>Agent: Proposal ID: 52
    end

    Agent-->>UI: Stream câu trả lời giải trình + Event `proposal: 52`
    UI->>UI: Hiển thị Thẻ đề xuất kèm giải trình:
    Note over UI: "✧ Vì sao AI chọn chỗ này?<br>1. Tiện đường: Cách bãi tắm chỉ 450m đi bộ.<br>2. Giờ vàng: Ghé lúc 18:00 vừa kịp đón hoàng hôn xong, quán chưa quá đông.<br>3. Đánh giá: 4.4★ (hơn 3.000 review)."

    alt Người dùng bấm [Áp dụng thay đổi]
        User->>UI: Bấm "Apply changes"
        UI->>Agent: POST /api/trips/{id}/ai/apply (proposalId: 52)
        Agent->>DB: Cập nhật activities thật, lưu trạng thái hoàn tác (undo snapshot)
        DB-->>UI: Cập nhật lịch trình + Hiển thị nút "↶ Hoàn tác (trong 10 phút)"
        
        opt Người dùng đổi ý bấm Hoàn tác
            User->>UI: Bấm "↶ Hoàn tác"
            UI->>Agent: POST /api/proposals/52/undo
            Agent->>DB: Khôi phục lại activities về nguyên trạng ban đầu
            DB-->>UI: Lịch trình quay lại như cũ ngay lập tức
        end
    else Người dùng bấm [Từ chối]
        User->>UI: Bấm "Reject"
        UI->>Agent: POST /api/proposals/52/reject
        Agent->>DB: Đánh dấu status = REJECTED (giữ nguyên lịch trình cũ)
    end
```

---

## 4. Lộ trình triển khai các bước tiếp theo

1. **Bước 1: Quản lý Lịch trình linh hoạt & Hoạt động (Itinerary & Activities)**
   - Triển khai CRUD hoạt động (`activities`) tập trung vào **thứ tự tiện đường** và **thẻ ghi chú Khung giờ vàng**.
   - Triển khai `DistanceService` tính khoảng cách (km) và thời gian di chuyển giữa các điểm liền kề.
   - Triển khai API sắp xếp thứ tự kéo thả (`reorder`).
   - Triển khai API lưu địa điểm yêu thích (`saved_places`).
2. **Bước 2: Dịch vụ ngoài (Weather & Currency Exchange)**
   - Tích hợp Open-Meteo theo tọa độ điểm đến.
   - Tích hợp Exchange Rate API quy đổi ngoại tệ.
3. **Bước 3: Quản lý Chi tiêu & Ngân sách (Budget & Expenses)**
   - Triển khai CRUD chi tiêu thực tế (`expenses`).
   - Tích hợp `BudgetService` trả về tổng hợp và cảnh báo hạn mức.
4. **Bước 4: Nền tảng AI Chatbot & Agent Tools (Gemini Integration)**
   - Tích hợp Spring AI / Gemini Client.
   - Xây dựng endpoint SSE Stream `POST /api/trips/{id}/ai/chat`.
   - Cài đặt 5 công cụ đọc (`get_current_trip`, `get_itinerary`, `get_weather`, `search_places`, `calculate_trip_budget`).
5. **Bước 5: Cơ chế Đề xuất, Giải trình & Hoàn tác an toàn (AI Proposal, Explain & Undo)**
   - Xây dựng công cụ `propose_itinerary_changes` kèm metadata giải trình (`evidence_json`, `reason`).
   - API xem trước diff & giải trình (`GET /api/proposals/{id}`).
   - API phê duyệt (`POST /api/trips/{id}/ai/apply`), từ chối (`POST /api/proposals/{id}/reject`) và hoàn tác (`POST /api/proposals/{id}/undo`).
