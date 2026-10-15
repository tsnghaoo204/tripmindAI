# CHƯƠNG 3. THIẾT KẾ HỆ THỐNG TRIPMIND

Thiết kế phần mềm là giai đoạn chuyển các yêu cầu đã phân tích thành mô hình chi tiết về cấu trúc, thành phần và cách các thành phần tương tác, làm cơ sở cho việc lập trình và kiểm thử. Chương này trình bày kiến trúc tổng thể, thiết kế chi tiết các chức năng chính bằng biểu đồ hoạt động, tuần tự, lớp, thiết kế cơ sở dữ liệu và thiết kế giao diện.

## 3.1. Thiết kế kiến trúc

Hệ thống được xây dựng theo kiến trúc client – server. Phía máy khách là một ứng dụng web một trang chạy trên trình duyệt; phía máy chủ là một ứng dụng Spring Boot nguyên khối được tổ chức theo các tầng điều khiển – dịch vụ – truy cập dữ liệu. Hai phía giao tiếp qua RESTful API với định dạng JSON; các tác vụ cần phản hồi theo thời gian thực như trò chuyện với trợ lý AI hay theo dõi tiến độ sinh lịch trình sử dụng cơ chế Server-Sent Events. Cách tổ chức này tách biệt rõ giao diện, nghiệp vụ và dữ liệu, tương ứng với tư tưởng của mô hình Model – View – Controller: giao diện React đóng vai trò View, các bộ điều khiển REST đóng vai trò Controller, còn tầng dịch vụ cùng các thực thể dữ liệu đóng vai trò Model.

@fig{DGM-ARC-001-tong-the}{Kiến trúc tổng thể hệ thống TripMind}

@table[T-ARCH]{Các thành phần kiến trúc của hệ thống}{3.6,12.4}
| Thành phần | Mô tả |
|---|---|
| Ứng dụng web một trang | Xây dựng bằng React và TypeScript; hiển thị dữ liệu, xử lý tương tác, lưu mã phiên trên trình duyệt và gọi API của máy chủ. |
| Bộ lọc bảo mật | Giới hạn kích thước yêu cầu, xác thực mã phiên, kiểm tra phiên bản mã để thu hồi phiên, giới hạn tần suất truy cập, phân quyền theo vai trò. |
| Tầng điều khiển | Tiếp nhận yêu cầu HTTP, kiểm tra dữ liệu đầu vào, xác định người dùng hiện tại, gọi tầng dịch vụ và trả về phản hồi JSON thống nhất. |
| Tầng dịch vụ | Chứa quy tắc nghiệp vụ: kiểm tra quyền sở hữu, tính ngân sách, gợi ý danh sách chuẩn bị, thời tiết, quản lý chuyến đi, lịch trình, cộng đồng, quản trị. |
| Mô-đun tác tử AI | Điều phối vòng lặp hội thoại với mô hình ngôn ngữ, thực thi công cụ, tạo và áp dụng đề xuất, sinh lịch trình chạy nền, kiểm chứng địa điểm. |
| Tầng truy cập dữ liệu | Ánh xạ thực thể – bảng bằng JPA, thực hiện truy vấn, khóa bản ghi khi cần bảo đảm nhất quán. |
| PostgreSQL | Lưu trữ toàn bộ dữ liệu nghiệp vụ có quan hệ. |
| Redis | Lưu bộ đếm giới hạn tần suất, bộ đệm thời tiết và kết quả tìm kiếm địa điểm, hàng đợi và trạng thái tác vụ sinh lịch trình. |
| Dịch vụ bên ngoài | Dịch vụ LLM, Google Places, Google Time Zone, Open-Meteo, máy chủ bản đồ nền. |

Một số nguyên tắc thiết kế xuyên suốt được áp dụng:

- Mọi truy cập tới dữ liệu thuộc chuyến đi đều kiểm tra quyền sở hữu; nếu dữ liệu không thuộc người dùng hiện tại, hệ thống trả về “không tìm thấy” thay vì “không có quyền” để không làm lộ sự tồn tại của dữ liệu.
- Lời gọi tới dịch vụ bên ngoài được thực hiện ngoài giao dịch cơ sở dữ liệu ở hầu hết các luồng, tránh giữ kết nối lâu.
- AI không ghi trực tiếp vào lịch trình mà tạo đề xuất; việc áp dụng đề xuất khóa bản ghi chuyến đi để tránh xung đột khi nhiều thao tác diễn ra đồng thời.
- Lỗi được chuẩn hóa thành mã lỗi nghiệp vụ và mã trạng thái HTTP tương ứng, không làm lộ chi tiết nội bộ.

## 3.2. Thiết kế các chức năng

### 3.2.1. Chức năng "Xác thực người dùng"

- **Biểu đồ hoạt động:**

Người dùng chưa có tài khoản thực hiện đăng ký, sau đó đăng nhập. Khi đăng nhập, hệ thống kiểm tra số lần đăng nhập sai trước khi đối chiếu mật khẩu để chống dò mật khẩu; đăng nhập thành công sẽ xóa bộ đếm và cấp mã phiên.

@fig{DGM-ACT-001-xac-thuc}{Biểu đồ hoạt động chức năng "Xác thực người dùng"}

- **Biểu đồ tuần tự:**

@fig{DGM-SEQ-001-xac-thuc}{Biểu đồ tuần tự chức năng "Xác thực người dùng"}

- **Biểu đồ lớp:**

@fig{DGM-CLS-001-xac-thuc}{Biểu đồ lớp chức năng "Xác thực người dùng"}

### 3.2.2. Chức năng "Tạo chuyến đi"

- **Biểu đồ hoạt động:**

Nếu người dùng nhập câu mô tả, hệ thống kiểm tra hạn mức sử dụng AI rồi gửi câu mô tả tới mô hình ngôn ngữ để trích xuất thông tin; khi AI không khả dụng, bộ phân tích dự phòng theo mẫu câu tiếng Việt được sử dụng. Kết quả chỉ dùng để điền sẵn biểu mẫu, chuyến đi chỉ được tạo khi người dùng xác nhận.

@fig{DGM-ACT-002-tao-chuyen-di}{Biểu đồ hoạt động chức năng "Tạo chuyến đi"}

- **Biểu đồ tuần tự:**

@fig{DGM-SEQ-002-tao-chuyen-di}{Biểu đồ tuần tự chức năng "Tạo chuyến đi"}

- **Biểu đồ lớp:**

@fig{DGM-CLS-002-tao-chuyen-di}{Biểu đồ lớp chức năng "Tạo chuyến đi"}

### 3.2.3. Chức năng "Thêm hoạt động vào lịch trình"

- **Biểu đồ hoạt động:**

Hoạt động có thể gắn với một địa điểm đã lưu hoặc nhập tự do. Gợi ý chi phí chỉ được đưa ra khi có mức giá của địa điểm; nếu thiếu dữ liệu hệ thống trả về lý do thay vì tự đoán. Địa điểm lấy từ dịch vụ tìm kiếm chỉ được ghi vào danh mục khi người dùng thực sự sử dụng.

@fig{DGM-ACT-003-them-hoat-dong}{Biểu đồ hoạt động chức năng "Thêm hoạt động vào lịch trình"}

- **Biểu đồ tuần tự:**

@fig{DGM-SEQ-003-them-hoat-dong}{Biểu đồ tuần tự chức năng "Thêm hoạt động vào lịch trình"}

- **Biểu đồ lớp:**

@fig{DGM-CLS-003-them-hoat-dong}{Biểu đồ lớp chức năng "Thêm hoạt động vào lịch trình"}

### 3.2.4. Chức năng "Sinh lịch trình tự động bằng AI"

- **Biểu đồ hoạt động:**

Yêu cầu sinh lịch trình được xử lý bất đồng bộ. Tiến trình nền lần lượt thu thập danh sách địa điểm thực, tổng hợp thời tiết và tri thức điểm đến, yêu cầu mô hình ngôn ngữ lập lịch, rồi kiểm chứng từng hoạt động trước khi áp dụng. Mỗi chuyến đi chỉ có một tác vụ chạy tại một thời điểm; tác vụ bị gián đoạn được tự động đưa lại vào hàng đợi.

@fig{DGM-ACT-004-sinh-lich-trinh}{Biểu đồ hoạt động chức năng "Sinh lịch trình tự động bằng AI"}

- **Biểu đồ tuần tự:**

@fig{DGM-SEQ-004-sinh-lich-trinh}{Biểu đồ tuần tự chức năng "Sinh lịch trình tự động bằng AI"}

- **Biểu đồ lớp:**

@fig{DGM-CLS-004-sinh-lich-trinh}{Biểu đồ lớp chức năng "Sinh lịch trình tự động bằng AI"}

### 3.2.5. Chức năng "Trò chuyện với trợ lý AI và áp dụng đề xuất"

- **Biểu đồ hoạt động:**

Mỗi lượt hội thoại là một vòng lặp: mô hình ngôn ngữ có thể yêu cầu gọi công cụ nhiều lần, mỗi lần gọi đều được ghi nhật ký; vòng lặp dừng khi có câu trả lời, khi đạt 8 vòng hoặc hết 60 giây. Thay đổi lịch trình chỉ xảy ra khi người dùng áp dụng một đề xuất còn hiệu lực.

@fig{DGM-ACT-005-tro-ly-ai}{Biểu đồ hoạt động chức năng "Trò chuyện với trợ lý AI và áp dụng đề xuất"}

- **Biểu đồ tuần tự:**

@fig{DGM-SEQ-005-tro-ly-ai}{Biểu đồ tuần tự chức năng "Trò chuyện với trợ lý AI và áp dụng đề xuất"}

- **Biểu đồ lớp:**

@fig{DGM-CLS-005-tro-ly-ai}{Biểu đồ lớp chức năng "Trò chuyện với trợ lý AI và áp dụng đề xuất"}

### 3.2.6. Chức năng "Chia sẻ bài viết cộng đồng"

- **Biểu đồ hoạt động:**

Người dùng có thể tự viết bài hoặc yêu cầu AI soạn bản tổng kết từ dữ liệu chuyến đi đã kết thúc. Bản nháp luôn được người dùng xem và chỉnh sửa trước khi đăng; nếu AI không khả dụng, bản nháp được dựng theo mẫu từ dữ liệu thực của chuyến đi.

@fig{DGM-ACT-006-cong-dong}{Biểu đồ hoạt động chức năng "Chia sẻ bài viết cộng đồng"}

- **Biểu đồ tuần tự:**

@fig{DGM-SEQ-006-cong-dong}{Biểu đồ tuần tự chức năng "Chia sẻ bài viết cộng đồng"}

- **Biểu đồ lớp:**

@fig{DGM-CLS-006-cong-dong}{Biểu đồ lớp chức năng "Chia sẻ bài viết cộng đồng"}

### 3.2.7. Chức năng "Quản trị tri thức điểm đến"

- **Biểu đồ hoạt động:**

Nội dung bài viết cộng đồng được coi là dữ liệu không tin cậy và được đánh dấu tách biệt khi đưa vào mô hình ngôn ngữ nhằm hạn chế việc chèn chỉ dẫn độc hại. Quản trị viên luôn kiểm tra, chỉnh sửa bản nháp trước khi lưu thành tri thức chính thức.

@fig{DGM-ACT-007-tri-thuc}{Biểu đồ hoạt động chức năng "Quản trị tri thức điểm đến"}

- **Biểu đồ tuần tự:**

@fig{DGM-SEQ-007-tri-thuc}{Biểu đồ tuần tự chức năng "Quản trị tri thức điểm đến"}

- **Biểu đồ lớp:**

@fig{DGM-CLS-007-tri-thuc}{Biểu đồ lớp chức năng "Quản trị tri thức điểm đến"}

## 3.3. Thiết kế cơ sở dữ liệu

Cơ sở dữ liệu quan hệ gồm 21 bảng được sử dụng trong ứng dụng, chia thành hai nhóm chính. Khóa chính của tất cả các bảng là số nguyên tự tăng; thời điểm được lưu kèm múi giờ; số tiền được lưu dạng số nguyên theo đơn vị tiền tệ của chuyến đi; các cấu trúc linh hoạt như hồ sơ đoàn đi, danh sách thay đổi của đề xuất được lưu dạng JSONB. Giá trị liệt kê được ràng buộc bằng điều kiện kiểm tra ở mức cơ sở dữ liệu.

Nhóm thứ nhất quản lý người dùng, chuyến đi, lịch trình, địa điểm, chi tiêu và danh sách chuẩn bị (Hình {fig:DGM-ERD-001-chuyen-di}). Mỗi chuyến đi thuộc một người dùng và một điểm đến, gồm nhiều ngày lịch trình (mỗi ngày một bản ghi, duy nhất theo chuyến đi và ngày); mỗi ngày gồm nhiều hoạt động có thứ tự, hoạt động có thể gắn với một địa điểm. Ràng buộc duy nhất về thứ tự hoạt động trong ngày được kiểm tra trễ tới cuối giao dịch để cho phép sắp xếp lại.

@fig{DGM-ERD-001-chuyen-di}{Biểu đồ thực thể liên kết nhóm người dùng – chuyến đi – lịch trình}

Nhóm thứ hai phục vụ trợ lý AI, cộng đồng và tri thức điểm đến (Hình {fig:DGM-ERD-002-ai-cong-dong}). Mỗi hội thoại gắn với một chuyến đi, gồm nhiều tin nhắn; đề xuất AI lưu danh sách thay đổi, trạng thái và nhật ký hoàn tác; nhật ký công cụ ghi lại mọi lần gọi công cụ. Bài viết cộng đồng có thể liên kết với một chuyến đi và có thể được chuyển thành một mục tri thức điểm đến.

@fig{DGM-ERD-002-ai-cong-dong}{Biểu đồ thực thể liên kết nhóm trợ lý AI – cộng đồng – tri thức}

@table[T-ENT]{Mô tả các thực thể chính}{4.2,11.8}
| Thực thể | Ý nghĩa và ràng buộc chính |
|---|---|
| users | Tài khoản; email duy nhất; vai trò Người dùng hoặc Quản trị viên; trạng thái hoạt động; phiên bản mã phiên dùng để thu hồi phiên. |
| user_preferences | Sở thích du lịch mặc định của người dùng (quan hệ một – một), được tạo từ chuyến đi đầu tiên. |
| destinations | Điểm đến (tỉnh, thành phố) với tọa độ và múi giờ; có sẵn 15 điểm đến phổ biến. |
| trips | Chuyến đi: thời gian (ngày về không trước ngày đi), số người, ngân sách, tiền tệ, phong cách, hồ sơ đoàn đi, kế hoạch di chuyển, giai đoạn điều chỉnh thủ công. |
| itinerary_days | Ngày lịch trình, duy nhất theo chuyến đi và số thứ tự ngày. |
| activities | Hoạt động: loại, người tạo (người dùng hoặc AI), thời gian, chi phí ước tính và nguồn ước tính, thứ tự, trạng thái thực hiện, đề xuất sinh ra hoạt động. |
| activity_alternatives | Tối đa ba phương án thay thế theo mức tiết kiệm, cân bằng, cao cấp cho hoạt động ăn uống, lưu trú. |
| places | Địa điểm đã được sử dụng, duy nhất theo nhà cung cấp và mã ngoài; ghi nhận lý do đưa vào danh mục. |
| saved_places, place_ratings | Địa điểm yêu thích; đánh giá thích/không thích sau chuyến đi (mỗi người dùng một đánh giá cho một địa điểm). |
| expenses | Khoản chi thực tế: danh mục, số tiền không âm, tiền tệ trùng với chuyến đi, ngày chi. |
| trip_checklist_items | Mục chuẩn bị loại đồ mang theo hoặc việc cần làm; không trùng tên trong cùng loại; nguồn người dùng, gợi ý hoặc AI. |
| conversations, messages | Hội thoại với trợ lý AI theo chuyến đi và các tin nhắn kèm lượng token sử dụng. |
| ai_proposals | Đề xuất thay đổi: danh sách thao tác, chênh lệch chi phí, trạng thái (chờ duyệt, đã áp dụng, bị từ chối, hết hạn, đã hoàn tác), hạn hiệu lực, nhật ký hoàn tác. |
| ai_tool_executions | Nhật ký thực thi công cụ: tên công cụ, tham số, kết quả, trạng thái, thời gian thực thi. |
| destination_knowledge | Tri thức điểm đến do quản trị viên kiểm duyệt: danh mục, nội dung, độ ưu tiên 1–3, trạng thái bật/tắt. |
| blog_posts, blog_comments, blog_post_upvotes, blog_comment_upvotes | Bài viết, bình luận và bình chọn; mỗi người dùng chỉ bình chọn một lần cho mỗi bài viết hoặc bình luận. |

## 3.4. Thiết kế giao diện

Giao diện được phác thảo dưới dạng khung dây trước khi xây dựng. Các màn hình dùng chung thanh điều hướng gồm logo, các mục Chuyến đi, Địa điểm, Cộng đồng (và Quản trị đối với quản trị viên), nút chuyển chế độ sáng/tối và tài khoản.

### 3.4.1. Giao diện "Đăng nhập, đăng ký"

@fig{UI-001-auth}{Hình dung giao diện "Đăng nhập, đăng ký"}

### 3.4.2. Giao diện "Danh sách chuyến đi và tạo chuyến đi"

Trang chủ có ô nhập mô tả bằng ngôn ngữ tự nhiên kèm các gợi ý nhanh; khi nhấn “Phân tích & Tạo”, hộp thoại tạo chuyến đi nhiều bước mở ra với thông tin đã được điền sẵn.

@fig{UI-002-dashboard}{Hình dung giao diện "Danh sách chuyến đi và tạo chuyến đi"}

### 3.4.3. Giao diện "Chi tiết chuyến đi"

@fig{UI-003-trip-detail}{Hình dung giao diện "Chi tiết chuyến đi", trợ lý AI và thêm hoạt động}

### 3.4.4. Giao diện "Ngân sách và danh sách chuẩn bị"

@fig{UI-004-budget-checklist}{Hình dung giao diện "Ngân sách và danh sách chuẩn bị"}

### 3.4.5. Giao diện "Khám phá địa điểm"

@fig{UI-005-places}{Hình dung giao diện "Khám phá địa điểm"}

### 3.4.6. Giao diện "Cộng đồng"

@fig{UI-006-community}{Hình dung giao diện "Cộng đồng" và viết bài chia sẻ}

### 3.4.7. Giao diện "Quản trị hệ thống"

@fig{UI-007-admin}{Hình dung giao diện "Quản trị hệ thống"}
