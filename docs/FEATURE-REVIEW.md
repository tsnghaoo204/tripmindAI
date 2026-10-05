# TripMind — Danh sách tính năng chờ duyệt

**05/10/2026** · Phạm vi: **backend**. Giao diện làm sau.

> **Kết quả duyệt 05/10/2026:** phần B chọn 7 mục đã tick bên dưới. Kế hoạch thực hiện cùng phần còn thiếu của backend nằm ở [BE-ROADMAP.md](BE-ROADMAP.md).

Cách duyệt: tick `[x]` vào việc muốn làm, để trống `[ ]` là cắt hoặc hoãn. Ghi chú thêm ngay dưới dòng nếu muốn sửa.

Ký hiệu:

- **Công sức**: S = vài giờ đến 1 ngày · M = 2–4 ngày · L = trên 1 tuần
- **Gợi ý**: ⭐ nên làm · ◐ cân nhắc · ✗ nên cắt

Mã `FR-xxx` tham chiếu [ADS-01](ADS/ADS-01-Dac-ta-yeu-cau.md).

---

## Phần A — Tính năng đã dự tính trong tài liệu

### A1. Đã có ở backend (không cần duyệt)

- ✅ Đăng ký, đăng nhập JWT, xem/sửa hồ sơ, đổi mật khẩu
- ✅ Điểm đến: phổ biến, tìm kiếm, chi tiết, tự nạp từ Google Places
- ✅ Địa điểm: tìm kiếm, gợi ý gần, chi tiết, dự phòng khi Google lỗi
- ✅ Chuyến đi: tạo (tự sinh các ngày), xem, sửa, xoá
- ✅ Lịch trình: xem theo ngày, thêm/sửa/xoá hoạt động, kéo thả, khoảng cách Haversine, gợi ý khung giờ đẹp
- ✅ Lưu / bỏ lưu địa điểm yêu thích

### A2. Lỗ hổng nhỏ trong phần đã làm

- [ ] ⭐ S — Lưu `travelStyle`, `budgetPreference`, `preferences` khi tạo chuyến. API đang **nhận nhưng bỏ đi**, `TripEntity` không có cột tương ứng. AI cần mấy trường này để cá nhân hoá.
- [ ] ⭐ S — Từ chối chuyến dài quá 30 ngày (FR-104)
- [ ] ⭐ M — Đổi khoảng ngày của chuyến thì sinh thêm / xoá bớt ngày (FR-106)
- [ ] ⭐ S — Lọc chuyến sắp tới / đã qua (FR-107)
- [ ] ◐ S — % tiến độ lập kế hoạch của chuyến (FR-108)
- [ ] ⭐ S — Sở thích du lịch mặc định của tài khoản (FR-007). Bảng `user_preferences` đã có, chưa có API.
- [ ] ✗ M — Refresh token, đăng xuất, thu hồi token (FR-002 → 005). [TODO.md](TODO.md) đã chốt không làm, [PLAN.md](PLAN.md) vẫn ghi MUST. Gợi ý: bỏ và ghi vào phần hạn chế của báo cáo.

### A3. Ngân sách và chi tiêu

- [ ] ⭐ M — Ghi / xem / xoá chi tiêu thực tế theo 6 hạng mục (FR-403, 404)
- [ ] ⭐ S — Tổng hợp ngân sách: ước tính, thực chi, còn lại, cảnh báo 90% / 100% (FR-402, 406)

### A4. Dịch vụ ngoài

- [ ] ⭐ M — Thời tiết từng ngày (Open-Meteo). Trong 16 ngày dùng dự báo, xa hơn dùng trung bình khí hậu, luôn kèm nhãn nguồn (FR-501 → 503)
- [ ] ⭐ S — Dịch vụ ngoài lỗi thì phần đó để trống kèm lý do, không làm hỏng cả trang (FR-505)
- [ ] ✗ M — Quy đổi tỷ giá (FR-504). PLAN xếp đầu danh sách cắt.

### A5. Trợ lý AI — lõi đề tài

- [ ] ⭐ M — Tích hợp Gemini qua Spring AI, chat SSE theo ngữ cảnh chuyến đi (FR-601, 605)
- [ ] ⭐ M — Bộ công cụ đọc: chuyến, lịch trình, sở thích, địa điểm đã lưu, ngân sách, thời tiết, tìm địa điểm, khoảng cách (FR-602, 603)
- [ ] ⭐ S — Lưu lịch sử hội thoại (FR-607)
- [ ] ⭐ S — Giới hạn vòng lặp: tối đa 8 vòng hoặc 60 giây (FR-608)
- [ ] ⭐ S — Ghi nhật ký mọi lần chạy công cụ, kể cả khi lỗi (FR-901, 902)
- [ ] ⭐ L — Tự sinh lịch trình cho chuyến mới, địa điểm phải có thật (FR-609, 610)
- [ ] ⭐ M — Tối ưu thứ tự trong ngày bằng thuật toán, không để mô hình tự đoán (FR-613)
- [ ] ◐ S — Phân tích ngân sách bằng AI (FR-611). Chủ yếu là công cụ đọc ngân sách + prompt.
- [ ] ◐ M — Đổi lịch khi thời tiết xấu (FR-612)
- [ ] ◐ M — Gợi ý địa điểm có chấm điểm bằng mã, `rank_candidates` (ADS-21)
- [ ] ◐ M — Ghi chi tiêu bằng câu chữ: "ăn Bé Mặn hết 1tr2" → phiếu chờ duyệt (`parse_expense`)

### A6. Đề xuất → duyệt → áp dụng — lõi đề tài, PLAN ghi "tuyệt đối không cắt"

- [ ] ⭐ L — AI chỉ dựng đề xuất (thêm / xoá / sửa / sắp lại), kèm chênh lệch chi phí và quãng đường (FR-701 → 703)
- [ ] ⭐ M — Áp dụng trong một giao dịch, kiểm đúng chuyến / đúng người / đang chờ / chưa quá 30 phút, chống áp dụng hai lần (FR-705 → 709)
- [ ] ⭐ S — Từ chối đề xuất, giữ bản ghi (FR-710)
- [ ] ⭐ M — Hoàn tác trong 10 phút, không đè phần người dùng đã sửa tay (FR-1201 → 1207)
- [ ] ⭐ M — Giải trình "Vì sao AI chọn chỗ này?": công cụ nào chạy, ứng viên nào bị loại và vì sao (FR-1208 → 1210)
- [ ] ◐ M — Ràng buộc cứng của chuyến; phương án vi phạm phải ghi rõ (FR-1211, 1212)

### A7. Quản trị

- [ ] ⭐ S — Xem nhật ký chạy công cụ, có lọc (FR-903). Cần cho phần demo minh bạch AI.
- [ ] ◐ S — Thống kê dùng AI: số lượt, token, công cụ hay dùng (FR-904)
- [ ] ◐ S — Danh sách người dùng và chuyến đi ở mức tổng hợp (FR-905)

### A8. Giai đoạn đang đi (v2.0)

Entity `activities` đã có cột `actual_start`, `actual_end`, `skip_reason`, nhưng chưa có API nào dùng.

- [ ] ◐ S — Ba giai đoạn chuyến: chưa đi / đang đi / đã xong (FR-1001, 1002)
- [ ] ◐ M — Đánh dấu hoạt động đã xong / đã bỏ, ghi giờ thực tế, tính lệch lịch (FR-1003 → 1007)
- [ ] ◐ M — AI đề xuất dời phần còn lại khi bị trễ (FR-1008, 1009)
- [ ] ◐ S — Dải "Hôm nay": đang làm gì, kế tiếp là gì (FR-1010)
- [ ] ✗ M — Nhật ký cảm nhận + đính ảnh (FR-1011 → 1013)
- [ ] ✗ S — Nhập chi tiêu bằng giọng nói (FR-1014). Làm ở frontend.

### A9. Sau chuyến (v2.0)

- [ ] ◐ M — Tab "Nhìn lại": làm bao nhiêu, bỏ bao nhiêu, ước tính so với thực tế theo hạng mục (FR-1101 → 1105)
- [ ] ◐ M — Chia tiền nhóm, người đi cùng không cần tài khoản, rút gọn số lượt chuyển (FR-1108 → 1110)
- [ ] ✗ M — Thói quen ước lượng qua nhiều chuyến (FR-1106, 1107). Cần ≥2 chuyến đã xong mới có dữ liệu để demo.

### A10. Hạ tầng

- [ ] ⭐ S — Chuyển khoá API khỏi `application.yml` sang `.env` (NFR-06) — **làm ngay**
- [ ] ⭐ S — Flyway thay cho chạy tay `schemas.sql` (NFR-09)
- [ ] ⭐ S — Docker Compose: postgres, redis, backend (NFR-12)
- [ ] ◐ S — Giới hạn tần suất gọi AI (NFR-07)

---

## Phần B — Tính năng mới đề xuất

Các ý dưới đây xuất phát từ những câu người đi du lịch hay tự hỏi mà tài liệu hiện tại chưa trả lời. Phần lớn **dùng lại dữ liệu và luồng đề xuất đã có**, nên tốn ít công mà làm phần AI mạnh hơn.

### B1. Nên làm

- [ ] ⭐ M — **Kiểm tra lịch trình** — *"Mình có quên gì không? Lịch có hợp lý không?"*
  Bộ luật chạy bằng mã, trả danh sách cảnh báo cho từng ngày:
  - Đặt hoạt động vào giờ / ngày địa điểm đóng cửa (dùng `opening_hours` đã lưu từ Google)
  - Hai hoạt động trùng giờ
  - Ngày quá dày so với phong cách đi (RELAXED / BALANCED / FAST_PACED)
  - Không có bữa trưa / tối trong khung 11–13h hoặc 18–20h
  - Hoạt động ngoài trời rơi vào ngày dự báo mưa
  - Một chặng di chuyển quá xa
  - Vượt ngân sách

  AI đọc danh sách cảnh báo rồi dựng đề xuất sửa. Đây là ví dụ rõ nhất cho nguyên tắc "tính bằng mã, AI chỉ giải thích và đề xuất", rất hợp để trình bày trong báo cáo.

- [ ] ⭐ S — **Khoá hoạt động có giờ cố định** — *"Chuyến bay 7h sáng, đừng ai đụng vào."*
  Thêm cờ `is_locked` cho hoạt động. Thuật toán tối ưu và AI không được dời hoặc xoá hoạt động đã khoá. Rẻ, nhưng làm đề xuất của AI đáng tin hơn hẳn.

- [ ] ⭐ M — **Nơi lưu trú và giờ rảnh mỗi ngày** — *"Ngày đầu 3h chiều mới tới, ngày cuối 10h phải ra sân bay."*
  - Gắn khách sạn cho từng đêm. Khoảng cách tính **từ khách sạn** tới điểm đầu tiên và từ điểm cuối về khách sạn. Hiện giờ chỉ tính giữa các hoạt động với nhau.
  - Mỗi ngày có giờ bắt đầu / kết thúc khả dụng. AI và bước kiểm tra lịch trình dùng hai mốc này.

- [ ] ⭐ M — **Danh sách chờ xếp lịch của chuyến** — *"Mình gom các chỗ muốn đi trước, xếp ngày sau."*
  Mỗi chuyến có một danh sách địa điểm chưa gán ngày. Người dùng bấm "Xếp giúp tôi", AI gom các chỗ gần nhau vào cùng ngày rồi dựng **một đề xuất** để duyệt. Đúng cách người thật lên kế hoạch, và là màn demo AI rất trực quan.

- [x] ⭐ S — **Hôm nay còn tiêu được bao nhiêu** — *"Tiêu thế này có lố không?"*
  (Ngân sách − đã chi) ÷ số ngày còn lại. Một phép tính nhỏ trên dữ liệu chi tiêu, nhưng là câu người ta hỏi nhiều nhất khi đang đi.

- [x] ⭐ S — **Gợi ý chi phí ước tính** — *"Không biết điền bao nhiêu tiền."*
  Khi thêm hoạt động từ Google Places, tự điền khoảng giá theo `price_level` (đã lưu sẵn) × số người. Người dùng sửa được. Tổng ước tính sẽ sát hơn mà không bắt người dùng tự đoán.

### B2. Cân nhắc

- [x] ◐ M — **Checklist chuẩn bị** — *"Mang gì, cần làm gì trước khi đi?"*
  AI gợi ý đồ cần mang theo thời tiết và loại hoạt động (đi biển → kem chống nắng, mưa → áo mưa) cùng việc cần làm trước chuyến (đổi tiền, check-in online). Gợi ý đi qua luồng đề xuất, người dùng tick dần. Cần thêm một bảng.

- [x] ◐ S — **Thông tin nhóm đi** — *"Đi với trẻ nhỏ / ông bà / có người ăn chay."*
  Vài cờ trên chuyến đi. AI và bước kiểm tra lịch trình dùng chúng để giảm nhịp độ, tránh chặng đi bộ dài, lọc quán ăn.

- [x] ◐ M — **Xuất lịch trình ra file `.ics` và PDF** — *"Gửi cho người đi cùng, mở trong Google Calendar."*
  Người đi cùng không có tài khoản, nên xuất file là cách chia sẻ không làm đổi mô hình quyền.

- [x] ◐ S — **Nhân bản chuyến cũ** — *"Năm ngoái đi Đà Lạt, năm nay đi lại."*
  Sao chép chuyến cũ sang khoảng ngày mới, giữ hoạt động, bỏ chi tiêu.

- [x] ◐ M — **Đánh giá địa điểm sau chuyến** — *"Chỗ này đáng quay lại, chỗ kia không."*
  Ở tab Nhìn lại, chấm từng chỗ thích / không thích. Lần lập chuyến sau, AI đọc lịch sử này để tránh gợi ý lại chỗ đã chê.

- [ ] ◐ M — **Thời gian di chuyển thực theo phương tiện** — *"Khoảng cách đường chim bay không đúng."*
  Thay Haversine bằng OSRM (miễn phí) hoặc nhân hệ số đường vòng theo xe máy / ô tô / đi bộ.

### B3. Để vào "Hướng phát triển" của báo cáo, không làm bây giờ

- [ ] ✗ L — **Chuyến nhiều điểm đến** (Hà Nội → Ninh Bình → Hạ Long). Rất sát nhu cầu thật nhưng phải đổi mô hình dữ liệu: một chuyến hiện chỉ có một điểm đến.
- [ ] ✗ M — **Cẩm nang điểm đến do AI viết** (phong tục, ổ cắm điện, số khẩn cấp). Dễ bịa, trái nguyên tắc "không truy được nguồn thì nói không biết".
- [ ] ✗ L — **Cùng lập kế hoạch với người khác / chia sẻ link**. Đổi mô hình quyền, ADS-01 §6.1 đã loại.

---

## Phần C — Ước lượng thời gian

Còn khoảng **7 tuần**, trong đó phải dành cả thời gian cho frontend, deploy và báo cáo.

| Gói | Công sức backend ước tính |
|---|---|
| Toàn bộ ⭐ của phần A | ~4 tuần |
| Toàn bộ ⭐ của phần B | ~1,5 tuần |
| Mọi mục ◐ | thêm ~4–5 tuần |

Nếu duyệt hết các mục ⭐ ở cả hai phần, backend mất khoảng 5,5 tuần, chỉ còn khoảng 1,5 tuần cho frontend. Như vậy là **quá căng**. Nên giữ toàn bộ ⭐ của phần A, rồi chọn 2–3 mục ⭐ ở phần B, ưu tiên **Kiểm tra lịch trình**, **Khoá hoạt động** và **Danh sách chờ xếp lịch**, vì ba mục này làm phần AI nổi bật hơn khi bảo vệ.
