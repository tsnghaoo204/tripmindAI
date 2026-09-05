# TripMind — bản demo giao diện

Một file HTML duy nhất: **[`tripmind-demo.html`](tripmind-demo.html)**.
Bấm đúp để mở bằng trình duyệt, không cần cài gì, không cần máy chủ.

Dữ liệu lưu vào `localStorage` (ảnh lưu vào IndexedDB), nên bạn tạo chuyến, ghi chi
tiêu, duyệt đề xuất xong tải lại trang vẫn còn nguyên.

> Font tải từ Google Fonts. Không có mạng thì trang vẫn chạy đủ chức năng, chỉ khác kiểu chữ.

## Tài khoản

| Email | Mật khẩu | Vai trò |
|---|---|---|
| `an@tripmind.vn` | `123456` | Người đi du lịch — có sẵn 2 chuyến |
| `admin@tripmind.vn` | `admin123` | Quản trị — xem nhật ký chạy công cụ |

## Ba giai đoạn của một chuyến đi

App theo chuyến đi qua cả ba giai đoạn, và **cùng một màn hình nói chuyện khác nhau**
ở mỗi giai đoạn.

| Giai đoạn | Màn Lịch trình | Thanh đo trên thẻ chuyến |
|---|---|---|
| Chưa đi | Danh sách ngày, kéo thả được | **% đã lên lịch** — số ngày có ≥2 hoạt động |
| Đang đi | Thêm dải **Hôm nay** + nút đánh dấu từng hoạt động | **% đã đi qua** — số hoạt động đã xong hoặc đã bỏ |
| Đã xong | Thêm tab **Nhìn lại** | **% đã đi qua** |

Hôm nay của bản demo cố định là **19/10/2026**, chuyến Đà Nẵng bắt đầu 20/10 — nên nó
không bao giờ tự vào giai đoạn *đang đi*. Bấm **Đổi giai đoạn** ở thanh tiêu đề để xem.
Khi đặt tay, mọi màn hình đều ghi rõ **"bạn tự đặt"** và **"(mô phỏng)"** — giao diện
không giả vờ hôm nay là ngày 1.

## Cơ sở dữ liệu không chứa danh mục địa điểm

Ba tầng, không phải một:

| Tầng | Chứa gì | Sống bao lâu |
|---|---|---|
| Nhà cung cấp ngoài | 45 địa điểm | Không thuộc hệ thống |
| Bộ đệm tra cứu | Kết quả các lượt tìm gần đây | 1 giờ, hết hạn là mất |
| **Cơ sở dữ liệu** | **Chỉ chỗ bạn đã chọn** | Còn mãi |

Bảng địa điểm khởi đầu 10 dòng, mỗi dòng ghi rõ `adoptedVia` — vì sao nó có mặt:
`SAVED`, `ITINERARY`, `PROPOSAL`, hay `AI_GENERATE`. Cả bốn đều là hành động của bạn.
Tra cứu không ghi gì xuống. Xem bảng đếm ở **Hồ sơ → Cơ sở dữ liệu của bạn**.

## Trợ lý: 14 công cụ, 3 công cụ ghi, 0 công cụ chạm dữ liệu thật

`propose_places`, `propose_expense`, `propose_itinerary_changes` chỉ ghi vào bảng đề
xuất. Mọi thứ vào cơ sở dữ liệu đều đi qua một cú bấm của bạn.

- **Gợi ý địa điểm** — hỏi nhà cung cấp, chấm điểm bằng mã, trả thẻ ứng viên kèm lý do.
- **Phương án tối ưu** — dựng vài phương án rồi *đo từng cái bằng mã*: chi phí, quãng
  đường, số hoạt động ngoài trời rơi vào ngày **dự báo** mưa. Ghi rõ ràng buộc nào
  không giữ được.
- **Ghi chi tiêu bằng câu chữ** — "hôm qua ăn ở Bé Mặn hết 1tr2, mình trả" thành một
  phiếu chờ bạn duyệt. Đọc được `450k`, `1tr2`, `hai trăm rưỡi`, `450.000 đ`.
  Có nút micro ở nơi trình duyệt hỗ trợ tiếng Việt.
- **Dời lịch khi trễ** — đánh dấu hoạt động đã xong, hệ thống tính ra bạn trễ bao
  nhiêu, trợ lý dựng đề xuất dời phần còn lại. Nó **không tự dời**.

### Hoàn tác và giải trình

**↶ Hoàn tác** trên thẻ đề xuất đã áp dụng, trong 10 phút. Nếu bạn đã sửa tay một hoạt
động sau khi áp dụng thì hoàn tác **giữ nguyên phần bạn sửa** và nói rõ là lịch trình
không quay về đúng như trước. Địa điểm đã ghi vào cơ sở dữ liệu cũng không rút lại —
hộp xác nhận nói cả hai điều đó trước khi bạn bấm.

**✧ AI tạo · vì sao?** trên hoạt động do trợ lý thêm: công cụ nào chạy mất bao lâu, đã
cân nhắc chỗ nào, **chỗ nào bị loại và vì sao**. Toàn bộ là dòng thật trong nhật ký,
đúng những dòng quản trị viên nhìn thấy. Không truy được nguồn thì nói thẳng là không
biết.

## Chia tiền và nhìn lại

Người đi cùng **không cần tài khoản**, chỉ cần một cái tên. Mỗi khoản chi ghi ai trả và
chia cho ai; khối **Chia tiền** rút gọn thành ít lượt chuyển nhất, tính theo **từng loại
tiền tệ một** (chuyến Đà Nẵng có khoản USD nên ra hai nhóm riêng).

Tab **Nhìn lại** của chuyến đã kết thúc có khối *"Bạn ước sai ở đâu"* — so ước tính với
thực tế theo hạng mục. Hạng mục thiếu một trong hai vế thì ghi **chưa đủ dữ liệu**, không
hiện 0% hay vô cực. Hệ số này **không** được cộng vào tổng ước tính ở tab Ngân sách.

## Kịch bản 18 bước

Bảng góc dưới trái (thu gọn sẵn). Dẫn đi hết mọi luồng, từ trang giới thiệu tới nhật ký
chạy công cụ bên quản trị.

Nút **Tình huống hỏng** bật tám ca lỗi: dịch vụ địa điểm hỏng, thời tiết hỏng, mô hình
hỏng, vượt giới hạn tần suất AI, đề xuất quá hạn, cửa sổ hoàn tác đã đóng, giả lập giờ
16:10, mạng chậm.

Về trạng thái ban đầu: Hồ sơ → *Đặt lại dữ liệu demo*.

## Tài liệu

Sáu tài liệu ADS đã được cập nhật lên **v2.0** cho khớp bản demo, cùng `schemas.sql`:

| Tài liệu | Thay đổi chính |
|---|---|
| [`ADS-01`](../docs/ADS/ADS-01-Dac-ta-yeu-cau.md) | Thêm `FR-10xx` (đang đi), `FR-11xx` (sau chuyến), `FR-12xx` (hoàn tác & giải trình) |
| [`ADS-02`](../docs/ADS/ADS-02-Tu-dien-Mo-hinh-mien.md) | Thêm trạng thái hoạt động, giai đoạn chuyến, loại đề xuất, lý do địa điểm có mặt |
| [`ADS-20`](../docs/ADS/ADS-20-Thiet-ke-CSDL.md) | **Đảo `DI-8`**; thêm 3 bảng; giải thích vì sao không có cột `actual_cost` và vì sao hoàn tác dùng nhật ký nghịch đảo |
| [`ADS-21`](../docs/ADS/ADS-21-Tich-hop-AI-Agent.md) | 9 → 14 công cụ, 1 → 2 loại đề xuất, thêm §4.5 hoàn tác và §4.6 giải trình |
| [`ADS-30`](../docs/ADS/ADS-30-Hop-dong-API.md) | `GET /api/places` không còn ghi vào CSDL; thêm §10a hoàn tác, §10b giai đoạn/nhật ký/chia tiền |
| [`ADS-40`](../docs/ADS/ADS-40-Luong-giao-dien.md) | Bảy chỗ phải nói thật → **mười**; thêm phần H về ba giai đoạn |
