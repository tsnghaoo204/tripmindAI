# KẾT LUẬN

## 1. Kết quả đạt được

Đề tài “Xây dựng nền tảng lập kế hoạch và quản lý chuyến du lịch thông minh TripMind sử dụng AI Agent” đã xây dựng được một ứng dụng web hoàn chỉnh với kiến trúc client – server phân lớp, giao diện tiếng Việt đáp ứng nhiều kích thước màn hình. Hệ thống hỗ trợ người dùng trong toàn bộ vòng đời chuyến đi: tạo chuyến đi bằng biểu mẫu hoặc câu mô tả tự nhiên; lập lịch trình theo ngày, thêm nhanh hoạt động từ danh sách địa điểm đã lưu, ước tính chi phí và phương án thay thế; trực quan hóa lịch trình trên bản đồ Leaflet với các điểm đánh số theo thứ tự hoạt động và tuyến di chuyển trong ngày, đồng thời hiển thị kết quả tìm kiếm địa điểm trên bản đồ; theo dõi ngân sách và chi tiêu; chuẩn bị đồ dùng với gợi ý theo thời tiết; khám phá, lưu và đánh giá địa điểm; chia sẻ kinh nghiệm và sao chép lịch trình trong cộng đồng. Quản trị viên có công cụ quản lý người dùng, chuyến đi, tri thức điểm đến và giám sát việc sử dụng AI.

Điểm nổi bật của đề tài là cách tích hợp trí tuệ nhân tạo theo mô hình tác tử có kiểm soát. Trợ lý AI được trang bị các công cụ đọc dữ liệu chuyến đi, tìm địa điểm thực, tính ngân sách, xem thời tiết và tạo đề xuất; mọi thay đổi do AI đưa ra đều phải được người dùng duyệt, có thời hạn hiệu lực và có thể hoàn tác mà không làm mất chỉnh sửa thủ công. Lịch trình do AI sinh ra được kiểm chứng với nguồn địa điểm có thật, các mục không xác minh được bị loại và báo lại cho người dùng. Hệ thống cũng áp dụng nhiều biện pháp bảo mật như thu hồi phiên khi đổi mật khẩu, giới hạn tần suất, chống dò mật khẩu và tách biệt dữ liệu giữa người dùng. Toàn bộ 116 ca kiểm thử tự động phía máy chủ và 18 ca kiểm thử phía giao diện đều đạt.

Qua quá trình thực hiện, em đã củng cố kiến thức phân tích, thiết kế hướng đối tượng, thiết kế cơ sở dữ liệu quan hệ, xây dựng RESTful API, xử lý tác vụ nền và truyền dữ liệu thời gian thực, đồng thời tích lũy kinh nghiệm thực tế về tích hợp mô hình ngôn ngữ lớn một cách an toàn vào sản phẩm phần mềm.

## 2. Hạn chế

- Cơ chế phiên chỉ dùng một mã có thời hạn 24 giờ, chưa có mã làm mới và chưa có đăng xuất phía máy chủ; mã phiên được lưu trên trình duyệt.
- Chưa có xác minh email, khôi phục mật khẩu qua email và tải ảnh trực tiếp lên hệ thống (ảnh bài viết, ảnh đại diện chỉ nhận đường dẫn).
- Một số chức năng đã có ở máy chủ nhưng chưa được đưa lên giao diện: chỉnh sửa thông tin chuyến đi, cập nhật trạng thái thực hiện hoạt động, kéo thả sắp xếp hoạt động, thiết lập sở thích mặc định.
- Gợi ý địa điểm và gợi ý danh sách chuẩn bị do trợ lý AI đưa ra trong hội thoại đã được gửi về nhưng chưa được hiển thị trên giao diện; việc lưu trực tiếp địa điểm lấy từ dịch vụ tìm kiếm bên ngoài trên trang khám phá và cập nhật tức thời số bình chọn bài viết trên giao diện cần tiếp tục hoàn thiện.
- Chưa hỗ trợ chuyển đổi tiền tệ, chia sẻ chi phí giữa thành viên và cộng tác nhiều người trên một chuyến đi.
- Chức năng sao chép lịch trình từ bài viết cộng đồng hiện sao chép toàn bộ thông tin của chuyến đi gốc; cần giới hạn các trường riêng tư. Chức năng tạo bài tổng kết bằng AI chưa áp dụng hạn mức sử dụng AI như các chức năng khác.
- Frontend chưa được đóng gói vào quy trình triển khai bằng vùng chứa; chưa có kiểm thử giao diện tự động, đo độ bao phủ và kiểm thử hiệu năng.

## 3. Hướng phát triển

- Bổ sung mã làm mới, đăng xuất phía máy chủ, xác minh email và khôi phục mật khẩu.
- Hoàn thiện giao diện cho các chức năng đã có ở máy chủ, hiển thị gợi ý địa điểm và danh sách chuẩn bị trong khung trợ lý AI.
- Cho phép nhiều thành viên cùng lập kế hoạch, chia sẻ và phân chia chi phí, chuyển đổi tiền tệ.
- Hỗ trợ tải ảnh, nhật ký hành trình và theo dõi tiến độ trong chuyến đi.
- Nâng cấp truy xuất tri thức điểm đến bằng tìm kiếm ngữ nghĩa, đánh giá định lượng chất lượng lịch trình do AI sinh ra.
- Hoàn thiện quy trình triển khai liên tục, bổ sung kiểm thử đầu – cuối và kiểm thử chịu tải; phát triển ứng dụng di động.
