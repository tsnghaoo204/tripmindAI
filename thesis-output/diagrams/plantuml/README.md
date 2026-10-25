# Hướng dẫn sử dụng và chỉnh sửa biểu đồ bằng Visual Paradigm / PlantUML

Thư mục này chứa toàn bộ mã nguồn biểu đồ UML dạng **PlantUML (`.puml`)** để bạn có thể mở, chỉnh sửa trực tiếp bằng các công cụ chuyên dụng.

---

## 1. Mở và chỉnh sửa bằng Visual Paradigm

**Visual Paradigm** hỗ trợ import trực tiếp mã PlantUML để chuyển thành biểu đồ UML tương tác (kéo thả, đổi style, đổi font, đổi màu):

1. Mở phần mềm **Visual Paradigm**.
2. Trên thanh công cụ, chọn: **Tools** > **Code** > **PlantUML** > **Import PlantUML...**
3. Chọn tệp `.puml` tương ứng trong thư mục này (ví dụ: `DGM-UC-001-tong-quan.puml`).
4. Visual Paradigm sẽ tự động tạo sơ đồ Use Case với đầy đủ Actor, Use Case elip và các đường liên kết Association/Generalization. Bạn có thể kéo thả căn chỉnh theo ý muốn, sau đó xuất ra ảnh hoặc lưu vào project.

---

## 2. Mở và chỉnh sửa bằng Visual Studio Code / Antigravity

1. Cài đặt tiện ích mở rộng **PlantUML** (của Jebbs) trong VS Code.
2. Mở bất kỳ tệp `.puml` nào trong thư mục này.
3. Nhấn tổ hợp phím **`Alt + D`** để xem trước (Preview) trực tiếp sơ đồ song song với code.
4. Nhấn chuột phải vào cửa sổ xem trước để xuất (Export) sang **PNG**, **SVG** hoặc **PDF**.

---

## 3. Chỉnh sửa online không cần cài đặt

Nếu máy chưa cài phần mềm, bạn có thể copy nội dung tệp `.puml` và dán vào các trang web miễn phí:
- [PlantText UML Editor](https://www.planttext.com/)
- [PlantUML Official Web Server](http://www.plantuml.com/plantuml/uml/)

---

## 4. Chỉnh sửa bằng Draw.io

Ngoài PlantUML, mã nguồn kéo thả Draw.io XML gốc cũng có sẵn tại thư mục:
- `thesis-output/diagrams/src/` (các file đuôi `.drawio`).
- Mở trực tiếp bằng ứng dụng web [app.diagrams.net](https://app.diagrams.net/) hoặc Draw.io Desktop.
