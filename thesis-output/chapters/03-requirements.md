# CHƯƠNG 2. PHÂN TÍCH VÀ ĐẶC TẢ YÊU CẦU HỆ THỐNG TRIPMIND

Phân tích và đặc tả yêu cầu là giai đoạn đầu tiên và quan trọng của quy trình phát triển phần mềm, nhằm xác định chính xác hệ thống cần làm gì. Các yêu cầu được thu thập từ quá trình khảo sát cách người dùng lập kế hoạch du lịch, sau đó được phân loại thành yêu cầu chức năng, yêu cầu phi chức năng và được mô hình hóa bằng biểu đồ use case cùng bảng đặc tả chi tiết.

## 2.1. Mô tả bài toán

Nền tảng TripMind phục vụ người dùng muốn tự lên kế hoạch cho chuyến du lịch. Để sử dụng hệ thống, khách truy cập cần đăng ký tài khoản bằng họ tên, email và mật khẩu, sau đó đăng nhập. Hệ thống cấp cho người dùng một mã thông báo phiên có thời hạn; khi người dùng đổi mật khẩu hoặc bị quản trị viên khóa tài khoản, các phiên cũ lập tức mất hiệu lực.

Trang chủ hiển thị danh sách chuyến đi của người dùng, chia thành nhóm “sắp tới và đang đi” và nhóm “đã hoàn thành”, mỗi chuyến đi cho biết điểm đến, thời gian, số người và tiến độ lập lịch. Người dùng tạo chuyến đi mới qua biểu mẫu nhiều bước gồm điểm xuất phát, điểm đến, ngày đi, ngày về, phương tiện, số người, trẻ em, người cao tuổi, chế độ ăn, sở thích, nhịp độ và ngân sách. Thay vì điền thủ công, người dùng có thể nhập một câu mô tả như “đi Đà Lạt 3 ngày 2 đêm cùng người yêu, ngân sách 5 triệu”; hệ thống dùng AI để bóc tách thông tin và điền sẵn vào biểu mẫu. Điểm đến có thể chọn từ danh mục có sẵn, từ kết quả tìm kiếm địa điểm hoặc tự nhập; khi tạo chuyến đi, hệ thống tự sinh một ngày lịch trình cho mỗi ngày của chuyến đi. Một chuyến đi dài tối đa 30 ngày và ngày về không được trước ngày đi.

Trong trang chi tiết chuyến đi, người dùng xem lịch trình theo từng ngày gồm các hoạt động có giờ bắt đầu, kết thúc, chi phí ước tính, khoảng cách và thời gian di chuyển giữa các hoạt động cùng bản đồ vị trí trong ngày. Người dùng có thể thêm hoạt động thủ công hoặc từ danh sách địa điểm đã lưu, được gợi ý chi phí dựa trên mức giá của địa điểm và số người, xóa hoạt động, xem các phương án thay thế theo mức tiết kiệm, cân bằng hoặc cao cấp đối với hoạt động ăn uống và lưu trú.

Khi lịch trình còn trống, người dùng có thể yêu cầu AI sinh lịch trình tự động. Tác vụ này chạy nền: hệ thống thu thập danh sách địa điểm thực theo điểm đến và sở thích, lấy thông tin thời tiết từng ngày cùng tri thức điểm đến, yêu cầu mô hình ngôn ngữ sắp xếp lịch trình, sau đó kiểm chứng từng hoạt động và loại bỏ các mục không xác minh được. Người dùng theo dõi tiến độ theo thời gian thực và có thể hoàn tác kết quả trong vòng 10 phút.

Người dùng có thể trò chuyện với trợ lý AI về chuyến đi. Trợ lý đọc thông tin chuyến đi, lịch trình, ngân sách, thời tiết, địa điểm đã lưu, mẹo từ cộng đồng và tìm kiếm địa điểm mới. Khi cần thay đổi lịch trình, trợ lý tạo một đề xuất gồm các thao tác thêm, xóa, cập nhật hoặc sắp xếp lại hoạt động, kèm chênh lệch chi phí và thời gian di chuyển. Đề xuất chỉ có hiệu lực trong 30 phút; người dùng chọn áp dụng hoặc từ chối, và có thể hoàn tác một đề xuất đã áp dụng trong 10 phút mà không làm mất các chỉnh sửa thủ công.

Hệ thống tổng hợp ngân sách dự kiến từ các hoạt động và chi tiêu thực tế do người dùng ghi nhận, cảnh báo khi chi tiêu vượt 90% hoặc vượt ngân sách và tính mức chi tiêu khuyến nghị mỗi ngày. Danh sách chuẩn bị gồm hai nhóm “đồ mang theo” và “việc cần làm”; hệ thống gợi ý các mục phù hợp dựa trên thời tiết, độ dài chuyến đi, điểm đến trong hay ngoài nước, thành phần đoàn và loại hoạt động.

Người dùng có thể tìm kiếm địa điểm, xem trên bản đồ, lưu địa điểm yêu thích và sau chuyến đi đánh giá “thích” hoặc “không thích” từng địa điểm đã đến; địa điểm bị đánh giá không thích sẽ không được AI đề xuất lại. Trong khu vực cộng đồng, người dùng đăng bài chia sẻ kinh nghiệm (có thể gắn với chuyến đi của mình), bình luận, bình chọn bài hữu ích và sao chép lịch trình từ bài viết sang chuyến đi mới. Sau chuyến đi, AI có thể soạn sẵn một bài tổng kết dựa trên các địa điểm đã đi, chi tiêu và đánh giá để người dùng chỉnh sửa và đăng.

Quản trị viên quản lý tài khoản người dùng (tạo, đổi vai trò, khóa, đặt lại mật khẩu, xóa), quản lý chuyến đi trên toàn hệ thống, quản lý tập tri thức điểm đến dùng để bổ sung ngữ cảnh cho AI, trích xuất tri thức từ bài viết cộng đồng và theo dõi thống kê sử dụng AI cũng như nhật ký gọi công cụ.

## 2.2. Yêu cầu của hệ thống TripMind

### 2.2.1. Yêu cầu chức năng

- **Về phía khách:**
  - Đăng ký tài khoản: tạo tài khoản mới với họ tên, email và mật khẩu.
  - Đăng nhập: đăng nhập để trở thành người dùng chính thức và sử dụng các chức năng của hệ thống.
- **Về phía người dùng:**
  - Quản lý tài khoản: cập nhật họ tên, ảnh đại diện, đổi mật khẩu, đăng xuất.
  - Quản lý chuyến đi: tạo (thủ công hoặc qua phân tích mô tả bằng AI), xem danh sách, xem chi tiết, thay đổi giai đoạn, nhân bản, xóa chuyến đi và xuất lịch trình ra tệp lịch.
  - Quản lý lịch trình: xem lịch trình theo ngày và bản đồ, thêm và xóa hoạt động, nhận gợi ý chi phí, xem và chọn phương án thay thế, xem lý do AI đề xuất hoạt động.
  - Sinh lịch trình tự động bằng AI: yêu cầu sinh, theo dõi tiến độ và nhận kết quả đã kiểm chứng.
  - Trò chuyện với trợ lý AI: đặt câu hỏi, xem lịch sử hội thoại, áp dụng, từ chối hoặc hoàn tác đề xuất.
  - Quản lý ngân sách và chi tiêu: xem tổng hợp, cảnh báo, thêm, xem và xóa khoản chi.
  - Quản lý danh sách chuẩn bị: thêm, đánh dấu hoàn thành, xóa mục và nhận gợi ý.
  - Khám phá và đánh giá địa điểm: tìm kiếm, xem bản đồ, lưu địa điểm, đánh giá địa điểm sau chuyến đi.
  - Cộng đồng: xem, lọc, tìm kiếm bài viết, đăng bài, bình luận, bình chọn, sao chép lịch trình, xóa bài của mình, tạo bài tổng kết bằng AI.
- **Về phía quản trị viên:** (kế thừa toàn bộ chức năng của người dùng)
  - Quản lý người dùng: tra cứu, tạo, cập nhật vai trò, khóa/mở khóa, đặt lại mật khẩu và xóa tài khoản.
  - Quản lý chuyến đi: xem danh sách, chỉnh sửa thông tin cơ bản và xóa chuyến đi bất kỳ.
  - Quản lý tri thức điểm đến: thêm, sửa, xóa, bật/tắt mục tri thức; trích xuất tri thức từ bài viết cộng đồng.
  - Giám sát AI: xem thống kê số lượt trò chuyện, lượng token, tỷ lệ lỗi và nhật ký thực thi công cụ.

### 2.2.2. Yêu cầu phi chức năng

- *Giao diện người dùng*
  - Giao diện tiếng Việt, tương thích nhiều kích thước màn hình, có thanh điều hướng thu gọn trên thiết bị di động và hỗ trợ chế độ sáng/tối.
  - Trạng thái đang tải, lỗi và kết quả được thông báo rõ ràng; thao tác nguy hiểm như xóa cần hộp thoại xác nhận.
  - Các tác vụ AI kéo dài phải hiển thị tiến độ hoặc nội dung trả lời dần theo thời gian thực.
- *Bảo mật hệ thống*
  - Xác thực bằng mã thông báo có chữ ký, thời hạn mặc định 24 giờ; mật khẩu băm bằng BCrypt; phiên cũ bị vô hiệu khi đổi mật khẩu hoặc khóa tài khoản.
  - Phân quyền theo vai trò người dùng và quản trị viên; người dùng chỉ truy cập được dữ liệu của mình, yêu cầu tới dữ liệu của người khác được phản hồi như không tồn tại để tránh lộ thông tin.
  - Giới hạn tần suất: tối đa 120 yêu cầu mỗi phút cho mỗi người dùng hoặc địa chỉ IP, 20 lượt sử dụng AI mỗi giờ cho mỗi người dùng; tạm khóa đăng nhập sau 5 lần sai mật khẩu trong 5 phút.
  - Giới hạn kích thước nội dung yêu cầu, chỉ chấp nhận các nguồn giao diện được cấu hình, không trả về thông tin riêng tư như email trong dữ liệu công khai.
- *Độ tin cậy và an toàn dữ liệu của AI*
  - AI không được tự ghi dữ liệu; mọi thay đổi phải qua đề xuất được người dùng duyệt và có thể hoàn tác.
  - Địa điểm do AI sử dụng phải được kiểm chứng là có thật.
  - Khi dịch vụ AI chưa được cấu hình hoặc gặp lỗi, các chức năng còn lại vẫn hoạt động; chức năng phân tích mô tả và tạo bài tổng kết có phương án dự phòng không dùng AI.
- *Hiệu năng và khả năng mở rộng*
  - Tác vụ sinh lịch trình chạy nền qua hàng đợi, có cơ chế khôi phục tác vụ bị gián đoạn.
  - Dữ liệu thời tiết và kết quả tìm kiếm địa điểm được lưu đệm để giảm số lần gọi dịch vụ ngoài.
  - Máy chủ ứng dụng không lưu trạng thái phiên, thuận lợi cho việc mở rộng theo chiều ngang.

## 2.3. Mô hình hóa và mô tả các chức năng

### 2.3.1. Mô hình hóa

#### 2.3.1.1. Mô hình use case tổng quan

Hệ thống có ba tác nhân chính là Khách, Người dùng và Quản trị viên; trong đó Quản trị viên kế thừa toàn bộ chức năng của Người dùng, và Người dùng kế thừa chức năng của Khách. Các dịch vụ bên ngoài gồm Dịch vụ LLM, Google Places và Open-Meteo tham gia với vai trò tác nhân phụ hỗ trợ dữ liệu và xử lý thông minh. Toàn bộ hệ thống được tổ chức thành 6 nhóm use case chính: Xác thực và quản lý tài khoản, Quản lý chuyến đi và lịch trình, Trợ lý AI lập và điều chỉnh lịch trình, Quản lý ngân sách và chuẩn bị hành lý, Khám phá địa điểm và cộng đồng chia sẻ, cùng Quản trị hệ thống.

@fig{DGM-UC-001-tong-quan}{Biểu đồ use case tổng quan}

#### 2.3.1.2. Mô hình use case phía người dùng

@fig{DGM-UC-002-nguoi-dung}{Biểu đồ use case tổng quan phía người dùng}

- Xác thực và quản lý tài khoản: đăng ký tài khoản, đăng nhập, đăng xuất và quản lý thông tin hồ sơ cá nhân.
- Quản lý chuyến đi và lịch trình: tạo chuyến đi (nhập liệu hoặc phân tích câu mô tả), quản lý danh sách chuyến đi, theo dõi và tùy chỉnh lịch trình chi tiết theo từng ngày trên bản đồ, xuất lịch trình ra tệp lịch.
- Trợ lý AI lập và điều chỉnh lịch trình: yêu cầu AI tự động sinh lịch trình hoàn chỉnh từ địa điểm có thật, hỏi đáp với trợ lý AI, nhận và xử lý các đề xuất thay đổi lịch trình có thể hoàn tác.
- Quản lý ngân sách và chuẩn bị hành lý: theo dõi dự toán và chi tiêu thực tế, quản lý danh sách đồ mang theo và việc cần làm, nhận gợi ý chuẩn bị phù hợp theo thời tiết và chuyến đi.
- Khám phá địa điểm và cộng đồng chia sẻ: tìm kiếm địa điểm, lưu địa điểm yêu thích, đánh giá địa điểm đã đến, chia sẻ bài viết kinh nghiệm và sao chép lịch trình từ cộng đồng.

#### 2.3.1.3. Mô hình use case phía quản trị

@fig{DGM-UC-003-quan-tri}{Biểu đồ use case tổng quan phía quản trị viên}

- Quản lý người dùng: tra cứu danh sách, tạo tài khoản mới, cập nhật vai trò, khóa hoặc mở khóa, đặt lại mật khẩu và xóa tài khoản.
- Quản lý chuyến đi toàn hệ thống: tra cứu, xem chi tiết, chỉnh sửa thông tin cơ bản và xóa chuyến đi bất kỳ.
- Quản lý tri thức điểm đến: thêm, sửa, xóa, bật/tắt các mục tri thức và trích xuất tri thức du lịch từ bài viết cộng đồng bằng AI.
- Giám sát AI: xem thống kê số lượt tương tác, lượng token tiêu thụ, tỷ lệ lỗi và kiểm tra nhật ký thực thi công cụ của trợ lý AI.

### 2.3.2. Mô tả các chức năng

#### 2.3.2.1. Mô tả chức năng "Khám phá địa điểm & Chia sẻ kinh nghiệm"

@usecase[UC01]{Mô tả chức năng "Khám phá địa điểm & Chia sẻ kinh nghiệm"}
name: Khám phá địa điểm & Chia sẻ kinh nghiệm
code: UC_01
actors: Khách vãng lai, Người dùng, Quản trị viên; tác nhân phụ: Google Places
desc: Chức năng cho phép tác nhân tìm kiếm và khám phá các địa điểm du lịch, lưu địa điểm yêu thích, đánh giá địa điểm đã đến; xem, lọc bài viết cộng đồng, đăng bài chia sẻ kinh nghiệm (có hỗ trợ AI tạo bản nháp), tương tác bình chọn/bình luận và sao chép lịch trình trực tiếp từ bài viết.
trigger: Tác nhân mở trang “Khám phá địa điểm”, trang “Cộng đồng & Review” hoặc chọn “Đánh giá” trên chuyến đi đã kết thúc.
pre: Xem và tìm kiếm: không yêu cầu đăng nhập. Lưu địa điểm, đánh giá, đăng bài và tương tác: yêu cầu Người dùng đã đăng nhập.
main:
== Khám phá và đánh giá địa điểm
1 | Tác nhân | Nhập từ khóa, chọn điểm đến và nhấn “Tìm kiếm”.
2 | Hệ thống | Tìm kiếm qua dịch vụ địa điểm, lưu đệm kết quả, hiển thị danh sách địa điểm và đánh dấu trực quan trên bản đồ.
3 | Người dùng | Chọn “Lưu” địa điểm vào danh sách yêu thích hoặc mở thẻ “Đã lưu”.
4 | Hệ thống | Lưu địa điểm vào danh mục yêu thích của người dùng; hiển thị danh sách đã lưu.
5 | Người dùng | Với chuyến đi đã kết thúc, chọn đánh giá “Thích” hoặc “Không thích” cho từng địa điểm đã trải nghiệm.
6 | Hệ thống | Lưu đánh giá; các địa điểm “không thích” được tự động loại khỏi gợi ý lập lịch về sau.
== Xem và tương tác cộng đồng
7 | Tác nhân | Chọn điểm đến hoặc nhập từ khóa để lọc các bài viết chia sẻ, cuộn trang để xem thêm.
8 | Hệ thống | Hiển thị danh sách bài viết mới nhất kèm lượt bình chọn hữu ích, bình luận và tác giả.
9 | Người dùng | Nhấn bình chọn hữu ích hoặc gửi nội dung bình luận vào bài viết.
10 | Hệ thống | Bật/tắt trạng thái bình chọn, cập nhật số đếm thời gian thực và lưu nội dung bình luận.
== Đăng bài và sao chép lịch trình
11 | Người dùng | Chọn “Viết bài”, nhập tiêu đề, điểm đến, nội dung, ảnh đính kèm và chọn liên kết chuyến đi (tùy chọn); hoặc chọn “AI viết review” từ chuyến đi đã hoàn thành.
12 | Hệ thống | Với hỗ trợ AI: tổng hợp danh sách địa điểm đã đi, chi phí và đánh giá, tự động sinh bản nháp bài tổng kết; hoặc kiểm tra dữ liệu bài viết nhập tay và lưu bài viết.
13 | Người dùng | Chọn “Sao chép lịch trình” trên bài viết có đính kèm chuyến đi, chọn ngày khởi hành mới.
14 | Hệ thống | Khởi tạo chuyến đi mới trong tài khoản người dùng với toàn bộ hoạt động lịch trình được dời theo ngày đã chọn.
alt:
2a | Hệ thống | Dịch vụ địa điểm bên ngoài gặp sự cố hoặc không có kết quả: chuyển sang tìm kiếm trong danh mục địa điểm nội bộ.
5a | Hệ thống | Chuyến đi chưa kết thúc: từ chối thực hiện đánh giá.
12a | Hệ thống | Dịch vụ AI lỗi khi sinh bản nháp bài viết: tạo bản nháp thay thế theo mẫu cấu trúc từ dữ liệu chuyến đi.
12b | Hệ thống | Ảnh đính kèm không phải định dạng https hợp lệ hoặc vượt độ dài cho phép: thông báo lỗi và yêu cầu chỉnh sửa.
post: Địa điểm được tìm kiếm và lưu; đánh giá được ghi nhận; bài viết/bình luận được cập nhật; chuyến đi mới được sao chép thành công.
@end

@fig{DGM-UC-008-dia-diem-chia-se}{Biểu đồ use case phân rã chức năng "Khám phá địa điểm & Chia sẻ kinh nghiệm"}

#### 2.3.2.2. Mô tả chức năng "Xác thực và quản lý tài khoản"

@usecase[UC02]{Mô tả chức năng "Xác thực và quản lý tài khoản"}
name: Xác thực và quản lý tài khoản
code: UC_02
actors: Khách vãng lai, Người dùng, Quản trị viên
desc: Chức năng cho phép tác nhân đăng ký tài khoản mới, đăng nhập, đăng xuất khỏi hệ thống, cập nhật thông tin hồ sơ cá nhân, đổi mật khẩu và thực hiện quy trình khôi phục mật khẩu qua email.
trigger: Tác nhân chọn “Đăng ký”, “Đăng nhập”, “Đăng xuất”, mở trang hồ sơ cá nhân hoặc chọn “Quên mật khẩu”.
pre: Đăng ký: email chưa tồn tại trên hệ thống. Đăng nhập: tài khoản đã tồn tại và đang ở trạng thái hoạt động. Cập nhật hồ sơ, đổi mật khẩu: tác nhân đã đăng nhập.
main:
== Đăng ký tài khoản
1 | Khách vãng lai | Chọn “Đăng ký”, nhập họ tên, email, mật khẩu và xác nhận.
2 | Hệ thống | Kiểm tra định dạng dữ liệu, chuẩn hóa email, băm mật khẩu bảo mật (BCrypt), tạo tài khoản vai trò Người dùng; hiển thị thông báo thành công và chuyển sang trang đăng nhập.
== Đăng nhập và đăng xuất
3 | Người dùng | Nhập email, mật khẩu và nhấn “Đăng nhập”.
4 | Hệ thống | Kiểm tra số lần đăng nhập sai, đối chiếu thông tin xác thực, kiểm tra trạng thái khóa tài khoản; cấp mã thông báo phiên (JWT token) và chuyển hướng tới trang làm việc.
5 | Người dùng | Chọn “Đăng xuất”.
6 | Hệ thống | Vô hiệu hóa phiên làm việc trên trình duyệt và điều hướng về màn hình đăng nhập.
== Cập nhật hồ sơ và đổi mật khẩu
7 | Người dùng | Mở trang hồ sơ, cập nhật họ tên, ảnh đại diện hoặc nhập mật khẩu hiện tại cùng mật khẩu mới.
8 | Hệ thống | Kiểm tra tính hợp lệ của dữ liệu, lưu thay đổi vào cơ sở dữ liệu; khi đổi mật khẩu thành công thì hủy các phiên đăng nhập cũ và cấp phiên mới.
== Khôi phục mật khẩu
9 | Tác nhân | Chọn “Quên mật khẩu”, nhập địa chỉ email đã đăng ký và yêu cầu cấp lại.
10 | Hệ thống | Xác thực email tồn tại, tạo mã xác thực an toàn gửi tới email người dùng; sau khi người dùng xác nhận mã hợp lệ, cho phép đặt lại mật khẩu mới.
alt:
2a | Hệ thống | Email đã tồn tại trên hệ thống: thông báo lỗi trùng lặp và yêu cầu sử dụng email khác.
2b | Hệ thống | Mật khẩu không đủ độ dài an toàn (dưới 8 ký tự): thông báo lỗi quy chuẩn.
4a | Hệ thống | Nhập sai email hoặc mật khẩu: ghi nhận thất bại và hiển thị thông báo sai thông tin.
4b | Hệ thống | Tài khoản đang bị quản trị viên khóa: từ chối quyền truy cập và thông báo lý do.
4c | Hệ thống | Đăng nhập sai quá 5 lần liên tiếp trong 5 phút: tạm khóa đăng nhập và thông báo thời gian chờ.
post: Tài khoản được khởi tạo; phiên đăng nhập được cấp hoặc hủy; hồ sơ và mật khẩu được cập nhật an toàn.
@end

@fig{DGM-UC-004-xac-thuc}{Biểu đồ use case phân rã chức năng "Xác thực và quản lý tài khoản"}

#### 2.3.2.3. Mô tả chức năng "Quản lý chuyến đi và lịch trình"

@usecase[UC03]{Mô tả chức năng "Quản lý chuyến đi và lịch trình"}
name: Quản lý chuyến đi và lịch trình
code: UC_03
actors: Người dùng
desc: Chức năng cho phép người dùng tạo chuyến đi mới (thủ công hoặc phân tích câu mô tả bằng AI), quản lý danh sách chuyến đi theo từng giai đoạn, theo dõi và tùy biến chi tiết hoạt động lịch trình theo từng ngày trên bản đồ số, xem phương án địa điểm thay thế, nhân bản, xóa chuyến đi và xuất lịch trình ra định dạng lịch iCalendar (.ics).
trigger: Người dùng nhấn “Tạo chuyến đi mới”, nhập câu lệnh tại ô “Lập kế hoạch bằng AI” hoặc thao tác trực tiếp trên trang chi tiết chuyến đi.
pre: Người dùng đã đăng nhập thành công vào hệ thống.
main:
== Tạo chuyến đi mới
1 | Người dùng | Nhập câu mô tả chuyến đi (tùy chọn) và nhấn “Phân tích & Tạo”, hoặc chọn tạo thủ công.
2 | Hệ thống | Phân tích tự động nội dung mô tả (nếu có) và điền sẵn thông tin vào biểu mẫu; hỗ trợ người dùng chọn điểm đến, ngày đi/về, số lượng thành viên đoàn, phong cách và dự toán ngân sách.
3 | Người dùng | Kiểm tra thông tin và nhấn “Xác nhận tạo”.
4 | Hệ thống | Kiểm tra tính hợp lệ của thời gian, lưu chuyến đi và tự động khởi tạo khung lịch trình theo từng ngày tương ứng.
== Quản lý danh sách và theo dõi lịch trình
5 | Người dùng | Chọn nhóm chuyến đi “Sắp tới & Đang đi” hoặc “Đã hoàn thành”, mở xem một chuyến đi cụ thể.
6 | Hệ thống | Hiển thị danh sách hoạt động theo thứ tự thời gian trong ngày, tính toán khoảng cách và thời gian di chuyển ước tính giữa các điểm, tổng hợp chi phí và đánh dấu trực quan trên bản đồ.
== Tùy chỉnh hoạt động và phương án thay thế
7 | Người dùng | Thêm mới một hoạt động (tiêu đề, thời gian, loại hình, chi phí ước tính) hoặc chọn xóa một hoạt động hiện có.
8 | Hệ thống | Cập nhật hoạt động vào ngày tương ứng, sắp xếp lại trình tự thời gian và tự động tính lại tổng chi phí ngày.
9 | Người dùng | Chọn xem “Phương án thay thế” cho một hoạt động ăn uống/lưu trú, hoặc nhấn “Vì sao?” để xem lý do lựa chọn.
10 | Hệ thống | Đề xuất tối đa 3 phương án địa điểm thay thế theo các tiêu chí (tiết kiệm, cân bằng, cao cấp) từ nguồn dữ liệu thực tế và cập nhật hoạt động khi người dùng chọn.
== Nhân bản, đổi giai đoạn và xuất lịch
11 | Người dùng | Chọn “Nhân bản” (chọn ngày bắt đầu mới), chuyển đổi trạng thái chuyến đi, hoặc chọn “Xuất lịch”.
12 | Hệ thống | Tạo bản sao độc lập dời toàn bộ lịch trình và dữ liệu liên quan sang mốc thời gian mới; hoặc tạo và tải về tệp tin lịch .ics để đồng bộ vào Google Calendar/Apple Calendar.
alt:
2a | Hệ thống | Dịch vụ AI phân tích câu mô tả gặp sự cố: chuyển hướng người dùng nhập liệu từng bước qua biểu mẫu chuẩn.
4a | Hệ thống | Ngày kết thúc trước ngày khởi hành hoặc chuyến đi vượt quá giới hạn 30 ngày: thông báo lỗi ngày tháng.
4b | Hệ thống | Dữ liệu cấu trúc đoàn không hợp lệ (số trẻ em/người cao tuổi vượt tổng người): thông báo lỗi nhập liệu.
post: Chuyến đi và khung lịch trình được khởi tạo, cập nhật, nhân bản hoặc xuất tệp tin lịch thành công.
@end

@fig{DGM-UC-005-chuyen-di-lich-trinh}{Biểu đồ use case phân rã chức năng "Quản lý chuyến đi và lịch trình"}

#### 2.3.2.4. Mô tả chức năng "Trợ lý AI lập và điều chỉnh lịch trình"

@usecase[UC04]{Mô tả chức năng "Trợ lý AI lập và điều chỉnh lịch trình"}
name: Trợ lý AI lập và điều chỉnh lịch trình
code: UC_04
actors: Người dùng; tác nhân phụ: Dịch vụ LLM, Google Places, Open-Meteo
desc: Chức năng cho phép tự động sinh lịch trình hoàn chỉnh từ các địa điểm có thật thông qua mô hình ngôn ngữ lớn kết hợp xác thực thời tiết và tri thức du lịch; hỗ trợ hỏi đáp trực tiếp với trợ lý AI trong ngữ cảnh chuyến đi, tự động gọi công cụ nghiệp vụ và tạo đề xuất thay đổi lịch trình có thể hoàn tác an toàn.
trigger: Người dùng nhấn “Tạo lịch trình AI” hoặc mở khung hội thoại “Trợ lý AI” trong trang chi tiết chuyến đi.
pre: Người dùng là chủ sở hữu chuyến đi; dịch vụ AI đã được cấu hình và sẵn sàng hoạt động.
main:
== Sinh lịch trình tự động bằng AI
1 | Người dùng | Nhấn “Tạo lịch trình AI” trên trang chi tiết chuyến đi.
2 | Hệ thống | Khởi tạo tác vụ chạy ngầm, cấp mã tác vụ và đưa vào hàng đợi xử lý.
3 | Hệ thống | Thu thập địa điểm thực tế theo điểm đến, khẩu vị ăn uống, sở thích du lịch; đồng thời loại bỏ các địa điểm mà người dùng đã từng đánh giá không thích.
4 | Hệ thống | Truy vấn dự báo thời tiết cho từng ngày của chuyến đi và tích hợp dữ liệu tri thức điểm đến, sau đó gửi prompt có cấu trúc tới mô hình LLM.
5 | Hệ thống | Kiểm chứng nghiêm ngặt từng hoạt động do AI sinh ra: bắt buộc phải khớp địa điểm thực tế, không bị trùng lặp tọa độ và thời gian; loại bỏ các hoạt động ảo kèm lý do chi tiết.
6 | Hệ thống | Lưu đề xuất vào cơ sở dữ liệu, áp dụng vào lịch trình chuyến đi và phát tiến độ xử lý theo thời gian thực (SSE) lên giao diện người dùng.
== Trò chuyện và điều chỉnh lịch trình qua AI
7 | Người dùng | Nhập câu hỏi hoặc yêu cầu điều chỉnh lịch trình vào khung chat trợ lý AI.
8 | Hệ thống | Dựng ngữ cảnh hội thoại gồm thông tin chuyến đi, thời tiết và lịch sử trao đổi; gửi yêu cầu tới mô hình AI và thực thi các công cụ nghiệp vụ (function calling) được yêu cầu.
9 | Hệ thống | Truyền phản hồi dạng dòng (streaming) về giao diện; nếu có sự thay đổi lịch trình, hiển thị thẻ đề xuất trực quan kèm mức chênh lệch chi phí.
10 | Người dùng | Xem xét thẻ đề xuất và nhấn “Áp dụng” hoặc “Từ chối”.
11 | Hệ thống | Áp dụng thay đổi vào các ngày lịch trình tương ứng, ghi nhận điểm lưu vết hoàn tác; hoặc hủy bỏ đề xuất.
12 | Người dùng | Nhấn “Hoàn tác” trong khoảng thời gian cho phép nếu không ưng ý với kết quả vừa áp dụng.
13 | Hệ thống | Khôi phục lại trạng thái lịch trình trước đó, bảo toàn nguyên vẹn các hoạt động người dùng đã tự chỉnh sửa thủ công.
alt:
2a | Hệ thống | Lịch trình hiện tại đã có hoạt động: yêu cầu người dùng xác nhận xóa lịch cũ trước khi tạo mới bằng AI.
5a | Hệ thống | Mô hình AI trả về dữ liệu sai định dạng hoặc không có địa điểm hợp lệ: đánh dấu tác vụ thất bại và thông báo người dùng thử lại.
9a | Hệ thống | Quá trình gọi công cụ AI vượt giới hạn thời gian (timeout): thông báo lỗi và yêu cầu người dùng tinh chỉnh lại câu hỏi ngắn gọn hơn.
12a | Hệ thống | Quá thời hạn hoàn tác (10 phút) hoặc đã có thay đổi mới ghi đè: từ chối yêu cầu hoàn tác.
post: Lịch trình chuyến đi được tự động tạo lập từ địa điểm thực; các đề xuất tinh chỉnh qua AI được áp dụng hoặc hoàn tác an toàn.
@end

@fig{DGM-UC-006-tro-ly-ai}{Biểu đồ use case phân rã chức năng "Trợ lý AI lập và điều chỉnh lịch trình"}

#### 2.3.2.5. Mô tả chức năng "Quản lý ngân sách và chuẩn bị hành lý"

@usecase[UC05]{Mô tả chức năng "Quản lý ngân sách và chuẩn bị hành lý"}
name: Quản lý ngân sách và chuẩn bị hành lý
code: UC_05
actors: Người dùng; tác nhân phụ: Open-Meteo
desc: Chức năng quản lý tài chính chuyến đi (dự toán, chi phí ước tính từ lịch trình, ghi nhận các khoản chi tiêu thực tế, tính hạn mức chi khuyến nghị mỗi ngày và cảnh báo nguy cơ vượt ngân sách) cùng quản lý danh sách chuẩn bị (checklist đồ dùng mang theo, danh mục việc cần làm, tự động gợi ý thông minh dựa theo điều kiện thời tiết thực tế).
trigger: Người dùng mở thẻ “Ngân sách & Chi tiêu” hoặc thẻ “Checklist chuẩn bị” trên trang chi tiết chuyến đi.
pre: Người dùng là chủ sở hữu chuyến đi.
main:
== Quản lý ngân sách và chi tiêu
1 | Người dùng | Mở thẻ “Ngân sách & Chi tiêu”.
2 | Hệ thống | Hiển thị bảng tổng hợp gồm tổng ngân sách thiết lập, chi phí ước tính từ lịch trình, số tiền đã chi, số dư còn lại, biểu đồ phân bổ danh mục và hạn mức chi tiêu khuyến nghị mỗi ngày.
3 | Người dùng | Nhấn “Thêm khoản chi”, nhập số tiền, chọn danh mục chi (ăn uống, đi lại, lưu trú, vé tham quan, mua sắm...), ngày chi và ghi chú.
4 | Hệ thống | Kiểm tra tính hợp lệ của số tiền và loại tiền tệ, lưu khoản chi vào cơ sở dữ liệu và tự động cập nhật lại các chỉ số tổng hợp tài chính.
5 | Người dùng | Xem danh sách lịch sử chi tiêu, thực hiện chỉnh sửa hoặc xóa một khoản chi đã ghi nhận.
6 | Hệ thống | Xóa khoản chi và tính toán lại tức thì số dư ngân sách khả dụng.
== Quản lý danh sách chuẩn bị và gợi ý thông minh
7 | Người dùng | Mở thẻ “Checklist chuẩn bị”, nhập tên mục đồ dùng hoặc đầu việc cần làm, chọn nhóm phân loại và nhấn “Thêm”.
8 | Hệ thống | Lưu mục chuẩn bị vào danh sách; tự động phát hiện và bỏ qua các mục bị trùng tên trong cùng một nhóm.
9 | Người dùng | Đánh dấu hoàn thành (tick chọn) khi đã chuẩn bị xong hoặc nhấn xóa mục không cần thiết.
10 | Người dùng | Nhấn nút “Gợi ý thông minh”.
11 | Hệ thống | Tự động phân tích các yếu tố: điều kiện thời tiết dự báo (mưa, nắng nóng, lạnh), độ dài chuyến đi, phương tiện di chuyển, có trẻ em hoặc người cao tuổi đi cùng; sinh danh sách gợi ý kèm lý do trực quan.
12 | Người dùng | Đánh dấu chọn các mục gợi ý mong muốn và nhấn “Thêm vào danh sách”.
13 | Hệ thống | Bổ sung các mục đã chọn vào danh sách chuẩn bị chính thức với nguồn gốc ghi nhận là “Gợi ý AI”.
alt:
4a | Hệ thống | Số tiền nhập vào mang giá trị âm hoặc khác đơn vị tiền tệ của chuyến đi: từ chối lưu và thông báo lỗi.
2a | Hệ thống | Chi phí thực tế vượt ngưỡng 80% hoặc 100% ngân sách: hiển thị cảnh báo màu vàng/đỏ nổi bật trên giao diện.
11a | Hệ thống | Dịch vụ thời tiết không phản hồi: bỏ qua nhóm quy tắc thời tiết và áp dụng các quy tắc gợi ý cơ bản theo thời lượng chuyến đi.
post: Các khoản chi tiêu được ghi nhận và tính toán chính xác; danh sách chuẩn bị đồ dùng và công việc được cập nhật đầy đủ.
@end

@fig{DGM-UC-007-ngan-sach-hanh-ly}{Biểu đồ use case phân rã chức năng "Quản lý ngân sách và chuẩn bị hành lý"}

#### 2.3.2.6. Mô tả chức năng "Quản trị hệ thống"

@usecase[UC06]{Mô tả chức năng "Quản trị hệ thống"}
name: Quản trị hệ thống
code: UC_06
actors: Quản trị viên
desc: Chức năng dành riêng cho quản trị viên nhằm quản lý người dùng toàn hệ thống, quản lý và kiểm duyệt chuyến đi, quản lý cơ sở tri thức điểm đến du lịch (thêm, sửa, xóa, trích xuất tự động từ bài viết cộng đồng bằng AI), đồng thời giám sát các chỉ số vận hành và nhật ký thực thi của mô hình AI.
trigger: Quản trị viên truy cập vào khu vực “Quản trị hệ thống” (Admin Dashboard).
pre: Tài khoản người dùng đã đăng nhập và được cấp quyền vai trò Quản trị viên (ADMIN).
main:
== Quản lý người dùng và phân quyền
1 | Quản trị viên | Tìm kiếm theo họ tên/email, tạo tài khoản mới, cập nhật vai trò (USER/ADMIN), khóa hoặc mở khóa tài khoản, đặt lại mật khẩu hoặc xóa tài khoản.
2 | Hệ thống | Kiểm tra tính hợp lệ và thẩm quyền, cập nhật cơ sở dữ liệu; khi tài khoản bị khóa hoặc đặt lại mật khẩu, tự động hủy bỏ toàn bộ phiên đăng nhập đang hoạt động của người dùng đó.
== Quản lý chuyến đi toàn hệ thống
3 | Quản trị viên | Tra cứu danh sách chuyến đi của tất cả người dùng, xem thông tin chi tiết, chỉnh sửa thông tin hành chính hoặc xóa các chuyến đi vi phạm.
4 | Hệ thống | Lưu lại các thay đổi hoặc xóa chuyến đi cùng toàn bộ dữ liệu lịch trình, ngân sách, checklist liên quan.
== Quản lý tri thức điểm đến
5 | Quản trị viên | Xem danh mục tri thức, thêm mới các mẹo du lịch/thông tin điểm đến, chỉnh sửa, bật/tắt kích hoạt; hoặc chọn “Trích xuất tri thức” từ một bài viết cộng đồng hữu ích.
6 | Hệ thống | Lưu mục tri thức; với yêu cầu trích xuất, tự động sử dụng AI để tóm tắt các thông tin giá trị từ bài viết thành bản nháp tri thức chuẩn hóa để quản trị viên kiểm tra và phê duyệt.
== Giám sát hoạt động và hiệu năng AI
7 | Quản trị viên | Mở khu vực “Thống kê & Giám sát AI”, lựa chọn mốc thời gian và các bộ lọc phân tích.
8 | Hệ thống | Hiển thị bảng điều khiển trực quan gồm: tổng số lượt tương tác AI, lượng token tiêu thụ, chi phí ước tính, tỷ lệ cuộc gọi lỗi, biểu đồ tần suất gọi các công cụ (tools) và nhật ký chi tiết các yêu cầu có phân trang.
alt:
2a | Hệ thống | Quản trị viên thao tác tự khóa hoặc tự xóa tài khoản của chính mình: từ chối thực hiện và thông báo cảnh báo.
1a | Hệ thống | Tài khoản thao tác không có quyền Quản trị viên: lập tức điều hướng về trang chủ và ghi nhận cảnh báo bảo mật.
6a | Hệ thống | Dịch vụ AI gặp sự cố khi trích xuất tri thức: giữ nguyên nội dung bài viết gốc làm bản nháp thô cho quản trị viên tự biên tập.
post: Dữ liệu người dùng, chuyến đi và tri thức điểm đến được cập nhật chuẩn xác; hệ thống ghi nhận đầy đủ nhật ký kiểm toán và giám sát AI.
@end

@fig{DGM-UC-009-quan-tri}{Biểu đồ use case phân rã chức năng "Quản trị hệ thống"}
