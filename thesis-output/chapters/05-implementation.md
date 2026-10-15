# CHƯƠNG 4. CÀI ĐẶT VÀ TRIỂN KHAI HỆ THỐNG TRIPMIND

## 4.1. Cài đặt Backend

### 4.1.1. Công nghệ sử dụng

@table[T-BE]{Công nghệ sử dụng phía Backend}{3.4,3.6,9.0}
| Thành phần | Công nghệ | Vai trò và lý do lựa chọn |
|---|---|---|
| Ngôn ngữ | Java 21 | Ngôn ngữ ổn định, kiểu tĩnh, hỗ trợ luồng ảo giúp xử lý hiệu quả nhiều tác vụ chờ vào ra như gọi AI và API bên ngoài. |
| Framework | Spring Boot 3.4 | Cấu hình tự động, máy chủ web nhúng, hỗ trợ kiểm tra dữ liệu, giao dịch và lập lịch tác vụ định kỳ. |
| Bảo mật | Spring Security, JWT | Chuỗi bộ lọc xác thực không trạng thái, phân quyền theo vai trò; mật khẩu băm bằng BCrypt. |
| Truy cập dữ liệu | Spring Data JPA (Hibernate) | Ánh xạ thực thể – bảng, truy vấn khai báo, kiểm tra khớp lược đồ khi khởi động. |
| Cơ sở dữ liệu | PostgreSQL 16 | Toàn vẹn dữ liệu với khóa ngoại, ràng buộc kiểm tra, kiểu JSONB, ràng buộc duy nhất kiểm tra trễ. |
| Bộ nhớ đệm, hàng đợi | Redis 7.4, Redisson | Giới hạn tần suất, bộ đệm thời tiết và địa điểm, hàng đợi tác vụ sinh lịch trình, khóa phân tán, phát – nhận tiến độ. |
| Tích hợp AI | Spring AI 1.0 | Kết nối dịch vụ LLM theo chuẩn OpenAI, khai báo công cụ cho mô hình gọi. |
| Tài liệu API | OpenAPI (Swagger UI) | Tự sinh tài liệu API, có thể tắt khi triển khai. |
| Kiểm thử | JUnit 5, Mockito, Testcontainers | Kiểm thử đơn vị và kiểm thử tích hợp với PostgreSQL, Redis thật trong vùng chứa. |

### 4.1.2. Mô hình kiến trúc Backend

- Lớp bộ lọc: giới hạn kích thước nội dung yêu cầu, xác thực mã phiên và đối chiếu phiên bản mã với tài khoản, giới hạn tần suất truy cập.
- Lớp điều khiển: định tuyến yêu cầu tới đúng chức năng, kiểm tra dữ liệu đầu vào theo các quy tắc ràng buộc, lấy định danh người dùng hiện tại và đóng gói phản hồi JSON theo cấu trúc thống nhất.
- Lớp dịch vụ: thực hiện quy tắc nghiệp vụ như kiểm tra quyền sở hữu, giới hạn độ dài chuyến đi, tính ngân sách, áp dụng đề xuất, sinh gợi ý danh sách chuẩn bị.
- Mô-đun tác tử AI: vòng lặp hội thoại, bộ thực thi công cụ, quản lý đề xuất, tiến trình sinh lịch trình chạy nền.
- Lớp truy cập dữ liệu: thực hiện các thao tác thêm, đọc, sửa, xóa trên cơ sở dữ liệu, tách biệt khỏi nghiệp vụ.
- Bộ kết nối dịch vụ ngoài: đóng gói việc gọi dịch vụ LLM, Google Places, Open-Meteo; xử lý thời gian chờ và lỗi để các dịch vụ phía trên có thể suy giảm chức năng một cách an toàn.
- Bộ xử lý lỗi tập trung: chuyển mọi ngoại lệ thành mã lỗi nghiệp vụ và mã trạng thái HTTP phù hợp, không làm lộ thông tin nội bộ.

@fig{DGM-ARC-002-backend}{Mô hình kiến trúc Backend}

### 4.1.3. Cơ chế bảo mật

@table[T-SEC]{Các biện pháp bảo mật đã cài đặt}{4.2,11.8}
| Biện pháp | Cách thực hiện |
|---|---|
| Xác thực | Mã phiên có chữ ký, hiệu lực mặc định 24 giờ, gửi kèm trong tiêu đề mỗi yêu cầu. |
| Thu hồi phiên | Mỗi tài khoản có số phiên bản mã; đổi mật khẩu hoặc bị khóa sẽ tăng số phiên bản làm các mã cũ mất hiệu lực. |
| Phân quyền | Khu vực quản trị chỉ dành cho vai trò Quản trị viên; dữ liệu cá nhân chỉ chủ sở hữu truy cập được. |
| Chống dò mật khẩu | Tạm khóa đăng nhập sau 5 lần sai theo email (hoặc 20 lần theo địa chỉ IP) trong 5 phút. |
| Giới hạn tần suất | 120 yêu cầu mỗi phút cho mỗi người dùng hoặc địa chỉ IP; 20 lượt sử dụng AI mỗi giờ cho mỗi người dùng. |
| Giới hạn dữ liệu vào | Kiểm tra độ dài, định dạng từng trường; nội dung yêu cầu tối đa 1 MB; đường dẫn ảnh phải dùng giao thức https. |
| An toàn khi dùng AI | Định danh chuyến đi và người dùng do máy chủ cung cấp cho công cụ, không lấy từ mô hình; nội dung cộng đồng được đánh dấu là dữ liệu không tin cậy; đề xuất chỉ áp dụng khi người dùng đồng ý. |
| Cấu hình | Khóa bí mật và thông tin truy cập đặt trong biến môi trường; chỉ cho phép các nguồn giao diện đã cấu hình; cơ sở dữ liệu và Redis chỉ mở trong mạng nội bộ. |

### 4.1.4. Xử lý nền và truyền dữ liệu thời gian thực

Trò chuyện với trợ lý AI sử dụng Server-Sent Events: các bước kiểm tra quyền, cấu hình AI và hạn mức được thực hiện trước khi mở luồng để lỗi được trả về đúng mã trạng thái; sau đó lượt hội thoại chạy trên luồng ảo và lần lượt gửi các sự kiện bắt đầu, kết thúc công cụ, đề xuất, nội dung trả lời và hoàn tất. Tác vụ sinh lịch trình được đưa vào hàng đợi tin cậy trên Redis, do hai tiến trình xử lý nền tiêu thụ; tiến độ được phát qua kênh phát – nhận để giao diện nhận theo thời gian thực, đồng thời giao diện có cơ chế hỏi trạng thái định kỳ khi kết nối bị gián đoạn. Hai tác vụ định kỳ chạy mỗi phút để đánh dấu các đề xuất quá hạn và đưa lại vào hàng đợi các tác vụ bị treo.

## 4.2. Cài đặt Frontend

### 4.2.1. Công nghệ sử dụng

@table[T-FE]{Công nghệ sử dụng phía Frontend}{3.4,3.6,9.0}
| Thành phần | Công nghệ | Vai trò và lý do lựa chọn |
|---|---|---|
| Thư viện giao diện | React 19 | Mô hình thành phần dễ tái sử dụng, hệ sinh thái lớn. |
| Ngôn ngữ | TypeScript 5 | Kiểu tĩnh giúp phát hiện lỗi sớm và đồng bộ kiểu dữ liệu với API. |
| Công cụ dựng | Vite 6 | Máy chủ phát triển khởi động nhanh, đóng gói tối ưu cho môi trường triển khai. |
| Định tuyến | TanStack Router | Định tuyến theo cấu trúc tệp, bảo vệ trang yêu cầu đăng nhập và trang quản trị. |
| Dữ liệu máy chủ | TanStack Query, Axios | Lưu đệm, làm mới dữ liệu, quản lý trạng thái đang tải, lỗi; tự gắn mã phiên vào yêu cầu và đăng xuất khi phiên hết hiệu lực. |
| Giao diện | Tailwind CSS 4, Radix UI | Giao diện đáp ứng, chế độ sáng/tối, hộp thoại và thẻ chuyển trang tuân thủ chuẩn truy cập. |
| Bản đồ | Leaflet | Hiển thị điểm đánh dấu và tuyến di chuyển trong ngày, vị trí kết quả tìm kiếm địa điểm. |
| Kiểm thử | Vitest | Kiểm thử đơn vị cho các mô-đun xử lý dữ liệu và luồng sự kiện. |

### 4.2.2. Mô hình kiến trúc Frontend

- Các thành phần giao diện: trang và hộp thoại theo từng nhóm chức năng (chuyến đi, lịch trình, trợ lý AI, ngân sách, danh sách chuẩn bị, cộng đồng, quản trị) cùng bộ thành phần dùng chung.
- Lớp định tuyến: điều hướng giữa các trang, chuyển hướng về trang đăng nhập khi chưa có phiên và chặn người dùng thường vào trang quản trị.
- Lớp biểu mẫu và kiểm tra dữ liệu: kiểm tra dữ liệu nhập trước khi gửi lên máy chủ.
- Lớp truy vấn: gọi dữ liệu, lưu đệm và làm mới sau mỗi thao tác thay đổi.
- Lớp API: gửi yêu cầu HTTP, đọc luồng sự kiện SSE, chuẩn hóa thông báo lỗi.
- Trạng thái xác thực: lưu phiên trên trình duyệt và thông báo cho toàn ứng dụng khi đăng nhập hoặc đăng xuất.

@fig{DGM-ARC-003-frontend}{Mô hình kiến trúc Frontend}

## 4.3. Cài đặt tác tử AI

Tác tử AI được cài đặt dưới dạng vòng lặp do ứng dụng tự điều phối thay vì để thư viện tự động thực thi công cụ. Cách làm này cho phép hệ thống kiểm soát số vòng, thời gian, loại bỏ các lời gọi công cụ trùng lặp, cắt ngắn kết quả quá dài và ghi nhật ký mọi lần thực thi kể cả khi thất bại. Kết nối tới dịch vụ LLM hỗ trợ danh sách mô hình dự phòng: khi một mô hình lỗi tạm thời, hệ thống thử lại rồi chuyển sang mô hình kế tiếp trong giới hạn thời gian.

@fig{DGM-ARC-004-tac-tu-ai}{Vòng lặp xử lý của tác tử AI}

@table[T-TOOLS]{Các nhóm công cụ của trợ lý AI}{3.6,12.4}
| Nhóm | Công cụ |
|---|---|
| Đọc dữ liệu chuyến đi | Lấy thông tin chuyến đi hiện tại; lấy lịch trình; lấy sở thích người dùng; lấy địa điểm đã lưu; tính ngân sách; lấy thời tiết; tìm mẹo từ tri thức điểm đến. |
| Địa điểm | Tìm kiếm địa điểm thực (loại trừ địa điểm người dùng không thích); tính khoảng cách; đề xuất danh sách địa điểm. |
| Đề xuất thay đổi | Tạo đề xuất thêm, xóa, cập nhật, sắp xếp lại hoạt động; tối ưu thứ tự hoạt động trong ngày theo quãng đường (thuật toán láng giềng gần nhất kết hợp cải tiến 2-opt). |
| Danh sách chuẩn bị | Gợi ý các mục chuẩn bị dựa trên quy tắc và thời tiết. |

Khi tạo đề xuất, hệ thống kiểm tra từng thao tác với dữ liệu thực của chuyến đi, loại bỏ thao tác không hợp lệ kèm lý do, từ chối toàn bộ đề xuất nếu có hai thao tác tác động lên cùng một hoạt động, và tính chênh lệch chi phí, thời gian di chuyển. Khi áp dụng, hệ thống lưu “dấu vân tay” nội dung của từng hoạt động bị tác động; khi hoàn tác, hoạt động nào đã được người dùng chỉnh sửa thủ công sau đó sẽ được giữ nguyên thay vì ghi đè. Với chức năng sinh lịch trình, mỗi hoạt động do mô hình đề xuất phải khớp một địa điểm trong danh sách đã thu thập hoặc một kết quả tìm kiếm có tên tương đồng tối thiểu 50%; các mục không đạt bị loại và được báo lại cho người dùng.

## 4.4. Triển khai hệ thống

Hệ thống được đóng gói bằng Docker Compose gồm ba vùng chứa: máy chủ ứng dụng Backend (chạy với người dùng không có đặc quyền và có kiểm tra tình trạng hoạt động), PostgreSQL 16 và Redis 7.4; dữ liệu được lưu trên các ổ lưu trữ riêng, cổng cơ sở dữ liệu và Redis chỉ mở trong phạm vi máy chủ. Lược đồ cơ sở dữ liệu được khởi tạo bằng kịch bản lược đồ trước khi chạy ứng dụng, ứng dụng kiểm tra sự khớp giữa thực thể và lược đồ khi khởi động. Lịch trình có thể được xuất ra tệp lịch theo chuẩn iCalendar [6] để nhập vào các ứng dụng lịch. Frontend được dựng thành các tệp tĩnh và phục vụ độc lập với Backend. Quy trình tích hợp liên tục trên GitHub Actions tự động biên dịch và chạy kiểm thử Backend, đồng thời kiểm tra kiểu, kiểm tra quy tắc mã, chạy kiểm thử và dựng Frontend trên hai phiên bản Node.js mỗi khi có thay đổi.

@fig{DGM-ARC-005-trien-khai}{Mô hình triển khai hệ thống}

## 4.5. Giao diện chương trình

### 4.5.1. Phía người dùng

@fig{SHOT-001-dang-nhap}{Giao diện màn hình "Đăng nhập"}

@fig{SHOT-002-dang-ky}{Giao diện màn hình "Đăng ký"}

@fig{SHOT-003-trang-chu}{Giao diện trang chủ "Kế hoạch du lịch"}

@fig{SHOT-004-tao-chuyen-di}{Giao diện hộp thoại "Lập kế hoạch chuyến đi mới"}

@fig{SHOT-005-lich-trinh}{Giao diện "Lịch trình chi tiết" kèm bản đồ trong ngày}

@fig{SHOT-006-sinh-lich-trinh}{Giao diện theo dõi tiến độ sinh lịch trình bằng AI}

@fig{SHOT-007-tro-ly-ai}{Giao diện "Trợ lý AI" và thẻ đề xuất thay đổi}

@fig{SHOT-008-ngan-sach}{Giao diện "Ngân sách & Chi tiêu"}

@fig{SHOT-009-checklist}{Giao diện "Checklist chuẩn bị" và gợi ý}

@fig{SHOT-010-dia-diem}{Giao diện "Khám phá địa điểm"}

@fig{SHOT-011-cong-dong}{Giao diện "Cộng đồng & Review"}

### 4.5.2. Phía quản trị viên

@fig{SHOT-012-quan-tri-nguoi-dung}{Giao diện "Quản trị hệ thống" – quản lý người dùng}

@fig{SHOT-013-quan-tri-tri-thuc}{Giao diện "Quản trị hệ thống" – quản lý tri thức điểm đến}

@fig{SHOT-014-quan-tri-thong-ke}{Giao diện "Quản trị hệ thống" – thống kê sử dụng AI}
