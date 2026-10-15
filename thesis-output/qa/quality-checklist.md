# Danh sách kiểm tra chất lượng (NỘI BỘ)

Ngày kiểm tra: 15/10/2026. Bản kiểm tra: report/final-report.docx (98 trang khi xuất PDF bằng LibreOffice 7.3).

## 1. Tính chính xác nội dung
| Hạng mục | Trạng thái | Bằng chứng / ghi chú |
|---|---|---|
| Chức năng mô tả có trong mã nguồn | ĐÃ KIỂM | Chỉ dùng các mục IMPLEMENTED trong spec/feature-catalog.md; BACKEND-ONLY/PARTIAL/PROPOSED chỉ nêu ở phần Hạn chế |
| Công nghệ, phiên bản | ĐÃ KIỂM | spec/system-overview.md §3 (pom.xml, package.json, docker-compose) |
| Quy tắc nghiệp vụ (30 ngày, 120 req/phút, 20 lượt AI/giờ, 5 lần sai/5 phút, 8 vòng/60 giây, đề xuất 30 phút, hoàn tác 10 phút, ngưỡng 90%) | ĐÃ KIỂM | application.yml, các service tương ứng (xem spec/feature-catalog.md, architecture.md) |
| Sơ đồ tuần tự/hoạt động khớp luồng thực | ĐÃ KIỂM | spec/key-flows.md F1–F10; tên đối tượng là lớp phân tích, không phải tên lớp thật |
| ERD khớp lược đồ | ĐÃ KIỂM | spec/data-model.md; chỉ 21 bảng có entity; 3 bảng chỉ có trong lược đồ không được vẽ; vẽ cột tiêu biểu, không đầy đủ |
| Kết quả kiểm thử | ĐÃ KIỂM | spec/test-evidence.md: chạy thật ngày 15/10/2026 (116/116 backend, 18/18 frontend, tsc sạch). Bảng test case chỉ gồm kịch bản có test tự động tương ứng |
| Không sao chép nội dung dự án mẫu | ĐÃ KIỂM | Chỉ học cấu trúc/phong cách; tài liệu tham khảo không lấy từ mẫu |
| Nhà cung cấp LLM | CẦN XÁC NHẬN | Mã nguồn trung lập (chuẩn OpenAI); báo cáo ghi “dịch vụ LLM tương thích chuẩn OpenAI”, không khẳng định Gemini hay GPT |
| Tên đề tài | CẦN XÁC NHẬN | Suy ra từ tài liệu thiết kế; xem chapters/meta.json |

## 2. Kiểm tra cấm mã nguồn / tên tệp / đường dẫn
| Hạng mục | Trạng thái | Ghi chú |
|---|---|---|
| Quét tự động (scripts/check_report.py) | ĐÃ KIỂM | 27 phát hiện = 25 placeholder đã đánh dấu + 2 dương tính giả (“Node.js”, ngày “15/10/2026”) |
| Không có khối code, pseudocode, SQL, lệnh shell | ĐÃ KIỂM | Quét + đọc theo ngữ cảnh các chương |
| Không có tên tệp, đường dẫn, số dòng, tên lớp/phương thức thật | ĐÃ KIỂM | Tên lớp trong sơ đồ lớp/tuần tự là lớp phân tích tiếng Việt (GiaoDienXacThuc, DieuKhienXacThuc…), không trùng tên lớp mã nguồn |
| Tên bảng CSDL xuất hiện trong ERD và bảng 3.2 | CHẤP NHẬN | Tương tự mẫu (ERD dùng tên bảng); không chép DDL nguyên văn |
| Tên công cụ AI nội bộ | ĐÃ KIỂM | Bảng 4.4 mô tả chức năng công cụ bằng tiếng Việt, không dùng định danh trong mã |
| Không có ảnh IDE/terminal | ĐÃ KIỂM | Chỉ có sơ đồ draw.io và placeholder |

## 3. Sơ đồ
| Hạng mục | Trạng thái | Ghi chú |
|---|---|---|
| 48 file .drawio hợp lệ XML, không nhúng bitmap | ĐÃ KIỂM | ElementTree parse; không có style image= |
| PNG xuất từ đúng nguồn, mới hơn nguồn | ĐÃ KIỂM | draw.io CLI 32.4.1, scale 2 |
| Phong cách so với PDF mẫu | ĐÃ KIỂM | Xem spec/diagram-style-guide.md; pilot + xem trực quan từng nhóm |
| Mở được trong ứng dụng draw.io có giao diện | CHƯA KIỂM | Chỉ kiểm bằng CLI xuất ảnh (cùng engine); chưa mở thủ công |
| Chữ dễ đọc khi in | MỘT PHẦN | Biểu đồ tuần tự/hoạt động cao gần trọn trang, chữ ~5–6 pt khi in A4 (tương đương mẫu). Có thể in khổ ngang hoặc tách nhỏ nếu hội đồng yêu cầu |
| Đường nối chồng chéo | MỘT PHẦN | Đã chỉnh hết các đường xuyên khối; còn vài điểm giao cắt nhỏ giữa đường (ERD, kiến trúc Frontend) |

## 4. Trình bày
| Hạng mục | Trạng thái | Ghi chú |
|---|---|---|
| Thứ tự chương, tiêu đề thống nhất | ĐÃ KIỂM | Xem bản PDF |
| Đánh số hình/bảng theo chương, chú thích | ĐÃ KIỂM | 63 hình, 23 bảng; 0 tham chiếu hỏng |
| Mục lục, danh mục hình, danh mục bảng | ĐÃ KIỂM (LibreOffice) | Đã cập nhật bằng LibreOffice; Word sẽ hỏi cập nhật trường khi mở — chọn Yes để số trang khớp bố cục Word |
| Số trang La Mã (phần đầu) / Ả Rập (thân bài) | ĐÃ KIỂM | Trên bản PDF LibreOffice |
| Viền trang chỉ ở bìa trong | ĐÃ KIỂM | Đã sửa lỗi viền bị kế thừa sang thân bài |
| Trang trắng thừa | MỘT PHẦN | Một số trang còn khoảng trống lớn do hình cao không vừa phần còn lại của trang (giữ hình đi liền chú thích) |
| Hiển thị trong Microsoft Word | CHƯA KIỂM | Môi trường không có Word; chỉ kiểm bằng LibreOffice. Font Times New Roman được thay bằng Liberation Serif (cùng số đo) khi render |
| Placeholder | CÒN 16 ảnh + 4 trường bìa + logo | Xem assets/placeholders/asset-manifest.md |
