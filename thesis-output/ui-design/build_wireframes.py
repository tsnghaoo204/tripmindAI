"""Sinh wireframe 'Hình dung giao diện' (draw.io) theo phong cách PDF mẫu."""
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "scripts"))
from drawio_lib import (Doc, wf_btn, wf_flow, wf_grid, wf_img, wf_input, wf_label, wf_lines, wf_rect,  # noqa: E402
                        wf_screen)

SRC = HERE / "src"
SRC.mkdir(exist_ok=True)


def header(d, x, y, w, admin=False):
    wf_img(d, x + 10, y + 8, 28, 22)
    wf_label(d, "TripMind", x + 42, y + 11, 60, 16, size=10, bold=True)
    items = ["Chuyến đi", "Địa điểm", "Cộng đồng"] + (["Quản trị"] if admin else [])
    bx = x + 110
    for it in items:
        wf_btn(d, bx, y + 10, 62, 18, it, size=8)
        bx += 66
    wf_btn(d, x + w - 62, y + 9, 22, 20, "☾", size=8)
    wf_btn(d, x + w - 36, y + 9, 22, 20, "A", size=8)
    d.line(x, y + 38, x + w, y + 38)


def screen_with_header(d, x, y, w, h, admin=False):
    s = wf_screen(d, x, y, w, h)
    header(d, x, y, w, admin)
    return s


def auth():
    d = Doc("wf-auth")
    wf_grid(d, 0, 0, 1000, 420)
    s1 = wf_screen(d, 20, 20, 460, 380)
    wf_img(d, 30, 30, 200, 360)
    wf_rect(d, 250, 60, 210, 300)
    wf_label(d, "Đăng nhập tài khoản", 262, 70, 190, 18, size=12, bold=True)
    wf_lines(d, 262, 98, 180, 2, 8)
    wf_input(d, 262, 140, 186, "Email")
    wf_input(d, 262, 185, 186, "Mật khẩu")
    b1 = wf_btn(d, 262, 222, 186, 24, "Đăng nhập", pill=False)
    lk1 = wf_label(d, "Chưa có tài khoản? Đăng ký", 262, 258, 186, 16, size=9, align="center")
    s2 = wf_screen(d, 520, 20, 460, 380)
    wf_img(d, 530, 30, 200, 360)
    wf_rect(d, 750, 40, 210, 340)
    wf_label(d, "Tạo tài khoản", 762, 50, 190, 18, size=12, bold=True)
    wf_input(d, 762, 98, 186, "Họ và tên")
    wf_input(d, 762, 140, 186, "Email")
    wf_input(d, 762, 182, 186, "Mật khẩu")
    wf_btn(d, 762, 226, 186, 24, "Đăng ký", pill=False)
    lk2 = wf_label(d, "Đã có tài khoản? Đăng nhập", 762, 262, 186, 16, size=9, align="center")
    wf_flow(d, lk1, s2, points=[(500, 300)], entry=(0, 0.7))
    wf_flow(d, lk2, s1, points=[(855, 395), (500, 405)], entry=(1, 0.95))
    d.save(SRC / "UI-001-auth.drawio")


def dashboard():
    d = Doc("wf-dashboard")
    wf_grid(d, 0, 0, 1040, 520)
    s1 = screen_with_header(d, 20, 20, 520, 480)
    wf_label(d, "Kế hoạch Du lịch", 34, 66, 200, 18, size=12, bold=True)
    wf_lines(d, 34, 92, 220, 1)
    wf_btn(d, 410, 66, 115, 22, "+ Tạo chuyến đi mới", size=8, pill=False)
    wf_rect(d, 34, 110, 492, 92)
    wf_label(d, "Lập kế hoạch bằng AI: nhập mô tả tự nhiên", 44, 116, 300, 16, size=9, bold=True)
    inp = wf_input(d, 44, 142, 360, h=22)
    go = wf_btn(d, 412, 142, 104, 22, "Phân tích & Tạo", size=8, pill=False)
    for k in range(4):
        wf_btn(d, 44 + k * 92, 174, 86, 18, f"Gợi ý {k + 1}", size=8)
    wf_label(d, "Sắp tới & Đang đi", 34, 214, 110, 16, size=9, bold=True)
    wf_label(d, "Đã hoàn thành", 150, 214, 100, 16, size=9)
    d.line(34, 234, 526, 234)
    for k in range(3):
        x = 34 + k * 166
        wf_rect(d, x, 246, 158, 150)
        wf_img(d, x + 6, 252, 146, 56)
        wf_lines(d, x + 8, 322, 140, 3, 12)
        wf_rect(d, x + 8, 370, 140, 8, rounded=True, fill="#FFFFFF")
    wf_label(d, "Thẻ chuyến đi: điểm đến, giai đoạn, ngày đi, số người, tiến độ lập lịch", 34, 404, 480, 16, size=9)
    s2 = wf_screen(d, 580, 20, 440, 480)
    wf_label(d, "Lập Kế Hoạch Chuyến Đi Mới", 594, 32, 260, 18, size=12, bold=True)
    for k, t in enumerate(["1. Cơ bản", "2. Đoàn đi", "3. Sở thích"]):
        wf_btn(d, 594 + k * 105, 60, 98, 20, t, size=8)
    wf_input(d, 594, 110, 410, "Mô tả nhanh bằng ngôn ngữ tự nhiên (AI tự điền form)", h=26)
    wf_input(d, 594, 168, 200, "Xuất phát từ")
    wf_input(d, 804, 168, 200, "Điểm đến *")
    wf_input(d, 594, 212, 200, "Ngày đi *")
    wf_input(d, 804, 212, 200, "Ngày về *")
    wf_input(d, 594, 256, 200, "Phương tiện đến/về")
    wf_input(d, 804, 256, 200, "Đi lại tại điểm đến")
    wf_input(d, 594, 300, 200, "Tổng số người *")
    wf_input(d, 804, 300, 200, "Ngân sách")
    wf_input(d, 594, 344, 410, "Sở thích / Nhịp độ / Dị ứng")
    wf_btn(d, 800, 440, 96, 24, "Hủy", pill=False)
    wf_btn(d, 908, 440, 96, 24, "Tạo chuyến đi", pill=False)
    wf_flow(d, go, s2, points=[(560, 160)], entry=(0, 0.3))
    d.save(SRC / "UI-002-dashboard.drawio")


def trip_detail():
    d = Doc("wf-trip")
    wf_grid(d, 0, 0, 1060, 560)
    s1 = screen_with_header(d, 20, 20, 640, 520)
    wf_rect(d, 34, 70, 612, 88)
    wf_img(d, 42, 78, 110, 72)
    wf_label(d, "Tên chuyến đi · Điểm đến", 162, 80, 260, 18, size=11, bold=True)
    wf_lines(d, 162, 106, 200, 2, 10)
    for k, t in enumerate(["Tạo lịch trình AI", "Giai đoạn", "Nhân bản", "Review AI", "Xóa"]):
        wf_btn(d, 162 + k * 94, 130, 88, 20, t, size=8, pill=False)
    for k, t in enumerate(["Lịch trình chi tiết", "Ngân sách & Chi tiêu", "Checklist chuẩn bị"]):
        wf_btn(d, 34 + k * 140, 170, 132, 22, t, size=8, fill="#DAE8FC" if k == 0 else "#FFFFFF")
    for k in range(4):
        wf_btn(d, 34 + k * 64, 204, 58, 20, f"Ngày {k + 1}", size=8)
    for k in range(3):
        y = 236 + k * 74
        wf_label(d, f"{8 + k * 3}:00", 34, y + 8, 40, 16, size=9)
        wf_rect(d, 78, y, 300, 64)
        wf_img(d, 86, y + 8, 60, 48)
        wf_lines(d, 154, y + 14, 200, 3, 12)
    wf_rect(d, 390, 236, 256, 212)
    wf_label(d, "Bản đồ hoạt động trong ngày", 400, 240, 230, 16, size=9, align="center")
    wf_img(d, 398, 258, 240, 182)
    add = wf_btn(d, 78, 464, 140, 22, "+ Thêm hoạt động", size=8, pill=False)
    ai = wf_btn(d, 560, 502, 86, 26, "Trợ lý AI", size=9)
    s2 = wf_screen(d, 700, 20, 340, 300)
    wf_label(d, "Trợ lý AI TripMind", 712, 30, 200, 18, size=11, bold=True)
    d.line(700, 54, 1040, 54)
    wf_rect(d, 712, 64, 200, 40)
    wf_lines(d, 720, 76, 180, 2, 10)
    wf_rect(d, 820, 114, 208, 30)
    wf_lines(d, 828, 124, 190, 1)
    wf_rect(d, 712, 154, 316, 110)
    wf_label(d, "Thẻ đề xuất thay đổi (proposal)", 720, 158, 220, 16, size=9, bold=True)
    wf_lines(d, 720, 184, 290, 4, 12)
    wf_btn(d, 860, 236, 76, 20, "Áp dụng", size=8, pill=False)
    wf_btn(d, 944, 236, 76, 20, "Từ chối", size=8, pill=False)
    wf_input(d, 712, 280, 250, h=24)
    wf_btn(d, 970, 280, 58, 24, "Gửi", size=8, pill=False)
    s3 = wf_screen(d, 700, 340, 340, 200)
    wf_label(d, "Thêm hoạt động", 712, 350, 200, 18, size=11, bold=True)
    wf_input(d, 712, 392, 316, "Tìm địa điểm")
    wf_input(d, 712, 436, 150, "Giờ bắt đầu")
    wf_input(d, 878, 436, 150, "Thời lượng / Chi phí")
    wf_btn(d, 952, 500, 76, 22, "Lưu", size=8, pill=False)
    wf_flow(d, ai, s2, points=[(682, 515), (682, 300)], exit=(1, 0.5), entry=(0, 0.9))
    wf_flow(d, add, s3, points=[(400, 520)], entry=(0, 0.5))
    d.save(SRC / "UI-003-trip-detail.drawio")


def budget_checklist():
    d = Doc("wf-budget")
    wf_grid(d, 0, 0, 1060, 420)
    screen_with_header(d, 20, 20, 500, 380)
    for k, t in enumerate(["Tổng ngân sách", "Ước tính", "Đã chi", "Còn lại"]):
        wf_rect(d, 34 + k * 120, 56, 112, 56)
        wf_label(d, t, 40 + k * 120, 60, 100, 14, size=8, bold=True)
        wf_lines(d, 42 + k * 120, 88, 90, 2, 9)
    wf_rect(d, 34, 124, 230, 260)
    wf_label(d, "Thêm khoản chi", 42, 128, 200, 16, size=9, bold=True)
    for k, t in enumerate(["Số tiền", "Danh mục", "Ngày chi", "Ghi chú"]):
        wf_input(d, 42, 166 + k * 44, 212, t)
    wf_btn(d, 160, 350, 94, 22, "Lưu khoản chi", size=8, pill=False)
    wf_rect(d, 274, 124, 232, 260)
    wf_label(d, "Lịch sử chi tiêu", 282, 128, 200, 16, size=9, bold=True)
    for k in range(6):
        wf_lines(d, 284, 160 + k * 36, 210, 2, 10)
    screen_with_header(d, 540, 20, 500, 380)
    wf_input(d, 554, 60, 330, h=22)
    wf_btn(d, 892, 60, 64, 22, "Thêm", size=8, pill=False)
    wf_btn(d, 962, 60, 64, 22, "Gợi ý AI", size=8, pill=False)
    for col, t in enumerate(["Đồ mang theo", "Việc cần làm"]):
        x = 554 + col * 240
        wf_rect(d, x, 94, 232, 290)
        wf_label(d, t, x + 8, 98, 200, 16, size=9, bold=True)
        for k in range(6):
            wf_rect(d, x + 10, 126 + k * 40, 12, 12, rounded=False)
            wf_lines(d, x + 30, 132 + k * 40, 180, 1)
    d.save(SRC / "UI-004-budget-checklist.drawio")


def places():
    d = Doc("wf-places")
    wf_grid(d, 0, 0, 700, 440)
    screen_with_header(d, 20, 20, 660, 400)
    wf_label(d, "Kho Dữ Liệu Điểm Đến Thông Minh", 34, 50, 300, 18, size=11, bold=True)
    wf_input(d, 34, 78, 420, h=22)
    wf_btn(d, 462, 78, 80, 22, "Tìm kiếm", size=8, pill=False)
    wf_btn(d, 550, 78, 116, 22, "Điểm đến ▾", size=8, pill=False)
    wf_btn(d, 34, 110, 80, 20, "Khám phá", size=8, fill="#DAE8FC")
    wf_btn(d, 120, 110, 80, 20, "Đã lưu", size=8)
    for k in range(4):
        y = 140 + k * 66
        wf_rect(d, 34, y, 300, 58)
        wf_img(d, 40, y + 6, 56, 46)
        wf_lines(d, 104, y + 12, 160, 3, 11)
        wf_btn(d, 272, y + 18, 54, 20, "Lưu", size=8)
    wf_rect(d, 344, 140, 322, 262)
    wf_label(d, "Bản đồ (đánh dấu các địa điểm)", 350, 144, 300, 16, size=9, align="center")
    wf_img(d, 352, 162, 306, 232)
    d.save(SRC / "UI-005-places.drawio")


def community():
    d = Doc("wf-community")
    wf_grid(d, 0, 0, 1060, 460)
    s1 = screen_with_header(d, 20, 20, 560, 420)
    wf_label(d, "Cộng Đồng Tri Thức Du Lịch", 34, 50, 300, 18, size=11, bold=True)
    wf_input(d, 34, 78, 380, h=22)
    cp = wf_btn(d, 424, 78, 140, 22, "+ Viết bài chia sẻ", size=8, pill=False)
    for k in range(2):
        y = 112 + k * 156
        wf_rect(d, 34, y, 530, 146)
        wf_rect(d, 42, y + 8, 26, 26)
        wf_lines(d, 76, y + 14, 140, 2, 10)
        wf_lines(d, 42, y + 44, 300, 3, 11)
        wf_img(d, 360, y + 40, 196, 74)
        for j, t in enumerate(["▲ Hữu ích", "Bình luận", "Sao chép lịch trình"]):
            wf_btn(d, 42 + j * 112, y + 118, 104, 20, t, size=8)
    s2 = wf_screen(d, 620, 20, 420, 420)
    wf_label(d, "Viết bài chia sẻ", 634, 30, 200, 18, size=11, bold=True)
    wf_input(d, 634, 76, 392, "Tiêu đề")
    wf_input(d, 634, 120, 190, "Điểm đến")
    wf_input(d, 836, 120, 190, "Chuyến đi liên kết (tùy chọn)")
    wf_input(d, 634, 164, 392, "Nội dung", h=120)
    wf_input(d, 634, 306, 392, "Ảnh (đường dẫn https)")
    wf_input(d, 634, 350, 392, "Thẻ (tags)")
    wf_btn(d, 930, 396, 96, 24, "Đăng bài", size=8, pill=False)
    wf_flow(d, cp, s2, points=[(600, 90)], entry=(0, 0.15))
    d.save(SRC / "UI-006-community.drawio")


def admin():
    d = Doc("wf-admin")
    wf_grid(d, 0, 0, 700, 420)
    screen_with_header(d, 20, 20, 660, 380, admin=True)
    wf_label(d, "Hệ Thống Quản Trị Toàn Diện", 34, 50, 300, 18, size=11, bold=True)
    for k, t in enumerate(["Người dùng", "Chuyến đi", "Tri thức điểm đến", "Thống kê", "Công cụ AI"]):
        wf_btn(d, 34 + k * 112, 78, 106, 22, t, size=8, fill="#DAE8FC" if k == 0 else "#FFFFFF")
    wf_input(d, 34, 112, 400, h=22)
    wf_btn(d, 556, 112, 110, 22, "+ Tạo người dùng", size=8, pill=False)
    wf_rect(d, 34, 146, 632, 236, rounded=True)
    cols = ["Họ tên", "Email", "Vai trò", "Trạng thái", "Hành động"]
    for k, t in enumerate(cols):
        wf_label(d, t, 44 + k * 124, 152, 110, 16, size=9, bold=True)
    for r in range(6):
        y = 182 + r * 30
        for k in range(4):
            d.line(44 + k * 124, y + 8, 140 + k * 124, y + 8)
        wf_btn(d, 540, y, 50, 18, "Sửa", size=8, pill=False)
        wf_btn(d, 598, y, 56, 18, "Khóa", size=8, pill=False)
    d.save(SRC / "UI-007-admin.drawio")


if __name__ == "__main__":
    for f in (auth, dashboard, trip_detail, budget_checklist, places, community, admin):
        f()
    print("wireframes ok")
