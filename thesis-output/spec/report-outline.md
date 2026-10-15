# Dàn ý báo cáo (TÀI LIỆU NỘI BỘ)

Nguyên tắc: bám cấu trúc 5 chương của PDF mẫu; chỉ trình bày chức năng IMPLEMENTED; chức năng BACKEND-ONLY / PROPOSED-ONLY chỉ nêu ở phần hạn chế – hướng phát triển (không vẽ UC cho chúng).

## Phần đầu
Bìa ngoài · Bìa trong · Lời nói đầu · Mục lục · Danh mục viết tắt · Danh mục hình · Danh mục bảng

## MỞ ĐẦU
1 Lý do chọn đề tài · 2 Mục tiêu · 3 Đối tượng và phạm vi · 4 Phương pháp thực hiện · 5 Bố cục báo cáo

## CHƯƠNG 1. CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ
1.1 Tổng quan về bài toán lập kế hoạch du lịch (định nghĩa, đặc điểm, lợi ích, thách thức, xu hướng)
1.2 Mô hình ngôn ngữ lớn và AI Agent (LLM; tool calling; vòng lặp tác tử; human-in-the-loop; kiểm chứng chống ảo giác)
1.3 Công nghệ phía máy chủ (Java/Spring Boot, Spring Security + JWT, Spring Data JPA, Spring AI, PostgreSQL, Redis/Redisson)
1.4 Công nghệ phía máy khách (React + TypeScript + Vite, TanStack Router/Query, Tailwind/Radix, Leaflet)
1.5 Dịch vụ bên thứ ba (Google Places, Google Time Zone, Open-Meteo, LLM tương thích OpenAI)
1.6 Công cụ triển khai (Docker Compose, GitHub Actions, Testcontainers)

## CHƯƠNG 2. PHÂN TÍCH VÀ ĐẶC TẢ YÊU CẦU
2.1 Mô tả bài toán
2.2 Yêu cầu hệ thống (2.2.1 chức năng theo tác nhân; 2.2.2 phi chức năng)
2.3 Mô hình hóa và mô tả chức năng
- 2.3.1 Mô hình hóa: UC tổng quan; UC phía người dùng; UC phía quản trị
- 2.3.2 Mô tả chức năng (bảng đặc tả + UC phân rã):
  UC01 Xác thực và quản lý tài khoản · UC02 Quản lý chuyến đi · UC03 Quản lý lịch trình
  UC04 Sinh lịch trình tự động bằng AI · UC05 Trò chuyện với trợ lý AI và xử lý đề xuất
  UC06 Quản lý ngân sách và chi tiêu · UC07 Quản lý danh sách chuẩn bị · UC08 Khám phá và đánh giá địa điểm
  UC09 Cộng đồng chia sẻ kinh nghiệm · UC10 Quản trị hệ thống

## CHƯƠNG 3. THIẾT KẾ HỆ THỐNG
3.1 Thiết kế kiến trúc (sơ đồ tổng thể + bảng thành phần)
3.2 Thiết kế các chức năng (mỗi chức năng: hoạt động → tuần tự → lớp)
  3.2.1 Xác thực người dùng (đăng nhập/đăng ký) · 3.2.2 Tạo chuyến đi (có phân tích mô tả bằng AI)
  3.2.3 Thêm hoạt động vào lịch trình · 3.2.4 Sinh lịch trình tự động bằng AI · 3.2.5 Trợ lý AI và áp dụng đề xuất
  3.2.6 Chia sẻ bài viết cộng đồng và review AI · 3.2.7 Quản trị tri thức điểm đến
3.3 Thiết kế cơ sở dữ liệu (ERD nhóm chuyến đi; ERD nhóm AI–cộng đồng; bảng mô tả thực thể)
3.4 Thiết kế giao diện (7 wireframe)

## CHƯƠNG 4. CÀI ĐẶT VÀ TRIỂN KHAI
4.1 Cài đặt Backend (bảng công nghệ; mô hình kiến trúc; bảo mật; xử lý nền)
4.2 Cài đặt Frontend (bảng công nghệ; mô hình kiến trúc)
4.3 Cài đặt AI Agent (vòng lặp; bảng công cụ)
4.4 Triển khai hệ thống (sơ đồ triển khai; CI)
4.5 Giao diện chương trình (ảnh chụp do người dùng bổ sung — placeholder)

## CHƯƠNG 5. KIỂM THỬ PHẦN MỀM
5.1 Mục tiêu · 5.2 Phạm vi · 5.3 Phương pháp và môi trường (kiểm thử tự động đơn vị/tích hợp, CI) · 5.4 Kiểm thử một số chức năng (bảng test case) · 5.5 Kết quả và đánh giá (chỉ số liệu có bằng chứng)

## KẾT LUẬN (kết quả đạt được · hạn chế · hướng phát triển)
## TÀI LIỆU THAM KHẢO
