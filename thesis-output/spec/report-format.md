# Phân tích định dạng PDF mẫu (TÀI LIỆU NỘI BỘ — không ghép vào báo cáo)

Nguồn: PDF mẫu đồ án tốt nghiệp ngành Kỹ thuật phần mềm (79 trang, xuất từ Microsoft Word 365).
Phương pháp: xem trực tiếp ảnh các trang 1–30, 42–56, 64–79; đo bằng `pdfinfo`, `pdffonts`, `pdftotext -bbox`.

## 1. Thông số trang (VERIFIED bằng đo đạc)
| Thuộc tính | Giá trị đo được | Ghi chú |
|---|---|---|
| Khổ giấy | A4 (595 × 842 pt) | pdfinfo |
| Font | Times New Roman (thường/đậm/nghiêng) | pdffonts; Arial chỉ xuất hiện trong ảnh sơ đồ |
| Cỡ chữ thân bài | ~14 pt | chiều cao bbox chữ ≈ 15,5 pt |
| Giãn dòng | ~24 pt/dòng → 1,5 lines | đo khoảng cách dòng liên tiếp |
| Lề trái | ~85 pt ≈ 3,0 cm | x nhỏ nhất của khối chữ |
| Lề phải | ~56 pt ≈ 2,0 cm | x lớn nhất 538,8 pt |
| Lề trên | ≈ 2 cm (dòng đầu y≈68 pt, số trang y≈36 pt trong header) | INFERRED |
| Lề dưới | ≈ 2 cm | INFERRED |
| Số trang | Giữa, phía trên (header). Phần đầu đánh số La Mã thường (i, ii, …); từ MỞ ĐẦU đánh 1, 2, … | VERIFIED |
| Thụt đầu dòng | ~1,27 cm cho đoạn văn | quan sát |
| Căn lề | Justify | quan sát |

## 2. Cấu trúc tổng thể (VERIFIED — quan sát trực tiếp)
1. Bìa ngoài: khung viền đơn, cột trái dọc chứa tên SV và ngành (chữ xoay dọc); khối phải: BỘ CÔNG THƯƠNG / ĐẠI HỌC CÔNG NGHIỆP HÀ NỘI / gạch / ĐỒ ÁN TỐT NGHIỆP / NGÀNH … / ĐỀ TÀI / GVHD – Sinh viên – Mã SV / Hà Nội – Năm.
2. Bìa trong: khung viền đôi xanh đậm, logo trường, "ĐỒ ÁN TỐT NGHIỆP ĐẠI HỌC", tên đề tài chữ in hoa màu xanh, GVHD/SV/MSV/Lớp.
3. LỜI NÓI ĐẦU (trang i).
4. MỤC LỤC (ii–iv) — 3 cấp, dot leader, số trang phải.
5. DANH MỤC CÁC THUẬT NGỮ, KÝ HIỆU VÀ CÁC CHỮ VIẾT TẮT — bảng 3 cột: STT | Từ viết tắt | Giải nghĩa.
6. DANH MỤC HÌNH ẢNH, DANH MỤC BẢNG BIỂU — dạng mục lục có dot leader.
7. MỞ ĐẦU: 1. Lý do chọn đề tài, 2. Mục tiêu, 3. Bố cục chính, 4. Phương pháp thực hiện.
8. CHƯƠNG 1. CƠ SỞ LÝ THUYẾT (tổng quan đề tài + công nghệ sử dụng).
9. CHƯƠNG 2. PHÂN TÍCH VÀ ĐẶC TẢ YÊU CẦU … (mô tả bài toán, yêu cầu chức năng theo tác nhân, phi chức năng, use case tổng quan, đặc tả từng use case bằng bảng + use case phân rã).
10. CHƯƠNG 3. THIẾT KẾ … (kiến trúc; với mỗi chức năng: biểu đồ hoạt động → tuần tự → lớp; thiết kế CSDL (ERD); thiết kế giao diện (wireframe)).
11. CHƯƠNG 4. CÀI ĐẶT … (công nghệ Backend dạng bảng, mô hình kiến trúc Backend; công nghệ Frontend, mô hình kiến trúc Frontend; ảnh giao diện thực tế).
12. CHƯƠNG 5. KIỂM THỬ PHẦN MỀM (mục tiêu, phạm vi, bảng test case từng chức năng).
13. KẾT LUẬN, TÀI LIỆU THAM KHẢO.

## 3. Cấp tiêu đề và đánh số
| Cấp | Ví dụ | Định dạng |
|---|---|---|
| Chương | CHƯƠNG 2. PHÂN TÍCH … | IN HOA, đậm, căn giữa, ~16 pt, mỗi chương sang trang mới |
| Mục cấp 2 | 2.1. Mô tả bài toán | đậm, căn trái |
| Mục cấp 3 | 2.2.1. Yêu cầu chức năng | đậm |
| Mục cấp 4 | 2.3.2.1. Mô tả chức năng "…" | đậm + nghiêng |
| Phần không đánh số | MỞ ĐẦU, KẾT LUẬN, TÀI LIỆU THAM KHẢO | IN HOA, đậm, căn giữa |

## 4. Hình và bảng
- Chú thích hình: dưới hình, căn giữa, nghiêng, dạng "Hình C.N. Nội dung" (đánh số theo chương).
- Chú thích bảng: TRÊN bảng, căn giữa, nghiêng, dạng "Bảng C.N. Nội dung".
- Bảng đặc tả use case: 2 cột chính (nhãn | nội dung); "Luồng sự kiện chính/thay thế" là bảng lồng 3 cột (# | Thực hiện bởi | Hành động) với dòng nhóm nền xanh nhạt (#B4CCE8-ish) căn giữa.
- Bảng test case: hàng ID / Mô tả / Nhánh, sau đó 5 cột STT | Kịch bản | Dữ liệu vào | Đầu ra kỳ vọng | Đầu ra thực tế.
- Trong chương 3, trước mỗi hình có gạch đầu dòng đậm "• Biểu đồ hoạt động:", "• Biểu đồ tuần tự:", "• Biểu đồ lớp:".
- Mẫu có lỗi nhỏ (Hình 3.11 đặt nhầm tên "biểu đồ lớp" cho biểu đồ hoạt động; trùng số 3.26) → không sao chép lỗi.

## 5. Quyết định áp dụng cho báo cáo mới
- A4; TNR 14; giãn dòng 1,5; lề T2 / D2 / Tr3 / P2 cm; thụt đầu dòng 1,27 cm; justify; spacing after 6 pt.
- Số trang giữa header: La Mã cho phần đầu, Ả Rập từ MỞ ĐẦU (2 section).
- MỤC LỤC, DANH MỤC HÌNH, DANH MỤC BẢNG bằng trường Word (TOC field) — cập nhật bằng LibreOffice headless khi build; ghi rõ nếu chưa cập nhật.
- Bìa: tái hiện bố cục bìa ngoài + bìa trong, KHÔNG chèn logo trường (người dùng tự bổ sung — placeholder).
- Cấu trúc 5 chương + Mở đầu + Kết luận giống mẫu; nội dung theo dự án TripMind.
