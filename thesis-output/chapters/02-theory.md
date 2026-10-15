# CHƯƠNG 1. CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ

## 1.1. Tổng quan về nền tảng TripMind

### 1.1.1. Định nghĩa

TripMind là một nền tảng web hỗ trợ lập kế hoạch và quản lý chuyến du lịch. Hệ thống cho phép người dùng tạo chuyến đi, xây dựng lịch trình chi tiết theo từng ngày, theo dõi ngân sách và chi tiêu, chuẩn bị đồ dùng, đồng thời sử dụng một trợ lý AI có khả năng đọc dữ liệu chuyến đi, tra cứu địa điểm thực và đề xuất thay đổi để người dùng xem xét, chấp thuận.

@fig{ASSET-IMG-001}{Minh họa đề tài nền tảng lập kế hoạch du lịch thông minh}

### 1.1.2. Đặc điểm

- Lấy chuyến đi làm trung tâm: mọi dữ liệu như lịch trình, chi tiêu, danh sách chuẩn bị, hội thoại với AI đều gắn với một chuyến đi cụ thể.
- Vòng đời ba giai đoạn: trước chuyến đi, trong chuyến đi và sau chuyến đi; giai đoạn được tính theo ngày tại múi giờ của điểm đến và có thể điều chỉnh thủ công.
- AI đóng vai trò tác tử chứ không chỉ là chatbot: trợ lý được phép gọi các công cụ nghiệp vụ như tra cứu lịch trình, tính ngân sách, xem thời tiết, tìm địa điểm, nhưng không tự ghi dữ liệu.
- Con người ra quyết định cuối cùng: mọi thay đổi do AI đề xuất được lưu thành một “đề xuất” chờ duyệt, người dùng có thể áp dụng, từ chối hoặc hoàn tác trong một khoảng thời gian.
- Chống ảo giác dữ liệu: địa điểm AI sử dụng phải là địa điểm có thật được trả về từ dịch vụ tìm kiếm hoặc đã tồn tại trong cơ sở dữ liệu.
- Tri thức cộng đồng: người dùng chia sẻ kinh nghiệm, quản trị viên chắt lọc thành tập tri thức điểm đến để bổ sung ngữ cảnh cho AI.

### 1.1.3. Lợi ích

- Đối với người dùng:
  - Rút ngắn thời gian lên kế hoạch nhờ phân tích mô tả tự nhiên và sinh lịch trình tự động.
  - Quản lý tập trung lịch trình, chi phí, thời tiết và danh sách chuẩn bị trên một nền tảng.
  - Chủ động kiểm soát kế hoạch vì AI chỉ đề xuất, không tự ý thay đổi dữ liệu.
- Đối với nhóm phát triển:
  - Rèn luyện kỹ năng phân tích nghiệp vụ, thiết kế kiến trúc phân lớp và cơ sở dữ liệu quan hệ.
  - Tiếp cận kỹ thuật tích hợp mô hình ngôn ngữ lớn có gọi công cụ, xử lý tác vụ nền và truyền dữ liệu thời gian thực.
  - Tích lũy kinh nghiệm bảo mật, kiểm thử tự động và tích hợp liên tục.

### 1.1.4. Thách thức

- Độ tin cậy của AI: mô hình ngôn ngữ có thể sinh ra địa điểm, giá cả hoặc giờ mở cửa không chính xác, cần cơ chế kiểm chứng.
- Kiểm soát thay đổi: việc AI sửa lịch trình phải minh bạch, có thể xem trước, từ chối và hoàn tác mà không làm mất chỉnh sửa thủ công của người dùng.
- Phụ thuộc dịch vụ bên ngoài: dịch vụ địa điểm, thời tiết và mô hình ngôn ngữ có thể chậm hoặc lỗi; hệ thống cần suy giảm chức năng một cách an toàn thay vì ngừng hoạt động.
- Chi phí và lạm dụng: các lời gọi AI tốn chi phí nên cần giới hạn tần suất theo người dùng.

### 1.1.5. Xu hướng phát triển

- Trợ lý du lịch dựa trên tác tử AI có khả năng phối hợp nhiều công cụ và nguồn dữ liệu.
- Cá nhân hóa kế hoạch dựa trên sở thích, lịch sử đánh giá địa điểm và đặc điểm đoàn đi.
- Kết hợp nội dung do cộng đồng tạo ra với dữ liệu có cấu trúc để nâng cao chất lượng gợi ý.

## 1.2. Mô hình ngôn ngữ lớn và AI Agent

### 1.2.1. Mô hình ngôn ngữ lớn

Mô hình ngôn ngữ lớn (LLM) là mô hình học sâu được huấn luyện trên tập văn bản rất lớn, có khả năng hiểu và sinh văn bản tự nhiên. Trong ứng dụng, LLM được dùng để hiểu yêu cầu của người dùng, trích xuất thông tin có cấu trúc từ câu mô tả và soạn thảo văn bản. Hạn chế cố hữu của LLM là hiện tượng “ảo giác” — sinh ra thông tin nghe hợp lý nhưng không có thật — và không tự biết dữ liệu riêng của người dùng.

### 1.2.2. Gọi công cụ và vòng lặp tác tử

Gọi công cụ (tool calling) là cơ chế cho phép LLM, thay vì trả lời ngay, yêu cầu ứng dụng thực thi một hàm đã được khai báo kèm tham số. Ứng dụng thực thi hàm, gửi kết quả lại cho mô hình và lặp lại cho tới khi mô hình đưa ra câu trả lời cuối cùng. Chuỗi bước “suy luận – gọi công cụ – quan sát kết quả” này tạo thành vòng lặp tác tử. Cách tiếp cận này được đề xuất và phân tích trong các nghiên cứu về tác tử kết hợp suy luận với hành động [3] và mô hình ngôn ngữ biết sử dụng công cụ [4]. Trong TripMind, vòng lặp được giới hạn số lượt và thời gian, mỗi lần gọi công cụ đều được ghi nhật ký để phục vụ kiểm tra.

### 1.2.3. Con người trong vòng lặp và kiểm chứng kết quả

Để bảo đảm an toàn dữ liệu, hệ thống áp dụng nguyên tắc con người trong vòng lặp (human-in-the-loop): AI chỉ được tạo đề xuất, còn việc ghi thay đổi do người dùng quyết định. Bên cạnh đó, mọi địa điểm do AI đưa ra đều được đối chiếu với kết quả tìm kiếm thực; mục nào không xác minh được bị loại kèm lý do. Hệ thống cũng bổ sung ngữ cảnh cho mô hình bằng tập tri thức điểm đến được quản trị viên kiểm duyệt.

## 1.3. Công nghệ phía máy chủ

### 1.3.1. Java và Spring Boot

Spring Boot là framework phát triển ứng dụng Java phổ biến, cung cấp cấu hình tự động, máy chủ web nhúng và hệ sinh thái thư viện phong phú. Hệ thống sử dụng Java 21 và Spring Boot 3.4 [7]. Lý do lựa chọn:

- Hỗ trợ tốt kiến trúc phân lớp, kiểm tra dữ liệu đầu vào và xử lý giao dịch.
- Hỗ trợ luồng ảo (virtual threads) của Java 21, phù hợp với các tác vụ chờ vào ra như gọi dịch vụ AI hoặc API bên ngoài.
- Có sẵn thư viện kiểm thử tích hợp và tài liệu API tự sinh theo chuẩn OpenAPI.

### 1.3.2. Spring Security và JSON Web Token

Spring Security [8] cung cấp chuỗi bộ lọc xác thực và phân quyền cho ứng dụng web. JSON Web Token (JWT) là chuẩn mã thông báo có chữ ký [5], cho phép xác thực không trạng thái giữa giao diện và máy chủ. Mật khẩu được băm bằng thuật toán BCrypt. Hệ thống bổ sung số phiên bản mã thông báo cho mỗi tài khoản để vô hiệu hóa các phiên cũ khi người dùng đổi mật khẩu hoặc bị khóa.

### 1.3.3. Spring Data JPA và PostgreSQL

PostgreSQL là hệ quản trị cơ sở dữ liệu quan hệ mã nguồn mở [10], hỗ trợ khóa ngoại, ràng buộc kiểm tra, giao dịch và kiểu dữ liệu JSONB. Spring Data JPA (với Hibernate) ánh xạ các bảng thành thực thể Java và cung cấp tầng truy cập dữ liệu. Lý do lựa chọn:

- Bảo đảm toàn vẹn dữ liệu cho các quan hệ phức tạp giữa chuyến đi, ngày, hoạt động, chi tiêu.
- Kiểu JSONB thuận tiện lưu các cấu trúc linh hoạt như hồ sơ đoàn đi, danh sách thay đổi trong đề xuất của AI.

### 1.3.4. Spring AI

Spring AI [9] là thư viện tích hợp mô hình ngôn ngữ vào ứng dụng Spring, hỗ trợ khai báo công cụ để mô hình gọi. Hệ thống kết nối tới dịch vụ LLM qua giao diện tương thích chuẩn OpenAI nên có thể thay đổi nhà cung cấp mô hình bằng cấu hình mà không cần sửa mã nguồn.

### 1.3.5. Redis và Redisson

Redis là kho dữ liệu khóa – giá trị trong bộ nhớ [11], tốc độ truy xuất cao. Trong hệ thống, Redis được dùng để giới hạn tần suất truy cập, đếm số lần đăng nhập sai, lưu đệm dữ liệu thời tiết và kết quả tìm kiếm địa điểm. Thư viện Redisson cung cấp hàng đợi, khóa phân tán và kênh phát – nhận để triển khai tác vụ sinh lịch trình chạy nền.

## 1.4. Công nghệ phía máy khách

### 1.4.1. React, TypeScript và Vite

React là thư viện xây dựng giao diện theo mô hình thành phần (component) [12], dễ tái sử dụng và mở rộng. TypeScript bổ sung kiểu tĩnh cho JavaScript, giúp phát hiện lỗi sớm. Vite là công cụ dựng dự án với máy chủ phát triển khởi động nhanh. Hệ thống sử dụng React 19, TypeScript 5 và Vite 6.

### 1.4.2. TanStack Router và TanStack Query

TanStack Router và TanStack Query [13] là bộ thư viện quản lý định tuyến và dữ liệu. TanStack Router cung cấp định tuyến theo cấu trúc tệp với kiểu dữ liệu an toàn. TanStack Query quản lý dữ liệu lấy từ máy chủ: lưu đệm, tự động tải lại, theo dõi trạng thái đang tải, lỗi và thành công, giúp giao diện luôn đồng bộ với dữ liệu phía máy chủ.

### 1.4.3. Tailwind CSS, Radix UI và Leaflet

Tailwind CSS là framework CSS theo hướng lớp tiện ích, cho phép xây dựng giao diện đáp ứng nhiều kích thước màn hình và hỗ trợ chế độ sáng/tối. Radix UI cung cấp các thành phần nền như hộp thoại, thẻ chuyển trang tuân thủ tiêu chuẩn truy cập. Leaflet là thư viện bản đồ mã nguồn mở dùng để hiển thị vị trí các hoạt động và địa điểm trên bản đồ.

## 1.5. Dịch vụ bên thứ ba

- Google Places API [14]: tìm kiếm địa điểm theo văn bản, lấy thông tin chi tiết (tọa độ, đánh giá, mức giá, địa chỉ) làm nguồn địa điểm có thật cho người dùng và AI. Google Time Zone API được dùng để xác định múi giờ của điểm đến mới.
- Open-Meteo [15]: cung cấp dự báo thời tiết, dữ liệu thời tiết lịch sử và dịch vụ mã hóa địa lý, không yêu cầu khóa truy cập.
- Dịch vụ LLM tương thích chuẩn OpenAI: cung cấp năng lực hiểu ngôn ngữ và gọi công cụ cho trợ lý AI.

## 1.6. Công cụ triển khai và kiểm thử

- Docker và Docker Compose: đóng gói và khởi chạy đồng bộ các dịch vụ cơ sở dữ liệu, bộ nhớ đệm và máy chủ ứng dụng.
- GitHub Actions: tự động biên dịch, kiểm tra kiểu, kiểm tra quy tắc mã và chạy kiểm thử mỗi khi có thay đổi mã nguồn.
- JUnit 5, Testcontainers [16] và Vitest: viết kiểm thử đơn vị và kiểm thử tích hợp; Testcontainers khởi tạo PostgreSQL và Redis thật trong vùng chứa để kiểm thử gần với môi trường vận hành.
