# MỞ ĐẦU

## 1. Lý do chọn đề tài

Du lịch đang trở thành nhu cầu phổ biến trong đời sống, đặc biệt là các chuyến đi tự túc của cá nhân, gia đình hoặc nhóm bạn. Tuy nhiên, việc lập kế hoạch cho một chuyến đi vẫn tiêu tốn nhiều thời gian: người dùng phải tự tìm kiếm điểm tham quan, quán ăn, nơi lưu trú trên nhiều nguồn khác nhau, tự sắp xếp thứ tự di chuyển trong ngày, ước tính chi phí, theo dõi thời tiết và chuẩn bị đồ dùng cần mang theo. Thông tin thường nằm rải rác ở các ứng dụng bản đồ, trang đánh giá, mạng xã hội và bảng tính cá nhân nên khó tổng hợp và dễ bỏ sót.

Sự phát triển của các mô hình ngôn ngữ lớn mở ra khả năng hỗ trợ lập kế hoạch bằng ngôn ngữ tự nhiên. Dù vậy, nếu chỉ sử dụng một chatbot thông thường, hệ thống dễ đưa ra địa điểm không có thật, thông tin lỗi thời hoặc tự ý thay đổi kế hoạch mà người dùng không kiểm soát được. Vì vậy cần một cách tiếp cận trong đó trí tuệ nhân tạo đóng vai trò tác tử (AI Agent): được phép đọc dữ liệu chuyến đi, gọi các công cụ nghiệp vụ và dịch vụ dữ liệu thực, nhưng mọi thay đổi chỉ được ghi vào kế hoạch khi người dùng chấp thuận.

Xuất phát từ nhu cầu đó, đề tài “Xây dựng nền tảng lập kế hoạch và quản lý chuyến du lịch thông minh TripMind sử dụng AI Agent” được lựa chọn nhằm xây dựng một ứng dụng web giúp người dùng tạo chuyến đi, lập lịch trình theo ngày, quản lý ngân sách và danh sách chuẩn bị, đồng thời được trợ lý AI hỗ trợ đề xuất lịch trình dựa trên địa điểm thực, dữ liệu thời tiết và kinh nghiệm do cộng đồng chia sẻ.

## 2. Mục tiêu đề tài

Mục tiêu của đề tài là xây dựng một hệ thống web lập kế hoạch và quản lý chuyến du lịch với các nhóm chức năng chính:

- Đăng ký, đăng nhập và quản lý hồ sơ cá nhân với cơ chế xác thực bằng mã thông báo, có khả năng thu hồi phiên khi đổi mật khẩu.
- Tạo chuyến đi theo biểu mẫu nhiều bước hoặc bằng một câu mô tả tự nhiên được AI phân tích để điền sẵn thông tin.
- Quản lý lịch trình theo từng ngày: thêm, xóa hoạt động, xem bản đồ, ước tính chi phí, xem phương án thay thế theo mức ngân sách.
- Sinh lịch trình tự động bằng AI từ danh sách địa điểm có thật, kèm bước kiểm chứng để loại bỏ địa điểm không xác minh được.
- Trò chuyện với trợ lý AI có khả năng gọi công cụ; mọi đề xuất thay đổi đều phải được người dùng duyệt và có thể hoàn tác.
- Theo dõi ngân sách, chi tiêu thực tế, cảnh báo vượt ngân sách và danh sách đồ dùng, việc cần làm kèm gợi ý theo thời tiết.
- Khám phá địa điểm, chia sẻ kinh nghiệm trong cộng đồng, sao chép lịch trình từ bài viết và tạo bài tổng kết chuyến đi bằng AI.
- Cung cấp khu vực quản trị để quản lý người dùng, chuyến đi, tập tri thức điểm đến và giám sát việc sử dụng AI.

## 3. Đối tượng và phạm vi

Đối tượng sử dụng hệ thống gồm người dùng có nhu cầu lập kế hoạch du lịch tự túc và quản trị viên vận hành nền tảng. Phạm vi đề tài tập trung vào ứng dụng web (giao diện tiếng Việt) chạy trên trình duyệt máy tính và thiết bị di động, dữ liệu điểm đến hướng tới người dùng Việt Nam với đơn vị tiền tệ mặc định là đồng Việt Nam. Các nghiệp vụ đặt phòng, thanh toán trực tuyến, chia sẻ chi phí giữa nhiều thành viên và ứng dụng di động gốc nằm ngoài phạm vi của đồ án.

## 4. Phương pháp thực hiện

- Nghiên cứu tổng quan về bài toán lập kế hoạch du lịch, các mô hình ngôn ngữ lớn và kỹ thuật xây dựng tác tử AI có khả năng gọi công cụ.
- Khảo sát quy trình lập kế hoạch thực tế của người dùng để xác định yêu cầu chức năng và phi chức năng.
- Phân tích và thiết kế hướng đối tượng với ngôn ngữ UML [1], [2]: biểu đồ use case, hoạt động, tuần tự, lớp và mô hình thực thể liên kết.
- Xây dựng hệ thống theo kiến trúc client – server phân lớp, giao tiếp qua RESTful API, sử dụng Spring Boot, React, PostgreSQL và Redis.
- Kiểm thử bằng các bộ kiểm thử tự động ở mức đơn vị và tích hợp, chạy trong quy trình tích hợp liên tục, kết hợp kiểm thử chức năng trên giao diện.

## 5. Bố cục báo cáo

Ngoài phần mở đầu và kết luận, báo cáo gồm năm chương:

- Chương 1: Cơ sở lý thuyết và công nghệ — trình bày tổng quan bài toán, khái niệm mô hình ngôn ngữ lớn, AI Agent và các công nghệ được sử dụng.
- Chương 2: Phân tích và đặc tả yêu cầu — mô tả bài toán, yêu cầu chức năng, phi chức năng, biểu đồ use case và đặc tả chi tiết các chức năng.
- Chương 3: Thiết kế hệ thống — kiến trúc tổng thể, biểu đồ hoạt động, tuần tự, lớp cho các chức năng chính, thiết kế cơ sở dữ liệu và giao diện.
- Chương 4: Cài đặt và triển khai — công nghệ, kiến trúc Backend, Frontend, tác tử AI, mô hình triển khai và giao diện chương trình.
- Chương 5: Kiểm thử phần mềm — mục tiêu, phạm vi, phương pháp và kết quả kiểm thử các chức năng.
