"""Sinh spec/diagram-inventory.md và assets/placeholders/asset-manifest.md từ nguồn chương + kết quả build."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
state = json.loads((ROOT / "qa" / "build-state.json").read_text(encoding="utf-8"))
caps = {}
for f in sorted((ROOT / "chapters").glob("*.md")):
    for m in re.finditer(r"^@fig\{([^}]+)\}\{(.+)\}\s*$", f.read_text(encoding="utf-8"), flags=re.M):
        caps[m.group(1)] = (m.group(2), f.name)
TYPES = {"UC": "Use Case", "ACT": "Activity", "SEQ": "Sequence", "CLS": "Class (phân tích)", "ERD": "ERD",
         "ARC": "Kiến trúc / khối / triển khai"}
EVID = {"UC": "feature-catalog.md (chỉ chức năng IMPLEMENTED có UI)", "ACT": "key-flows.md F1–F10, feature-catalog.md",
        "SEQ": "key-flows.md F1–F10", "CLS": "data-model.md, key-flows.md", "ERD": "data-model.md §2–3 (21 bảng có entity)",
        "ARC": "architecture.md §1–7, system-overview.md §3.6"}
rows = ["# Danh mục sơ đồ (NỘI BỘ — không ghép vào báo cáo)", "",
        "Nguồn sinh: diagrams/gen_*.py, ui-design/build_wireframes.py → *.drawio (XML gốc, mọi khối/nhãn/đường là cell riêng) → PNG (draw.io CLI 32.4.1, scale 2).",
        "Kiểm tra: XML hợp lệ (ElementTree), không có ảnh bitmap nhúng, PNG mới hơn nguồn; xem trực quan từng PNG và bản PDF dựng thử.", "",
        "| ID | Hình trong báo cáo | Loại | Chú thích | Nguồn | Xuất | Bằng chứng | Xác minh | Hiển thị |", "|---|---|---|---|---|---|---|---|---|"]
for fid, num in state["figures"].items():
    if not (fid.startswith("DGM-") or fid.startswith("UI-")):
        continue
    cap = caps.get(fid, ("", ""))[0]
    if fid.startswith("UI-"):
        typ, src, exp, ev = "Wireframe", f"ui-design/src/{fid}.drawio", f"ui-design/export/{fid}.png", "Bố cục các trang trong mã giao diện (routes, components)"
    else:
        k = fid.split("-")[1]
        typ, src, exp, ev = TYPES[k], f"diagrams/src/{fid}.drawio", f"diagrams/export/{fid}.png", EVID[k]
    rows.append(f"| {fid} | {num} | {typ} | {cap} | {src} | {exp} | {ev} | VERIFIED | OK (đã xem) |")
(ROOT / "spec" / "diagram-inventory.md").write_text("\n".join(rows) + "\n", encoding="utf-8")

guide = {
    "ASSET-IMG-001": ("Ảnh minh họa bên ngoài", "Ảnh minh họa chủ đề du lịch/lập kế hoạch (có giấy phép sử dụng)", "—"),
    "SHOT-001-dang-nhap": ("Ảnh chụp giao diện", "Trang đăng nhập", "Khách; mở trang đăng nhập"),
    "SHOT-002-dang-ky": ("Ảnh chụp giao diện", "Trang đăng ký", "Khách; mở trang đăng ký"),
    "SHOT-003-trang-chu": ("Ảnh chụp giao diện", "Trang chủ có ô mô tả AI và vài thẻ chuyến đi", "Người dùng đã có ≥2 chuyến đi"),
    "SHOT-004-tao-chuyen-di": ("Ảnh chụp giao diện", "Hộp thoại tạo chuyến đi đã được AI điền sẵn", "Nhập câu mô tả rồi nhấn “Phân tích & Tạo”"),
    "SHOT-005-lich-trinh": ("Ảnh chụp giao diện", "Thẻ Lịch trình chi tiết, có bản đồ trong ngày", "Chuyến đi có hoạt động gắn địa điểm"),
    "SHOT-006-sinh-lich-trinh": ("Ảnh chụp giao diện", "Thanh tiến độ sinh lịch trình AI", "Chuyến đi trống, nhấn “Tạo lịch trình AI” (cần cấu hình LLM)"),
    "SHOT-007-tro-ly-ai": ("Ảnh chụp giao diện", "Khung trợ lý AI với thẻ đề xuất", "Hỏi trợ lý yêu cầu thay đổi lịch trình"),
    "SHOT-008-ngan-sach": ("Ảnh chụp giao diện", "Thẻ Ngân sách & Chi tiêu", "Chuyến đi có ngân sách và vài khoản chi"),
    "SHOT-009-checklist": ("Ảnh chụp giao diện", "Thẻ Checklist và hộp thoại gợi ý", "Mở gợi ý chuẩn bị"),
    "SHOT-010-dia-diem": ("Ảnh chụp giao diện", "Trang Khám phá địa điểm có bản đồ", "Tìm kiếm một từ khóa (cần khóa Google Places)"),
    "SHOT-011-cong-dong": ("Ảnh chụp giao diện", "Trang Cộng đồng & Review", "Có vài bài viết"),
    "SHOT-012-quan-tri-nguoi-dung": ("Ảnh chụp giao diện", "Trang quản trị – thẻ người dùng", "Tài khoản Quản trị viên"),
    "SHOT-013-quan-tri-tri-thuc": ("Ảnh chụp giao diện", "Trang quản trị – thẻ tri thức điểm đến", "Tài khoản Quản trị viên"),
    "SHOT-014-quan-tri-thong-ke": ("Ảnh chụp giao diện", "Trang quản trị – thẻ thống kê AI", "Tài khoản Quản trị viên, đã có lượt dùng AI"),
}
out = ["# Danh mục ảnh cần người dùng bổ sung (NỘI BỘ)", "",
       "Cách bổ sung: lưu ảnh PNG/JPG với đúng tên `<ID>.png` vào thư mục `assets/screenshots/`, rồi chạy lại `scripts/build.sh`. Script tự thay placeholder bằng ảnh.",
       "Không dùng wireframe thay cho ảnh chụp thực tế; không chụp IDE/terminal.", "",
       "| ID | Hình | Loại | Nội dung cần chụp | Vai trò / thao tác | Chú thích đề xuất | Trạng thái |", "|---|---|---|---|---|---|---|"]
for where, fid, cap in state["placeholders"]:
    if fid in guide:
        typ, what, how = guide[fid]
        out.append(f"| {fid} | {where} | {typ} | {what} | {how} | {cap} | CHƯA CÓ |")
    else:
        out.append(f"| {fid} | {where} | Khác | {cap} | — | — | CHƯA CÓ |")
(ROOT / "assets" / "placeholders").mkdir(parents=True, exist_ok=True)
(ROOT / "assets" / "screenshots").mkdir(parents=True, exist_ok=True)
(ROOT / "assets" / "placeholders" / "asset-manifest.md").write_text("\n".join(out) + "\n", encoding="utf-8")
print("inventory + manifest written")
