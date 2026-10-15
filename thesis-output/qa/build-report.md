# Báo cáo build và bàn giao (NỘI BỘ)

## Sản phẩm đã tạo
| Sản phẩm | Vị trí | Trạng thái |
|---|---|---|
| Báo cáo Word | report/final-report.docx | Đã build, 98 trang (PDF LibreOffice), 63 hình, 23 bảng |
| Bản PDF xem thử | report/final-report.pdf | Từ LibreOffice |
| Chương nguồn | chapters/00–08 *.md, meta.json | Chỉnh sửa độc lập được |
| Sơ đồ nguồn | diagrams/src/*.drawio (41) | 13 UC, 7 hoạt động, 7 tuần tự, 7 lớp, 2 ERD, 5 kiến trúc |
| Sơ đồ xuất | diagrams/export/*.png | |
| Wireframe | ui-design/src/*.drawio (7), ui-design/export/*.png | |
| Tài liệu phân tích | spec/*.md (11 tệp) | Có tham chiếu mã nguồn — không ghép vào báo cáo |
| Script | scripts/build.sh, build_report.py, update_fields.py, export_diagrams.sh, check_report.py, gen_inventory.py, drawio_lib.py | Pipeline chạy lại được toàn bộ (đã chạy thử end-to-end) |
| Danh mục ảnh cần bổ sung | assets/placeholders/asset-manifest.md | 16 mục |

## Thông tin người dùng cần cung cấp / xác nhận
1. Họ tên, mã sinh viên, lớp, GVHD (chapters/meta.json và Lời nói đầu trong chapters/00-front-matter.md).
2. Tên đề tài chính thức (hiện: “Xây dựng nền tảng lập kế hoạch và quản lý chuyến du lịch thông minh TripMind sử dụng AI Agent”).
3. Nhà cung cấp LLM dùng khi demo (mã nguồn trung lập; giao diện có dòng nhắc “Gemini”, cấu hình cục bộ liệt kê các mô hình họ GPT). Báo cáo hiện viết trung lập.
4. Logo trường, ảnh minh họa Hình 1.1, 14 ảnh chụp giao diện (đặt vào assets/screenshots/ theo tên ID).
5. Yêu cầu định dạng riêng của khoa (nếu khác thông số đo từ PDF mẫu: A4, TNR 14, giãn dòng 1,5, lề 2/2/3/2 cm).
6. Có muốn bổ sung tài liệu tham khảo giáo trình của trường không (không sao chép từ mẫu).
7. Kết quả kiểm thử thủ công trên giao diện (nếu có) để bổ sung Chương 5.

## Phát hiện từ mã nguồn nên cân nhắc trước khi bảo vệ (đã phản ánh trung thực ở phần Hạn chế)
- Sao chép lịch trình từ bài viết cộng đồng cho phép bất kỳ người dùng đăng nhập nào sao chép toàn bộ dữ liệu chuyến đi gốc (gồm ngân sách, hồ sơ đoàn) — thay đổi chưa commit, chưa có test.
- Chức năng tạo bài tổng kết bằng AI không áp dụng hạn mức AI và gọi LLM trong giao dịch chỉ đọc.
- Lưu địa điểm từ kết quả Google trên trang Khám phá nhiều khả năng lỗi (suy luận từ mã, chưa chạy); cập nhật số bình chọn trên giao diện không khớp dữ liệu trả về (suy luận).
- Với cơ sở dữ liệu trống, Docker Compose không tự khởi tạo lược đồ.

## Giới hạn của quá trình kiểm tra
- Chưa mở báo cáo bằng Microsoft Word; chưa mở file .drawio bằng giao diện draw.io (chỉ dùng CLI).
- Không chạy ứng dụng thật nên không có ảnh chụp và không kiểm thử giao diện thủ công.
- Không kiểm tra trạng thái CI trên GitHub.
