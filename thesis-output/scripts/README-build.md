# Hướng dẫn dựng báo cáo (NỘI BỘ)

## Chạy
```
DRAWIO=/đường/dẫn/drawio scripts/build.sh
```
- Cần: Python 3 (python-docx, Pillow), draw.io desktop (CLI xuất PNG), LibreOffice (soffice, mô-đun python `uno`).
- Kết quả: `report/final-report.docx` (đã cập nhật Mục lục, Danh mục hình, Danh mục bảng qua LibreOffice), `report/final-report.pdf` để xem thử.
- Khi mở bằng Word, Word sẽ hỏi cập nhật trường (updateFields=true) → chọn Yes để số trang khớp bố cục Word.

## Nguồn
- `chapters/meta.json`: thông tin bìa (điền họ tên, MSV, GVHD, lớp).
- `chapters/00-front-matter.md`: Lời nói đầu (`@section LỜI NÓI ĐẦU`), Danh mục viết tắt (`@section DANH MỤC VIẾT TẮT`).
- `chapters/01…08-*.md`: nội dung từng chương, ghép theo thứ tự.

## Cú pháp mở rộng trong chương
| Cú pháp | Ý nghĩa |
|---|---|
| `# CHƯƠNG n. …`, `##`, `###`, `####` | Tiêu đề cấp 1–4 (cấp 1 sang trang mới; số hình/bảng đánh lại theo chương) |
| `@fig{ID}{Chú thích}` | Chèn hình `diagrams/export/ID.png`, `ui-design/export/ID.png` hoặc `assets/screenshots/ID.png`; không có ảnh → placeholder |
| `@table[KEY]{Chú thích}{w1,w2,…}` + bảng `|…|` | Bảng có chú thích, độ rộng cột (cm) tùy chọn |
| `@usecase[KEY]{Chú thích}` … `@end` | Bảng đặc tả use case (name, code, actors, desc, trigger, pre, main, alt, post; `== nhóm`, `# | ai | hành động`) |
| `@testcase[KEY]{Chú thích}` … `@end` | Bảng kiểm thử (id, desc, branch, rows) |
| `{fig:ID}`, `{tab:KEY}` | Tham chiếu số hình/bảng (tự đánh số theo thứ tự xuất hiện) |
| `- `, `  - `, `**đậm**`, `*nghiêng*`, `> căn giữa nghiêng` | Gạch đầu dòng, định dạng |

Mã quản lý (DGM-…, SHOT-…) chỉ dùng trong nguồn; báo cáo chỉ hiện "Hình x.y".

## Sơ đồ
- Nguồn sinh: `diagrams/gen_*.py` (thư viện `scripts/drawio_lib.py`), `ui-design/build_wireframes.py`.
- File chỉnh sửa được: `diagrams/src/*.drawio`, `ui-design/src/*.drawio`. Lưu ý: nếu sửa tay trong draw.io, chạy lại các script `gen_*` sẽ ghi đè — hãy sửa trong script, hoặc bỏ module đó khỏi `diagrams/build_all.py`.
