# Quy chuẩn phong cách sơ đồ (TÀI LIỆU NỘI BỘ)

Đối chiếu trực tiếp (xem ảnh trang) với PDF mẫu: trang 21–22 (UC tổng quan, nền xanh), 26/29 (UC phân rã nền vàng), 43–56 (activity, sequence, class), 64 (ERD), 65–67 (wireframe), 69–70 (kiến trúc BE/FE).
Toàn bộ sơ đồ được sinh bởi thư viện `scripts/drawio_lib.py` → XML draw.io gốc (mỗi khối/nhãn/đường là một cell riêng, không nhúng bitmap) → xuất PNG bằng draw.io desktop CLI (v32.4.1, scale 2).
Pilot đã xuất và so sánh trực quan: OK (scratchpad pilot/all.png).

| Loại | Quan sát trong mẫu | Quy chuẩn áp dụng |
|---|---|---|
| Use case | Actor người que; elip vàng nhạt viền đen (UC phân rã), mũi tên tam giác rỗng cho tổng quát hóa; `<<Include>>`/`<<Extend>>` nét đứt mũi tên mở. UC tổng quan trong mẫu dùng elip xanh (khác phong cách) | Thống nhất elip vàng `#FFF2CC`, viền đen 1px, Arial 12. Thêm khung ranh giới hệ thống (mẫu không có — cải tiến nhỏ để rõ phạm vi). Không vẽ actor "Hệ thống" như mẫu (không đúng ký pháp UML) |
| Activity | 2–3 swimlane (Người dùng / Hệ thống / …), header trắng; action bo góc vàng; hình thoi vàng; nút đầu tròn đen, nút cuối bullseye; nhãn Yes/No | Giữ nguyên; nhãn nhánh tiếng Việt ("Có/Không" hoặc điều kiện), thanh fork/join đen khi gộp nhánh |
| Sequence | Actor + lifeline thanh đôi vàng; boundary/control/entity dạng icon tròn vàng; lifeline nét đứt; activation nhỏ vàng; mũi tên gọi đặc, trả về nét đứt mũi mở; khung `alt` có guard `[...]`; ghi chú dạng note vàng bên trái | Giữ nguyên (shape umlBoundary/umlControl/umlEntity). Message đánh số "n: tênThôngĐiệp()" tiếng Việt có dấu. Chia nhỏ để không quá ~30 message/hình |
| Class | Hộp vàng 3 ngăn, stereotype `<<boundary>>`, `<<control>>`, `<<entity>>`; liên kết có nhãn và bội số 1 / * | Giữ nguyên; chỉ thuộc tính/phương thức tiêu biểu (không liệt kê getter/setter như mẫu) |
| ERD | Bảng tiêu đề nền xanh dương chữ trắng, thân trắng, dòng PK nền xanh nhạt, biểu tượng khóa; quan hệ crow's-foot màu xanh | Giữ nguyên, ghi "PK"/"FK" thay biểu tượng khóa |
| Kiến trúc / khối | Khối chữ nhật trắng viền đen, khung nét đứt nhóm thành phần, mũi tên có nhãn, nền lưới nhẹ | Giữ nguyên, không dùng màu hiện đại |
| Wireframe | Nền lưới ô vuông xám nhạt; khung màn hình viền đen; ảnh = hộp có chữ X; văn bản = các vạch ngang; nút bo tròn; mũi tên cong nối màn hình | Giữ nguyên (shape mxgraph.mockup.graphics.simpleIcon cho ảnh) |
| Deployment (Docker) | Mẫu không có hình tương ứng | Dùng ký pháp khối như sơ đồ kiến trúc; ghi nhận "không có mẫu trực tiếp" |

Font: Arial (giống ảnh sơ đồ trong mẫu). Cỡ chữ tối thiểu 10 px trên canvas; xuất scale 2 để rõ khi chèn Word.
