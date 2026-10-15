# CHƯƠNG 5. KIỂM THỬ PHẦN MỀM

## 5.1. Mục tiêu kiểm thử

- Phát hiện lỗi trong các luồng nghiệp vụ chính: xác thực, quản lý chuyến đi, lịch trình, ngân sách, danh sách chuẩn bị, cộng đồng và quản trị.
- Xác nhận các quy tắc an toàn của AI: AI không tự ghi dữ liệu, địa điểm bịa đặt bị loại, đề xuất không thể áp dụng hai lần, hoàn tác không làm mất chỉnh sửa thủ công.
- Bảo đảm tách biệt dữ liệu giữa các người dùng và phân quyền giữa Người dùng và Quản trị viên.
- Kiểm tra khả năng chống chịu khi dịch vụ bên ngoài lỗi hoặc không khả dụng.

## 5.2. Phạm vi kiểm thử

Kiểm thử tập trung vào tầng máy chủ, nơi chứa toàn bộ quy tắc nghiệp vụ, và các mô-đun xử lý dữ liệu quan trọng ở giao diện:

- Xác thực và bảo mật.
- Quản lý chuyến đi và lịch trình.
- Sinh lịch trình tự động bằng AI.
- Trợ lý AI và đề xuất thay đổi.
- Ngân sách, danh sách chuẩn bị, đánh giá địa điểm.
- Cộng đồng và quản trị.

## 5.3. Phương pháp và môi trường kiểm thử

Hệ thống được kiểm thử tự động ở hai mức:

- Kiểm thử đơn vị: kiểm tra từng dịch vụ và thuật toán riêng lẻ (tính ngân sách, ước tính chi phí, tính khoảng cách, tối ưu thứ tự hoạt động, gợi ý danh sách chuẩn bị, xuất tệp lịch, xác định ngày theo múi giờ điểm đến) với các phụ thuộc được giả lập.
- Kiểm thử tích hợp API: khởi động toàn bộ ứng dụng, gửi yêu cầu HTTP thực tới API với cơ sở dữ liệu PostgreSQL 16 và Redis 7.4 thật chạy trong vùng chứa. Dịch vụ LLM được thay bằng một bộ giả lập trả lời theo kịch bản định sẵn để kết quả lặp lại được; dịch vụ địa điểm chạy ở chế độ giả lập; dịch vụ thời tiết được cố ý trỏ tới địa chỉ không truy cập được để kiểm tra nhánh xử lý lỗi.

Phía giao diện có các kiểm thử đơn vị cho bộ chuẩn hóa lỗi, bộ đọc luồng sự kiện, luồng trò chuyện và quản lý trạng thái đề xuất, cùng bước kiểm tra kiểu toàn bộ mã nguồn. Các bộ kiểm thử được cấu hình chạy tự động trong quy trình tích hợp liên tục.

## 5.4. Kiểm thử một số chức năng

Các bảng dưới đây trình bày kịch bản kiểm thử tiêu biểu. Đầu ra thực tế được ghi nhận từ lần chạy bộ kiểm thử tự động ngày 15/10/2026.

### 5.4.1. Kiểm thử chức năng "Xác thực người dùng"

@testcase[KT01]{Kiểm thử chức năng "Xác thực người dùng"}
id: KT_01
desc: Kiểm thử đăng ký, đăng nhập, đổi mật khẩu
branch: Xác thực người dùng
rows:
| 1 | Đăng ký thành công | Email mới, mật khẩu hợp lệ | Tạo tài khoản vai trò Người dùng | Giống đầu ra kỳ vọng |
| 2 | Đăng ký email đã tồn tại | Email đã đăng ký | Báo lỗi xung đột, không tạo tài khoản | Giống đầu ra kỳ vọng |
| 3 | Đăng ký mật khẩu ngắn | Mật khẩu dưới 8 ký tự | Báo lỗi dữ liệu không hợp lệ | Giống đầu ra kỳ vọng |
| 4 | Đăng nhập sai mật khẩu | Email đúng, mật khẩu sai | Báo lỗi xác thực | Giống đầu ra kỳ vọng |
| 5 | Dò mật khẩu | Sai mật khẩu 5 lần liên tiếp, lần 6 nhập đúng | Lần 6 vẫn bị tạm khóa, trả về thời gian chờ | Giống đầu ra kỳ vọng |
| 6 | Đổi mật khẩu | Mật khẩu hiện tại đúng, mật khẩu mới | Cấp phiên mới; phiên cũ bị từ chối | Giống đầu ra kỳ vọng |
| 7 | Quản trị viên khóa tài khoản | Khóa tài khoản đang đăng nhập | Phiên của tài khoản bị vô hiệu | Giống đầu ra kỳ vọng |
@end

### 5.4.2. Kiểm thử chức năng "Quản lý chuyến đi và lịch trình"

@testcase[KT02]{Kiểm thử chức năng "Quản lý chuyến đi và lịch trình"}
id: KT_02
desc: Kiểm thử tạo chuyến đi, thêm hoạt động, nhân bản, phân quyền dữ liệu
branch: Quản lý chuyến đi, Quản lý lịch trình
rows:
| 1 | Tạo chuyến đi hợp lệ | Điểm đến, ngày đi, ngày về, đoàn đi, sở thích | Tạo chuyến đi và đủ số ngày lịch trình, lưu sở thích và hồ sơ đoàn | Giống đầu ra kỳ vọng |
| 2 | Chuyến đi quá dài | Thời gian trên 30 ngày | Từ chối, báo chuyến đi quá dài | Giống đầu ra kỳ vọng |
| 3 | Truy cập chuyến đi của người khác | Người dùng B mở chuyến đi của A | Phản hồi không tìm thấy | Giống đầu ra kỳ vọng |
| 4 | Thêm hoạt động từ kết quả tìm kiếm | Hoạt động gắn địa điểm từ dịch vụ tìm kiếm | Thêm hoạt động, địa điểm được ghi vào danh mục | Giống đầu ra kỳ vọng |
| 5 | Khung giờ sai | Giờ kết thúc trước giờ bắt đầu | Báo lỗi khung giờ | Giống đầu ra kỳ vọng |
| 6 | Phương án thay thế | Hoạt động ăn uống | Trả về ba phương án là địa điểm có thật; chọn được phương án | Giống đầu ra kỳ vọng |
| 7 | Nhân bản chuyến đi | Ngày bắt đầu mới | Sao chép ngày, hoạt động, danh sách chuẩn bị; không sao chép khoản chi | Giống đầu ra kỳ vọng |
@end

### 5.4.3. Kiểm thử chức năng "Sinh lịch trình tự động bằng AI"

@testcase[KT03]{Kiểm thử chức năng "Sinh lịch trình tự động bằng AI"}
id: KT_03
desc: Kiểm thử tác vụ sinh lịch trình và bước kiểm chứng địa điểm
branch: Sinh lịch trình tự động bằng AI
rows:
| 1 | Sinh lịch trình có địa điểm bịa đặt | Kế hoạch giả lập gồm địa điểm thật và địa điểm không tồn tại | Giữ địa điểm thật, loại và đếm số địa điểm không tồn tại | Giống đầu ra kỳ vọng |
| 2 | Lịch trình đã có hoạt động | Chuyến đi đã có hoạt động | Từ chối yêu cầu | Giống đầu ra kỳ vọng |
| 3 | Gửi yêu cầu lặp lại | Gọi sinh lịch trình hai lần liên tiếp | Trả về cùng một tác vụ | Giống đầu ra kỳ vọng |
| 4 | Kế hoạch trả về không đầy đủ | Kế hoạch có ngày trùng, thiếu trường | Đọc được phần hợp lệ, gộp ngày trùng, sinh phương án thay thế | Giống đầu ra kỳ vọng |
@end

### 5.4.4. Kiểm thử chức năng "Trợ lý AI và đề xuất"

@testcase[KT04]{Kiểm thử chức năng "Trợ lý AI và đề xuất"}
id: KT_04
desc: Kiểm thử vòng lặp tác tử, áp dụng, hoàn tác đề xuất
branch: Trò chuyện với trợ lý AI
rows:
| 1 | Lượt hội thoại có gọi công cụ | Mô hình yêu cầu hai công cụ | Phát sự kiện công cụ và nội dung trả lời; ghi hai dòng nhật ký thành công | Giống đầu ra kỳ vọng |
| 2 | Mô hình gọi công cụ liên tục | Mô hình luôn yêu cầu công cụ | Dừng sau 8 vòng | Giống đầu ra kỳ vọng |
| 3 | Mô hình truyền mã chuyến đi khác | Tham số trỏ tới chuyến đi khác | Công cụ bỏ qua, chỉ dùng chuyến đi hiện tại | Giống đầu ra kỳ vọng |
| 4 | Áp dụng đề xuất hai lần | Áp dụng cùng một đề xuất lần thứ hai | Từ chối, không nhân đôi hoạt động | Giống đầu ra kỳ vọng |
| 5 | Đề xuất chứa địa điểm bịa đặt | Mã địa điểm không có thật | Thao tác bị loại | Giống đầu ra kỳ vọng |
| 6 | Hoàn tác sau khi sửa tay | Áp dụng, người dùng sửa một hoạt động, rồi hoàn tác | Giữ hoạt động đã sửa, khôi phục phần còn lại | Giống đầu ra kỳ vọng |
| 7 | Hoàn tác quá hạn | Hoàn tác sau 10 phút | Từ chối, báo hết thời gian hoàn tác | Giống đầu ra kỳ vọng |
@end

### 5.4.5. Kiểm thử chức năng "Ngân sách và danh sách chuẩn bị"

@testcase[KT05]{Kiểm thử chức năng "Ngân sách và danh sách chuẩn bị"}
id: KT_05
desc: Kiểm thử cảnh báo ngân sách, khoản chi, gợi ý chuẩn bị
branch: Quản lý ngân sách, Quản lý danh sách chuẩn bị
rows:
| 1 | Mức cảnh báo ngân sách | Chi tiêu 89%, 91%, 101% ngân sách | Không cảnh báo, sắp vượt, vượt ngân sách | Giống đầu ra kỳ vọng |
| 2 | Khoản chi sai tiền tệ | Tiền tệ khác chuyến đi | Từ chối khoản chi | Giống đầu ra kỳ vọng |
| 3 | Hoạt động bị bỏ qua | Hoạt động có trạng thái bỏ qua | Không tính vào chi phí ước tính | Giống đầu ra kỳ vọng |
| 4 | Gợi ý theo thời tiết | Ngày mưa, có hoạt động biển | Gợi ý áo mưa, đồ bơi kèm lý do | Giống đầu ra kỳ vọng |
| 5 | Dịch vụ thời tiết lỗi | Không kết nối được dịch vụ thời tiết | Bỏ qua nhóm quy tắc thời tiết và báo lại | Giống đầu ra kỳ vọng |
| 6 | Thêm mục trùng | Mục cùng tên, cùng loại | Không tạo bản trùng | Giống đầu ra kỳ vọng |
@end

### 5.4.6. Kiểm thử chức năng "Bảo mật, cộng đồng và quản trị"

@testcase[KT06]{Kiểm thử chức năng "Bảo mật, cộng đồng và quản trị"}
id: KT_06
desc: Kiểm thử phân quyền, giới hạn tần suất, bài viết cộng đồng
branch: Cộng đồng, Quản trị hệ thống
rows:
| 1 | Người dùng thường gọi chức năng quản trị | Tài khoản vai trò Người dùng | Từ chối, không có quyền | Giống đầu ra kỳ vọng |
| 2 | Vượt giới hạn tần suất | Hơn 120 yêu cầu trong một phút | Từ chối, trả về thời gian chờ | Giống đầu ra kỳ vọng |
| 3 | Nội dung yêu cầu quá lớn | Nội dung trên 1 MB | Từ chối, không lộ thông tin nội bộ | Giống đầu ra kỳ vọng |
| 4 | Xóa bài viết của người khác | Người dùng không phải tác giả | Từ chối; quản trị viên xóa được | Giống đầu ra kỳ vọng |
| 5 | Liên kết chuyến đi của người khác | Đăng bài gắn chuyến đi không thuộc mình | Từ chối | Giống đầu ra kỳ vọng |
| 6 | Dữ liệu công khai của bài viết | Xem bài viết | Không hiển thị email, vai trò tác giả | Giống đầu ra kỳ vọng |
| 7 | Tri thức điểm đến | Tắt một mục tri thức | Mục bị loại khỏi ngữ cảnh AI; bật lại thì được đưa vào | Giống đầu ra kỳ vọng |
@end

## 5.5. Kết quả và đánh giá

@table[T-TESTSUM]{Tổng hợp kết quả chạy kiểm thử tự động}{4.0,4.4,3.6,4.0}
| Bộ kiểm thử | Phạm vi | Số ca kiểm thử | Kết quả |
|---|---|---|---|
| Backend – đơn vị và tích hợp | 32 lớp kiểm thử | 116 | 116 đạt, 0 lỗi, 0 bỏ qua |
| Frontend – đơn vị | 4 tệp kiểm thử | 18 | 18 đạt |
| Frontend – kiểm tra kiểu | Toàn bộ mã nguồn giao diện | — | Không có lỗi kiểu |

Toàn bộ ca kiểm thử tự động đều đạt, cho thấy các quy tắc nghiệp vụ cốt lõi và các cơ chế an toàn của AI hoạt động đúng như thiết kế. Tuy nhiên, kết quả này có các giới hạn sau:

- Chưa đo độ bao phủ mã nguồn nên không có số liệu tỷ lệ bao phủ.
- Phía giao diện chưa có kiểm thử thành phần và kiểm thử đầu – cuối tự động; kết quả kiểm thử thủ công trên giao diện cần được ghi nhận bổ sung.
- Các kiểm thử AI dùng bộ giả lập mô hình; chất lượng câu trả lời của mô hình thực chưa được đánh giá định lượng.
- Chưa thực hiện kiểm thử hiệu năng, chịu tải và quét bảo mật tự động.
- Hai tính năng mới bổ sung (tạo bài tổng kết bằng AI, sao chép lịch trình từ bài viết cộng đồng) chưa có kiểm thử tự động.
