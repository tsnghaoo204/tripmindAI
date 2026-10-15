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

Hệ thống có ba tác nhân chính là Khách, Người dùng và Quản trị viên; trong đó Quản trị viên kế thừa toàn bộ chức năng của Người dùng. Các dịch vụ bên ngoài gồm Dịch vụ LLM, Google Places và Open-Meteo tham gia với vai trò tác nhân phụ.

@fig{DGM-UC-001-tong-quan}{Biểu đồ use case tổng quan}

#### 2.3.1.2. Mô hình use case phía người dùng

@fig{DGM-UC-002-nguoi-dung}{Biểu đồ use case tổng quan phía người dùng}

- Xác thực và quản lý tài khoản: đăng ký, đăng nhập, đăng xuất, cập nhật hồ sơ và đổi mật khẩu.
- Quản lý chuyến đi: tạo, xem, thay đổi giai đoạn, nhân bản, xóa chuyến đi và xuất lịch trình.
- Quản lý lịch trình: xem lịch trình và bản đồ, thêm, xóa hoạt động, xem phương án thay thế.
- Sinh lịch trình tự động bằng AI: tạo lịch trình hoàn chỉnh từ địa điểm có thật.
- Trò chuyện với trợ lý AI: hỏi đáp, nhận và xử lý đề xuất thay đổi lịch trình.
- Quản lý ngân sách và chi tiêu, quản lý danh sách chuẩn bị.
- Khám phá và đánh giá địa điểm; tham gia cộng đồng chia sẻ kinh nghiệm.

#### 2.3.1.3. Mô hình use case phía quản trị

@fig{DGM-UC-003-quan-tri}{Biểu đồ use case tổng quan phía quản trị viên}

- Quản lý người dùng: tra cứu, tạo tài khoản, đổi vai trò, khóa/mở khóa, đặt lại mật khẩu, xóa tài khoản.
- Quản lý chuyến đi trên toàn hệ thống: xem, chỉnh sửa thông tin cơ bản, xóa.
- Quản lý tri thức điểm đến và trích xuất tri thức từ bài viết cộng đồng.
- Xem thống kê sử dụng AI và nhật ký thực thi công cụ của trợ lý.

### 2.3.2. Mô tả các chức năng

#### 2.3.2.1. Mô tả chức năng "Xác thực và quản lý tài khoản"

@usecase[UC01]{Mô tả chức năng "Xác thực và quản lý tài khoản"}
name: Xác thực và quản lý tài khoản
code: UC_01
actors: Khách, Người dùng, Quản trị viên
desc: Chức năng cho phép tác nhân:
- Đăng ký tài khoản mới
- Đăng nhập, đăng xuất
- Cập nhật hồ sơ, đổi mật khẩu
trigger: Tác nhân chọn “Đăng ký”, “Đăng nhập”, “Đăng xuất” hoặc mở hộp thoại hồ sơ cá nhân.
pre: Đăng ký: email chưa tồn tại. Đăng nhập: tài khoản tồn tại và đang hoạt động. Cập nhật hồ sơ, đổi mật khẩu: đã đăng nhập.
main:
== Đăng ký
1 | Khách | Chọn “Đăng ký”.
2 | Hệ thống | Hiển thị biểu mẫu gồm họ tên, email, mật khẩu.
3 | Khách | Nhập thông tin và nhấn “Đăng ký”.
4 | Hệ thống | Kiểm tra dữ liệu, chuẩn hóa email, băm mật khẩu, tạo tài khoản vai trò Người dùng; thông báo thành công và chuyển sang trang đăng nhập.
== Đăng nhập
5 | Người dùng | Nhập email, mật khẩu và nhấn “Đăng nhập”.
6 | Hệ thống | Kiểm tra số lần đăng nhập sai, đối chiếu mật khẩu, trạng thái tài khoản; cấp mã thông báo phiên và chuyển tới trang chủ.
== Đăng xuất
7 | Người dùng | Nhấn “Đăng xuất”.
8 | Hệ thống | Xóa phiên đăng nhập trên trình duyệt và chuyển về trang đăng nhập.
== Cập nhật hồ sơ, đổi mật khẩu
9 | Người dùng | Mở hồ sơ, sửa họ tên, ảnh đại diện hoặc nhập mật khẩu hiện tại và mật khẩu mới.
10 | Hệ thống | Kiểm tra dữ liệu, lưu thay đổi; khi đổi mật khẩu thì vô hiệu hóa các phiên cũ và cấp phiên mới.
alt:
4a | Hệ thống | Email đã được sử dụng: thông báo lỗi, quay lại bước 2.
4b | Hệ thống | Dữ liệu không hợp lệ (email sai định dạng, mật khẩu dưới 8 ký tự): thông báo lỗi.
6a | Hệ thống | Sai email hoặc mật khẩu: thông báo lỗi và ghi nhận một lần thất bại.
6b | Hệ thống | Tài khoản bị khóa: từ chối đăng nhập.
6c | Hệ thống | Sai quá 5 lần trong 5 phút: tạm khóa đăng nhập và thông báo thời gian chờ.
10a | Hệ thống | Mật khẩu hiện tại không đúng hoặc ảnh đại diện không phải địa chỉ https: thông báo lỗi.
post: Tài khoản được tạo; phiên đăng nhập được thiết lập hoặc hủy; thông tin hồ sơ, mật khẩu được cập nhật.
@end

@fig{DGM-UC-004-xac-thuc}{Biểu đồ use case phân rã chức năng "Xác thực và quản lý tài khoản"}

#### 2.3.2.2. Mô tả chức năng "Quản lý chuyến đi"

@usecase[UC02]{Mô tả chức năng "Quản lý chuyến đi"}
name: Quản lý chuyến đi
code: UC_02
actors: Người dùng
desc: Chức năng cho phép người dùng tạo chuyến đi, xem danh sách và chi tiết, thay đổi giai đoạn, nhân bản, xóa chuyến đi và xuất lịch trình ra tệp lịch.
trigger: Người dùng chọn “Tạo chuyến đi mới”, nhập mô tả ở ô “Lập kế hoạch bằng AI” hoặc thao tác trên trang chi tiết chuyến đi.
pre: Người dùng đã đăng nhập.
main:
== Tạo chuyến đi
1 | Người dùng | Nhập câu mô tả chuyến đi (tùy chọn) và nhấn “Phân tích & Tạo”, hoặc chọn “Tạo chuyến đi mới”.
2 | Hệ thống | Phân tích câu mô tả bằng AI (nếu có) và điền sẵn biểu mẫu nhiều bước.
3 | Người dùng | Chọn hoặc nhập điểm đến, ngày đi, ngày về, đoàn đi, sở thích, ngân sách và xác nhận.
4 | Hệ thống | Kiểm tra dữ liệu, xác định điểm đến (danh mục có sẵn, dịch vụ địa điểm hoặc nhập tay), tạo chuyến đi cùng các ngày lịch trình.
== Xem danh sách và chi tiết
5 | Người dùng | Chọn nhóm “Sắp tới & Đang đi” hoặc “Đã hoàn thành”, chọn một chuyến đi.
6 | Hệ thống | Hiển thị danh sách theo giai đoạn; trang chi tiết gồm thông tin chung, lịch trình, ngân sách, danh sách chuẩn bị và thời tiết.
== Thay đổi giai đoạn, nhân bản, xóa, xuất lịch
7 | Người dùng | Chọn giai đoạn, “Nhân bản” (chọn ngày bắt đầu mới), “Xóa” hoặc “Xuất lịch”.
8 | Hệ thống | Cập nhật giai đoạn; hoặc tạo bản sao dời toàn bộ ngày, hoạt động, danh sách chuẩn bị theo ngày mới; hoặc xóa chuyến đi cùng dữ liệu liên quan sau khi xác nhận; hoặc tạo tệp lịch để tải về.
alt:
2a | Hệ thống | AI lỗi hoặc chưa cấu hình: dùng bộ phân tích dự phòng theo mẫu câu tiếng Việt.
2b | Hệ thống | Vượt 20 lượt dùng AI trong giờ: thông báo giới hạn.
4a | Hệ thống | Ngày về trước ngày đi hoặc chuyến đi dài quá 30 ngày: thông báo lỗi.
4b | Hệ thống | Số trẻ em và người cao tuổi vượt tổng số người hoặc thiếu điểm đến: thông báo lỗi.
4c | Hệ thống | Dịch vụ địa điểm lỗi khi xác định điểm đến: thông báo lỗi dịch vụ bên ngoài.
8a | Hệ thống | Chuyến đi không thuộc người dùng: thông báo không tìm thấy.
post: Chuyến đi được tạo, cập nhật, nhân bản hoặc xóa; tệp lịch được tải về.
@end

@fig{DGM-UC-005-chuyen-di}{Biểu đồ use case phân rã chức năng "Quản lý chuyến đi"}

#### 2.3.2.3. Mô tả chức năng "Quản lý lịch trình"

@usecase[UC03]{Mô tả chức năng "Quản lý lịch trình"}
name: Quản lý lịch trình
code: UC_03
actors: Người dùng
desc: Chức năng cho phép xem lịch trình theo ngày cùng bản đồ, thêm và xóa hoạt động, nhận gợi ý chi phí, xem và chọn phương án thay thế, xem lý do AI đề xuất hoạt động.
trigger: Người dùng mở thẻ “Lịch trình chi tiết” trong trang chuyến đi.
pre: Người dùng đã đăng nhập và là chủ chuyến đi.
main:
== Xem lịch trình
1 | Người dùng | Chọn một ngày trong lịch trình.
2 | Hệ thống | Hiển thị các hoạt động theo thứ tự, khoảng cách, thời gian di chuyển ước tính giữa các hoạt động, tổng chi phí và bản đồ trong ngày.
== Thêm hoạt động
3 | Người dùng | Chọn “Thêm hoạt động”, nhập tiêu đề, loại, thời gian, chi phí hoặc chọn từ địa điểm đã lưu.
4 | Hệ thống | Gợi ý chi phí theo mức giá địa điểm và số người khi được yêu cầu.
5 | Người dùng | Xác nhận lưu.
6 | Hệ thống | Kiểm tra dữ liệu, ghi nhận địa điểm vào danh mục nếu cần, thêm hoạt động vào cuối ngày.
== Xóa hoạt động
7 | Người dùng | Chọn xóa một hoạt động và xác nhận.
8 | Hệ thống | Xóa hoạt động và đánh lại thứ tự các hoạt động còn lại.
== Phương án thay thế và giải thích
9 | Người dùng | Mở phương án thay thế của hoạt động ăn uống/lưu trú, hoặc chọn “Vì sao?”.
10 | Hệ thống | Hiển thị tối đa ba phương án theo mức tiết kiệm, cân bằng, cao cấp từ địa điểm có thật; hoặc hiển thị lý do, công cụ đã dùng và các ứng viên bị loại.
11 | Người dùng | Chọn một phương án thay thế.
12 | Hệ thống | Cập nhật địa điểm, tiêu đề và chi phí của hoạt động.
alt:
6a | Hệ thống | Giờ kết thúc trước giờ bắt đầu hoặc thiếu thông tin bắt buộc: thông báo lỗi.
6b | Hệ thống | Ngày không thuộc chuyến đi: thông báo không tìm thấy.
4a | Hệ thống | Không đủ dữ liệu để ước tính: trả về lý do, không tự đoán giá.
post: Lịch trình được cập nhật và hiển thị lại.
@end

@fig{DGM-UC-006-lich-trinh}{Biểu đồ use case phân rã chức năng "Quản lý lịch trình"}

#### 2.3.2.4. Mô tả chức năng "Sinh lịch trình tự động bằng AI"

@usecase[UC04]{Mô tả chức năng "Sinh lịch trình tự động bằng AI"}
name: Sinh lịch trình tự động bằng AI
code: UC_04
actors: Người dùng; tác nhân phụ: Dịch vụ LLM, Google Places, Open-Meteo
desc: Chức năng tạo lịch trình hoàn chỉnh cho chuyến đi từ các địa điểm có thật, có kiểm chứng và theo dõi tiến độ.
trigger: Người dùng nhấn “Tạo lịch trình AI” trên trang chi tiết chuyến đi.
pre: Người dùng là chủ chuyến đi; lịch trình chưa có hoạt động nào; dịch vụ AI đã được cấu hình.
main:
1 | Người dùng | Nhấn “Tạo lịch trình AI”.
2 | Hệ thống | Tạo tác vụ nền, đưa vào hàng đợi và trả về mã tác vụ.
3 | Hệ thống | Thu thập địa điểm thực theo điểm đến, chế độ ăn và sở thích; loại bỏ địa điểm người dùng không thích.
4 | Hệ thống | Lấy thời tiết từng ngày và tri thức điểm đến, gửi yêu cầu lập lịch tới mô hình ngôn ngữ.
5 | Hệ thống | Kiểm chứng từng hoạt động: phải khớp địa điểm thực, không trùng lặp; loại các mục không hợp lệ kèm lý do.
6 | Hệ thống | Lưu đề xuất, áp dụng vào lịch trình, sinh phương án thay thế và gửi tiến độ theo thời gian thực.
7 | Người dùng | Theo dõi thanh tiến độ, xem kết quả và số hoạt động bị loại.
alt:
2a | Hệ thống | Lịch trình đã có hoạt động: từ chối yêu cầu.
2b | Hệ thống | Đã có tác vụ đang chạy cho chuyến đi: trả về tác vụ hiện có.
2c | Hệ thống | AI chưa cấu hình hoặc vượt giới hạn sử dụng: thông báo lỗi.
5a | Hệ thống | Mô hình trả về dữ liệu không đọc được hoặc không có hoạt động hợp lệ: tác vụ thất bại, đề xuất bị đánh dấu hết hạn.
7a | Người dùng | Kết nối thời gian thực bị gián đoạn: giao diện chuyển sang hỏi trạng thái định kỳ.
post: Lịch trình được điền các hoạt động đã kiểm chứng; có thể hoàn tác trong 10 phút.
@end

@fig{DGM-UC-007-sinh-lich-trinh}{Biểu đồ use case phân rã chức năng "Sinh lịch trình tự động bằng AI"}

#### 2.3.2.5. Mô tả chức năng "Trò chuyện với trợ lý AI"

@usecase[UC05]{Mô tả chức năng "Trò chuyện với trợ lý AI"}
name: Trò chuyện với trợ lý AI
code: UC_05
actors: Người dùng; tác nhân phụ: Dịch vụ LLM
desc: Chức năng cho phép người dùng hỏi đáp với trợ lý AI về chuyến đi; trợ lý gọi các công cụ nghiệp vụ và có thể tạo đề xuất thay đổi lịch trình để người dùng áp dụng, từ chối hoặc hoàn tác.
trigger: Người dùng mở khung “Trợ lý AI” trong trang chi tiết chuyến đi.
pre: Người dùng là chủ chuyến đi; dịch vụ AI đã được cấu hình.
main:
== Hỏi đáp
1 | Người dùng | Nhập câu hỏi và gửi.
2 | Hệ thống | Lưu tin nhắn, dựng ngữ cảnh gồm hướng dẫn hệ thống, thông tin chuyến đi và lịch sử hội thoại gần nhất.
3 | Hệ thống | Lặp: gửi yêu cầu tới mô hình; nếu mô hình yêu cầu công cụ thì thực thi, ghi nhật ký và báo trạng thái công cụ lên giao diện.
4 | Hệ thống | Truyền dần câu trả lời về giao diện, lưu tin nhắn trả lời.
== Xử lý đề xuất
5 | Người dùng | Xem thẻ đề xuất (danh sách thay đổi, chênh lệch chi phí) và chọn “Áp dụng” hoặc “Từ chối”.
6 | Hệ thống | Kiểm tra đề xuất còn hiệu lực, áp dụng các thay đổi và lưu nhật ký hoàn tác; hoặc đánh dấu bị từ chối.
7 | Người dùng | Chọn “Hoàn tác” trong thời gian cho phép.
8 | Hệ thống | Khôi phục lịch trình, giữ nguyên các hoạt động người dùng đã sửa tay sau khi áp dụng.
alt:
3a | Hệ thống | Vượt số vòng hoặc thời gian cho phép: yêu cầu mô hình kết luận với thông tin đã có.
3b | Hệ thống | Lỗi trong quá trình xử lý: gửi sự kiện lỗi và đóng kết nối.
6a | Hệ thống | Đề xuất đã hết hạn, đã xử lý hoặc không còn phù hợp với lịch trình hiện tại: từ chối áp dụng.
8a | Hệ thống | Quá 10 phút hoặc có đề xuất áp dụng sau đó chạm cùng hoạt động: từ chối hoàn tác.
post: Hội thoại được lưu; lịch trình được thay đổi chỉ khi người dùng áp dụng đề xuất.
@end

@fig{DGM-UC-008-tro-ly-ai}{Biểu đồ use case phân rã chức năng "Trò chuyện với trợ lý AI"}

#### 2.3.2.6. Mô tả chức năng "Quản lý ngân sách và chi tiêu"

@usecase[UC06]{Mô tả chức năng "Quản lý ngân sách và chi tiêu"}
name: Quản lý ngân sách và chi tiêu
code: UC_06
actors: Người dùng
desc: Chức năng tổng hợp ngân sách dự kiến và thực tế, cảnh báo vượt ngân sách, tính mức chi mỗi ngày và quản lý các khoản chi.
trigger: Người dùng mở thẻ “Ngân sách & Chi tiêu”.
pre: Người dùng là chủ chuyến đi.
main:
1 | Người dùng | Mở thẻ ngân sách.
2 | Hệ thống | Hiển thị tổng ngân sách, chi phí ước tính từ lịch trình, số đã chi, số còn lại, mức cảnh báo và mức chi khuyến nghị mỗi ngày.
3 | Người dùng | Nhập số tiền, danh mục, ngày chi, ghi chú và lưu khoản chi.
4 | Hệ thống | Kiểm tra dữ liệu, lưu khoản chi, cập nhật tổng hợp.
5 | Người dùng | Xem lịch sử chi tiêu, chọn xóa một khoản chi.
6 | Hệ thống | Xóa khoản chi và cập nhật tổng hợp.
alt:
4a | Hệ thống | Số tiền âm hoặc đơn vị tiền khác với đơn vị của chuyến đi: thông báo lỗi.
2a | Hệ thống | Chưa đặt ngân sách: chỉ hiển thị số liệu, không cảnh báo.
post: Khoản chi được ghi nhận hoặc xóa; tổng hợp ngân sách được cập nhật.
@end

@fig{DGM-UC-009-ngan-sach}{Biểu đồ use case phân rã chức năng "Quản lý ngân sách và chi tiêu"}

#### 2.3.2.7. Mô tả chức năng "Quản lý danh sách chuẩn bị"

@usecase[UC07]{Mô tả chức năng "Quản lý danh sách chuẩn bị"}
name: Quản lý danh sách chuẩn bị
code: UC_07
actors: Người dùng; tác nhân phụ: Open-Meteo
desc: Chức năng quản lý hai nhóm “đồ mang theo” và “việc cần làm”, đồng thời gợi ý các mục phù hợp với chuyến đi.
trigger: Người dùng mở thẻ “Checklist chuẩn bị”.
pre: Người dùng là chủ chuyến đi.
main:
1 | Người dùng | Nhập tên mục, chọn loại và nhấn “Thêm”.
2 | Hệ thống | Lưu mục mới, bỏ qua mục trùng tên trong cùng nhóm.
3 | Người dùng | Đánh dấu hoàn thành hoặc xóa mục.
4 | Hệ thống | Cập nhật danh sách.
5 | Người dùng | Chọn “Gợi ý”.
6 | Hệ thống | Áp dụng các quy tắc theo thời tiết (mưa, nóng, lạnh), độ dài chuyến đi, đi nước ngoài, trẻ em, phương tiện, loại hoạt động; trả về danh sách gợi ý kèm lý do.
7 | Người dùng | Chọn các mục gợi ý muốn thêm.
8 | Hệ thống | Lưu các mục đã chọn với nguồn “gợi ý”.
alt:
6a | Hệ thống | Không lấy được thời tiết: bỏ qua nhóm quy tắc thời tiết và thông báo.
2a | Hệ thống | Tên mục trống hoặc quá dài: thông báo lỗi.
post: Danh sách chuẩn bị được cập nhật.
@end

@fig{DGM-UC-010-checklist}{Biểu đồ use case phân rã chức năng "Quản lý danh sách chuẩn bị"}

#### 2.3.2.8. Mô tả chức năng "Khám phá và đánh giá địa điểm"

@usecase[UC08]{Mô tả chức năng "Khám phá và đánh giá địa điểm"}
name: Khám phá và đánh giá địa điểm
code: UC_08
actors: Người dùng; tác nhân phụ: Google Places
desc: Chức năng tìm kiếm địa điểm, hiển thị trên bản đồ, lưu địa điểm yêu thích và đánh giá các địa điểm đã đến sau chuyến đi.
trigger: Người dùng mở trang “Khám phá địa điểm” hoặc chọn “Đánh giá” trên chuyến đi đã kết thúc.
pre: Người dùng đã đăng nhập; đánh giá chỉ khả dụng khi chuyến đi đã kết thúc.
main:
1 | Người dùng | Nhập từ khóa, chọn điểm đến và nhấn “Tìm kiếm”.
2 | Hệ thống | Tìm kiếm qua dịch vụ địa điểm, lưu đệm kết quả, hiển thị danh sách và đánh dấu trên bản đồ.
3 | Người dùng | Chọn “Lưu” một địa điểm hoặc mở thẻ “Đã lưu”.
4 | Hệ thống | Lưu địa điểm vào danh sách yêu thích; hiển thị danh sách đã lưu.
5 | Người dùng | Với chuyến đi đã kết thúc, chọn “Thích” hoặc “Không thích” cho từng địa điểm đã đến.
6 | Hệ thống | Lưu đánh giá; địa điểm “không thích” bị loại khỏi các gợi ý về sau.
alt:
2a | Hệ thống | Dịch vụ địa điểm lỗi hoặc không có kết quả: tìm trong danh mục địa điểm nội bộ.
6a | Hệ thống | Chuyến đi chưa kết thúc: từ chối đánh giá.
post: Danh sách địa điểm đã lưu và đánh giá được cập nhật.
@end

@fig{DGM-UC-011-dia-diem}{Biểu đồ use case phân rã chức năng "Khám phá và đánh giá địa điểm"}

#### 2.3.2.9. Mô tả chức năng "Cộng đồng chia sẻ kinh nghiệm"

@usecase[UC09]{Mô tả chức năng "Cộng đồng chia sẻ kinh nghiệm"}
name: Cộng đồng chia sẻ kinh nghiệm
code: UC_09
actors: Người dùng, Quản trị viên
desc: Chức năng xem, lọc, tìm kiếm bài viết; đăng bài, bình luận, bình chọn hữu ích; sao chép lịch trình từ bài viết; tạo bản nháp bài tổng kết bằng AI; xóa bài viết.
trigger: Người dùng mở trang “Cộng đồng & Review” hoặc chọn “AI viết review tổng kết” trên chuyến đi đã kết thúc.
pre: Người dùng đã đăng nhập.
main:
== Xem và tương tác
1 | Người dùng | Chọn điểm đến hoặc nhập từ khóa để lọc bài viết, cuộn để tải thêm.
2 | Hệ thống | Hiển thị bài viết mới nhất kèm số bình chọn, số bình luận, trạng thái đã bình chọn.
3 | Người dùng | Bình chọn hoặc viết bình luận.
4 | Hệ thống | Bật/tắt bình chọn và cập nhật số đếm; lưu bình luận.
== Đăng bài
5 | Người dùng | Chọn “Viết bài”, nhập tiêu đề, điểm đến, nội dung, ảnh và chuyến đi liên kết (tùy chọn); hoặc chọn “AI viết review” để nhận bản nháp.
6 | Hệ thống | Với bản nháp AI: tổng hợp địa điểm đã đi, chi tiêu và đánh giá, sinh nội dung và điền sẵn biểu mẫu.
7 | Người dùng | Chỉnh sửa và nhấn “Đăng bài”.
8 | Hệ thống | Kiểm tra dữ liệu, kiểm tra chuyến đi liên kết thuộc người dùng, lưu bài viết.
== Sao chép lịch trình, xóa bài
9 | Người dùng | Chọn “Sao chép lịch trình” trên bài viết có liên kết chuyến đi, chọn ngày bắt đầu.
10 | Hệ thống | Tạo chuyến đi mới thuộc người dùng với lịch trình dời theo ngày đã chọn.
11 | Người dùng/Quản trị viên | Xóa bài viết.
12 | Hệ thống | Xóa bài cùng bình luận, bình chọn.
alt:
6a | Hệ thống | AI lỗi hoặc chưa cấu hình: tạo bản nháp theo mẫu từ dữ liệu chuyến đi.
8a | Hệ thống | Ảnh không phải địa chỉ https hoặc vượt giới hạn độ dài: thông báo lỗi.
12a | Hệ thống | Người xóa không phải tác giả hay quản trị viên: từ chối.
post: Bài viết, bình luận, bình chọn được cập nhật; chuyến đi sao chép được tạo.
@end

@fig{DGM-UC-012-cong-dong}{Biểu đồ use case phân rã chức năng "Cộng đồng chia sẻ kinh nghiệm"}

#### 2.3.2.10. Mô tả chức năng "Quản trị hệ thống"

@usecase[UC10]{Mô tả chức năng "Quản trị hệ thống"}
name: Quản trị hệ thống
code: UC_10
actors: Quản trị viên
desc: Chức năng quản lý người dùng, chuyến đi, tri thức điểm đến và giám sát việc sử dụng AI.
trigger: Quản trị viên mở trang “Quản trị hệ thống”.
pre: Tài khoản có vai trò Quản trị viên.
main:
== Quản lý người dùng và chuyến đi
1 | Quản trị viên | Tìm kiếm, tạo, sửa (vai trò, trạng thái, mật khẩu) hoặc xóa tài khoản; xem, sửa, xóa chuyến đi.
2 | Hệ thống | Kiểm tra quyền và dữ liệu, lưu thay đổi; khi khóa tài khoản hoặc đặt lại mật khẩu thì vô hiệu các phiên cũ.
== Quản lý tri thức điểm đến
3 | Quản trị viên | Thêm, sửa, xóa, bật/tắt mục tri thức; hoặc chọn “Trích xuất tri thức” trên một bài viết cộng đồng.
4 | Hệ thống | Lưu mục tri thức; với trích xuất, dùng AI tóm tắt bài viết thành bản nháp tri thức để quản trị viên xác nhận.
== Giám sát AI
5 | Quản trị viên | Mở thẻ “Thống kê” hoặc “Công cụ AI”, chọn khoảng thời gian, bộ lọc.
6 | Hệ thống | Hiển thị số lượt trò chuyện, lượng token, tỷ lệ lỗi, công cụ dùng nhiều nhất và nhật ký thực thi có phân trang.
alt:
2a | Hệ thống | Quản trị viên tự xóa tài khoản của mình: từ chối.
2b | Hệ thống | Tài khoản không có quyền quản trị: từ chối truy cập.
4a | Hệ thống | AI lỗi khi trích xuất: dùng nội dung gốc của bài viết làm bản nháp.
post: Dữ liệu quản trị được cập nhật; tri thức mới được dùng trong ngữ cảnh AI.
@end

@fig{DGM-UC-013-quan-tri}{Biểu đồ use case phân rã chức năng "Quản trị hệ thống"}
