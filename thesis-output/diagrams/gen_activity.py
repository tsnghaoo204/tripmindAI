"""Biểu đồ hoạt động (bằng chứng: spec/key-flows.md, spec/feature-catalog.md)."""
from common import SRC
from drawio_lib import Doc, Lanes, act_action, act_decision, act_end, act_flow, act_start

ROW = 76
TOP = 50


def build_activity(fname, lanes, widths, nodes, edges, rows):
    """nodes: id -> (kind, lane, row, label, frac, w); edges: (a, b, label, exit, entry, points)."""
    d = Doc(fname)
    L = Lanes(d, lanes, widths, height=TOP + rows * ROW + 30)
    ids = {}
    for nid, spec in nodes.items():
        kind, lane, row, label = spec[:4]
        frac = spec[4] if len(spec) > 4 else 0.5
        w = spec[5] if len(spec) > 5 else 190
        cx = L.cx(lane, frac)
        y = TOP + row * ROW
        if kind == "start":
            ids[nid] = act_start(d, cx, y + 10)
        elif kind == "end":
            ids[nid] = act_end(d, cx, y + 8)
        elif kind == "dec":
            ids[nid] = act_decision(d, cx, y + 5, label)
        else:
            ids[nid] = act_action(d, label, cx, y, w=w, h=46)
    for e in edges:
        a, b = e[0], e[1]
        label = e[2] if len(e) > 2 else ""
        ex = e[3] if len(e) > 3 else None
        en = e[4] if len(e) > 4 else None
        pts = e[5] if len(e) > 5 else None
        act_flow(d, ids[a], ids[b], label, points=pts, exit=ex, entry=en)
    d.save(SRC / fname)
    return L


R, B, T, L_ = (1, 0.5), (0.5, 1), (0.5, 0), (0, 0.5)


def auth():
    nodes = {
        "s": ("start", 0, 0, ""), "a1": ("act", 0, 1, "Mở trang xác thực"),
        "d0": ("dec", 0, 2, "Đã có tài khoản?"),
        "a2": ("act", 0, 3, "Nhập họ tên, email, mật khẩu, nhấn “Đăng ký”"),
        "s1": ("act", 1, 3, "Kiểm tra dữ liệu và email đã tồn tại"),
        "d1": ("dec", 1, 4, "Hợp lệ?", 0.4), "e1": ("act", 1, 4, "Báo lỗi", 0.82, 130),
        "s2": ("act", 1, 5, "Băm mật khẩu, tạo tài khoản vai trò Người dùng", 0.4, 220),
        "s3": ("act", 1, 6, "Thông báo thành công, chuyển trang đăng nhập", 0.4, 220),
        "a3": ("act", 0, 7, "Nhập email, mật khẩu, nhấn “Đăng nhập”"),
        "s4": ("act", 1, 7, "Kiểm tra số lần đăng nhập sai", 0.4),
        "d2": ("dec", 1, 8, "Đang bị tạm khóa?", 0.4), "e2": ("act", 1, 8, "Báo lỗi kèm thời gian chờ", 0.82, 150),
        "s5": ("act", 1, 9, "Đối chiếu mật khẩu và trạng thái tài khoản", 0.4, 220),
        "d3": ("dec", 1, 10, "Hợp lệ?", 0.4), "e3": ("act", 1, 10, "Ghi nhận thất bại, báo lỗi", 0.82, 150),
        "s6": ("act", 1, 11, "Cấp mã thông báo phiên, xóa bộ đếm sai", 0.4, 220),
        "a4": ("act", 0, 12, "Lưu phiên, chuyển tới trang chủ"), "end": ("end", 0, 13, ""),
    }
    edges = [("s", "a1"), ("a1", "d0"), ("d0", "a2", "Chưa"), ("a2", "s1"), ("s1", "d1"),
             ("d1", "e1", "Không"), ("e1", "a2", "", T, (0.75, 0), [(706, 266), (187, 266)]),
             ("d1", "s2", "Có"), ("s2", "s3"), ("s3", "a3", "", L_, (1, 0.3), [(300, 529), (300, 596)]),
             ("d0", "a3", "Rồi", L_, L_, [(20, 222), (20, 605)]),
             ("a3", "s4", "", (1, 0.7), (0, 0.7)), ("s4", "d2"), ("d2", "e2", "Có"),
             ("e2", "a3", "", B, B, [(706, 724), (140, 724)]),
             ("d2", "s5", "Không"), ("s5", "d3"), ("d3", "e3", "Không"),
             ("e3", "a3", "", R, (0.7, 1), [(792, 833), (792, 724), (178, 724)]),
             ("d3", "s6", "Có"), ("s6", "a4", "", B, R), ("a4", "end")]
    build_activity("DGM-ACT-001-xac-thuc.drawio", ["Người dùng", "Hệ thống"], [280, 520], nodes, edges, 14)


def create_trip():
    nodes = {
        "s": ("start", 0, 0, ""), "a1": ("act", 0, 1, "Chọn tạo chuyến đi mới"),
        "d0": ("dec", 0, 2, "Nhập câu mô tả?"),
        "a2": ("act", 0, 3, "Nhập mô tả bằng ngôn ngữ tự nhiên"),
        "s1": ("act", 1, 3, "Kiểm tra giới hạn lượt dùng AI", 0.35),
        "d1": ("dec", 1, 4, "AI khả dụng?", 0.35),
        "s2": ("act", 2, 4, "Trích xuất điểm đến, số ngày, số người, ngân sách", 0.5, 220),
        "s3": ("act", 1, 5, "Phân tích dự phòng theo mẫu câu", 0.75, 180),
        "s4": ("act", 1, 6, "Điền sẵn biểu mẫu nhiều bước", 0.35),
        "a3": ("act", 0, 7, "Chọn điểm đến, nhập ngày, đoàn đi, sở thích, ngân sách; xác nhận", 0.5, 230),
        "s5": ("act", 1, 7, "Kiểm tra ngày, độ dài ≤ 30 ngày, thành phần đoàn", 0.35, 220),
        "d2": ("dec", 1, 8, "Hợp lệ?", 0.35), "e1": ("act", 1, 8, "Báo lỗi", 0.82, 120), "x1": ("end", 1, 9, "", 0.82),
        "s6": ("act", 1, 9, "Xác định điểm đến (danh mục, Google Places hoặc nhập tay)", 0.35, 240),
        "s7": ("act", 1, 10, "Tạo chuyến đi và một ngày lịch trình cho mỗi ngày", 0.35, 240),
        "a4": ("act", 0, 11, "Xem chuyến đi mới trong danh sách"), "end": ("end", 0, 12, ""),
    }
    edges = [("s", "a1"), ("a1", "d0"), ("d0", "a2", "Có"), ("d0", "a3", "Không", L_, L_, [(20, 222), (20, 605)]),
             ("a2", "s1"), ("s1", "d1"), ("d1", "s2", "Có"), ("d1", "s3", "Không", B, T, [(462, 412), (670, 412)]),
             ("s2", "s4", "", B, R, [(940, 529)]), ("s3", "s4", "", B, R, [(670, 529)]),
             ("s4", "a3", "", B, T, [(462, 567), (140, 567)]), ("a3", "s5"), ("s5", "d2"), ("d2", "e1", "Không"),
             ("e1", "x1"), ("d2", "s6", "Có"), ("s6", "s7"),
             ("s7", "a4", "", B, R), ("a4", "end")]
    build_activity("DGM-ACT-002-tao-chuyen-di.drawio", ["Người dùng", "Hệ thống", "Dịch vụ LLM"],
                   [280, 470, 300], nodes, edges, 13)


def add_activity():
    nodes = {
        "s": ("start", 0, 0, ""), "a1": ("act", 0, 1, "Chọn “Thêm hoạt động” trên một ngày"),
        "s1": ("act", 1, 1, "Hiển thị biểu mẫu và danh sách địa điểm đã lưu"),
        "d0": ("dec", 0, 2, "Chọn địa điểm đã lưu?"),
        "a2": ("act", 0, 3, "Chọn địa điểm; hệ thống điền tiêu đề, loại"),
        "a3": ("act", 0, 4, "Nhập tiêu đề, loại, giờ, chi phí"),
        "d1": ("dec", 0, 5, "Cần gợi ý chi phí?"),
        "s2": ("act", 1, 5, "Ước tính theo mức giá địa điểm và số người", 0.4, 220),
        "d2": ("dec", 1, 6, "Đủ dữ liệu?", 0.4), "s3": ("act", 1, 6, "Hiển thị lý do, không đoán giá", 0.82, 160),
        "s4": ("act", 1, 7, "Hiển thị chi phí gợi ý", 0.4),
        "a4": ("act", 0, 8, "Nhấn “Lưu”"),
        "s5": ("act", 1, 8, "Kiểm tra quyền sở hữu, ngày thuộc chuyến đi, khung giờ", 0.4, 240),
        "d3": ("dec", 1, 9, "Hợp lệ?", 0.4), "e1": ("act", 1, 9, "Báo lỗi", 0.82, 120),
        "x1": ("end", 1, 10, "", 0.82),
        "s6": ("act", 1, 10, "Ghi nhận địa điểm vào danh mục nếu chưa có", 0.4, 230),
        "s7": ("act", 1, 11, "Thêm hoạt động vào cuối ngày", 0.4),
        "a5": ("act", 0, 12, "Xem lịch trình và bản đồ cập nhật"), "end": ("end", 0, 13, ""),
    }
    edges = [("s", "a1"), ("a1", "s1"), ("s1", "d0", "", B, R, [(540, 224)]), ("d0", "a2", "Có"),
             ("d0", "a3", "Không", L_, L_, [(20, 224), (20, 377)]), ("a2", "a3"), ("a3", "d1"),
             ("d1", "s2", "Có"), ("d1", "a4", "Không", L_, L_, [(20, 452), (20, 681)]), ("s2", "d2"),
             ("d2", "s3", "Không"), ("d2", "s4", "Có"),
             ("s3", "a4", "", B, (0.6, 0), [(706, 645), (159, 645)]),
             ("s4", "a4", "", L_, (0.4, 0), [(121, 605)]), ("a4", "s5"), ("s5", "d3"), ("d3", "e1", "Không"),
             ("e1", "x1"), ("d3", "s6", "Có"), ("s6", "s7"), ("s7", "a5", "", B, R), ("a5", "end")]
    build_activity("DGM-ACT-003-them-hoat-dong.drawio", ["Người dùng", "Hệ thống"], [280, 520], nodes, edges, 14)


def generate():
    nodes = {
        "s": ("start", 0, 0, ""), "a1": ("act", 0, 1, "Nhấn “Tạo lịch trình AI”"),
        "s1": ("act", 1, 1, "Kiểm tra quyền, cấu hình AI, lượt dùng AI", 0.4, 220),
        "d1": ("dec", 1, 2, "Lịch trình trống?", 0.4), "e1": ("act", 1, 2, "Từ chối, báo lỗi", 0.85, 130),
        "x0": ("end", 1, 3, "", 0.85),
        "s2": ("act", 1, 3, "Tạo tác vụ nền, đưa vào hàng đợi", 0.4, 210),
        "a2": ("act", 0, 3, "Theo dõi tiến độ theo thời gian thực"),
        "s3": ("act", 1, 4, "Thu thập địa điểm thực theo điểm đến, chế độ ăn, sở thích", 0.4, 240),
        "g1": ("act", 2, 4, "Google Places trả về địa điểm", 0.5, 200),
        "s4": ("act", 1, 5, "Loại địa điểm người dùng không thích, xếp hạng", 0.4, 230),
        "s5": ("act", 1, 6, "Lấy thời tiết từng ngày và tri thức điểm đến", 0.4, 230),
        "g2": ("act", 2, 7, "Mô hình ngôn ngữ đề xuất lịch trình", 0.5, 200),
        "s6": ("act", 1, 8, "Kiểm chứng từng hoạt động với địa điểm thực", 0.4, 230),
        "d2": ("dec", 1, 9, "Có hoạt động hợp lệ?", 0.4),
        "e2": ("act", 1, 9, "Đánh dấu thất bại, báo lỗi", 0.85, 150), "x2": ("end", 1, 10, "", 0.85),
        "s7": ("act", 1, 10, "Lưu đề xuất và áp dụng vào lịch trình", 0.4, 220),
        "s8": ("act", 1, 11, "Sinh phương án thay thế cho ăn uống, lưu trú", 0.4, 230),
        "a3": ("act", 0, 12, "Xem lịch trình mới và số mục bị loại"), "end": ("end", 0, 13, ""),
    }
    edges = [("s", "a1"), ("a1", "s1"), ("s1", "d1"), ("d1", "e1", "Không"), ("e1", "x0"), ("d1", "s2", "Có"),
             ("s2", "a2", "", L_, R), ("s2", "s3"), ("a2", "a3", "chờ kết quả"), ("s3", "g1"),
             ("g1", "s4", "", B, R, [(940, 453)]), ("s4", "s5"), ("s5", "g2", "", R, T, [(940, 529)]),
             ("g2", "s6", "", B, R, [(940, 681)]), ("s6", "d2"), ("d2", "e2", "Không"), ("e2", "x2"),
             ("d2", "s7", "Có"), ("s7", "s8"), ("s8", "a3", "", B, R), ("a3", "end")]
    build_activity("DGM-ACT-004-sinh-lich-trinh.drawio", ["Người dùng", "Hệ thống", "Dịch vụ bên ngoài"],
                   [280, 520, 280], nodes, edges, 14)


def chat():
    nodes = {
        "s": ("start", 0, 0, ""), "a1": ("act", 0, 1, "Nhập câu hỏi và gửi"),
        "s1": ("act", 1, 1, "Kiểm tra quyền, cấu hình AI, lượt dùng", 0.4, 220),
        "s2": ("act", 1, 2, "Lưu tin nhắn, dựng ngữ cảnh chuyến đi và lịch sử", 0.4, 230),
        "g1": ("act", 2, 3, "Mô hình trả lời hoặc yêu cầu gọi công cụ", 0.5, 200),
        "d1": ("dec", 1, 4, "Yêu cầu công cụ?", 0.4),
        "s3": ("act", 1, 4, "Thực thi công cụ, ghi nhật ký", 0.82, 160),
        "d2": ("dec", 1, 5, "Còn lượt, còn thời gian?", 0.82),
        "s4": ("act", 1, 6, "Truyền dần câu trả lời, lưu tin nhắn", 0.4, 220),
        "a2": ("act", 0, 6, "Đọc câu trả lời và thẻ đề xuất"),
        "d3": ("dec", 0, 7, "Quyết định?"),
        "s7": ("act", 1, 7, "Đánh dấu đề xuất bị từ chối", 0.82, 160), "x1": ("end", 1, 8, "", 0.82),
        "a3": ("act", 0, 8, "Nhấn “Áp dụng”"),
        "s5": ("act", 1, 8, "Kiểm tra đề xuất còn hiệu lực, chưa lỗi thời", 0.4, 220),
        "d4": ("dec", 1, 9, "Hợp lệ?", 0.4), "e1": ("act", 1, 9, "Báo lỗi đề xuất", 0.82, 160),
        "x2": ("end", 1, 10, "", 0.82),
        "s6": ("act", 1, 10, "Áp dụng thay đổi, lưu nhật ký hoàn tác", 0.4, 220),
        "a4": ("act", 0, 11, "Xem lịch trình mới; có thể hoàn tác trong 10 phút", 0.5, 220),
        "end": ("end", 0, 12, ""),
    }
    edges = [("s", "a1"), ("a1", "s1"), ("s1", "s2"), ("s2", "g1", "", R, T, [(940, 225)]),
             ("g1", "d1", "", B, T, [(940, 345), (488, 345)]), ("d1", "s3", "Có"), ("s3", "d2"),
             ("d2", "g1", "Có", R, R, [(1060, 452), (1060, 301)]), ("d2", "s4", "Không", B, R, [(706, 529)]),
             ("d1", "s4", "Không"), ("s4", "a2", "", L_, R), ("a2", "d3"), ("d3", "a3", "Áp dụng"),
             ("d3", "s7", "Từ chối"), ("s7", "x1"), ("a3", "s5"), ("s5", "d4"), ("d4", "e1", "Không"), ("e1", "x2"),
             ("d4", "s6", "Có"), ("s6", "a4", "", B, R), ("a4", "end")]
    build_activity("DGM-ACT-005-tro-ly-ai.drawio", ["Người dùng", "Hệ thống", "Dịch vụ LLM"],
                   [280, 520, 280], nodes, edges, 13)


def community():
    nodes = {
        "s": ("start", 0, 0, ""), "a1": ("act", 0, 1, "Chọn viết bài chia sẻ"),
        "d0": ("dec", 0, 2, "Dùng AI viết review?"),
        "a2": ("act", 0, 3, "Chọn “AI viết review” trên chuyến đi đã kết thúc", 0.5, 220),
        "s1": ("act", 1, 3, "Tổng hợp địa điểm đã đi, chi tiêu, đánh giá", 0.4, 230),
        "g1": ("act", 2, 3, "Sinh tiêu đề và nội dung bài viết", 0.5, 200),
        "d1": ("dec", 1, 4, "Nội dung AI hợp lệ?", 0.4),
        "s2": ("act", 1, 4, "Tạo bản nháp theo mẫu", 0.82, 160),
        "s3": ("act", 1, 6, "Điền sẵn biểu mẫu đăng bài", 0.4),
        "a3": ("act", 0, 7, "Nhập/chỉnh tiêu đề, điểm đến, nội dung, ảnh, chuyến đi liên kết", 0.5, 230),
        "s4": ("act", 1, 7, "Kiểm tra dữ liệu, ảnh https, chuyến đi thuộc người dùng", 0.4, 240),
        "d2": ("dec", 1, 8, "Hợp lệ?", 0.4), "e1": ("act", 1, 8, "Báo lỗi", 0.82, 120), "x1": ("end", 1, 9, "", 0.82),
        "s5": ("act", 1, 9, "Lưu bài viết", 0.4),
        "a4": ("act", 0, 10, "Xem bài viết trên bảng tin"), "end": ("end", 0, 11, ""),
    }
    edges = [("s", "a1"), ("a1", "d0"), ("d0", "a2", "Có"), ("d0", "a3", "Không", L_, L_, [(20, 224), (20, 605)]),
             ("a2", "s1"), ("s1", "g1"), ("g1", "d1", "", B, T, [(940, 345), (488, 345)]),
             ("d1", "s3", "Có"), ("d1", "s2", "Không"), ("s2", "s3", "", B, R, [(706, 529)]),
             ("s3", "a3", "", B, T, [(488, 567), (140, 567)]), ("a3", "s4"), ("s4", "d2"), ("d2", "e1", "Không"),
             ("e1", "x1"), ("d2", "s5", "Có"), ("s5", "a4", "", B, R), ("a4", "end")]
    build_activity("DGM-ACT-006-cong-dong.drawio", ["Người dùng", "Hệ thống", "Dịch vụ LLM"],
                   [280, 520, 280], nodes, edges, 12)


def knowledge():
    nodes = {
        "s": ("start", 0, 0, ""), "a1": ("act", 0, 1, "Chọn “Trích xuất tri thức” trên bài viết", 0.5, 220),
        "s1": ("act", 1, 1, "Kiểm tra vai trò quản trị viên", 0.4),
        "d0": ("dec", 1, 2, "Có quyền?", 0.4), "e0": ("act", 1, 2, "Từ chối truy cập", 0.72, 140),
        "x0": ("end", 1, 2, "", 0.95),
        "s2": ("act", 1, 3, "Lấy nội dung bài viết, đánh dấu là dữ liệu không tin cậy", 0.4, 240),
        "g1": ("act", 2, 3, "Tóm tắt thành mục tri thức có cấu trúc", 0.5, 200),
        "d1": ("dec", 1, 4, "Kết quả AI hợp lệ?", 0.4),
        "s3": ("act", 1, 4, "Dùng nội dung gốc làm bản nháp", 0.82, 160),
        "s4": ("act", 1, 6, "Hiển thị bản nháp tri thức", 0.4),
        "a2": ("act", 0, 7, "Chỉnh danh mục, tiêu đề, nội dung, độ ưu tiên; xác nhận", 0.5, 230),
        "s5": ("act", 1, 7, "Kiểm tra dữ liệu", 0.4),
        "d2": ("dec", 1, 8, "Hợp lệ?", 0.4), "e1": ("act", 1, 8, "Báo lỗi", 0.82, 120), "x1": ("end", 1, 9, "", 0.82),
        "s6": ("act", 1, 9, "Lưu mục tri thức, liên kết với bài viết", 0.4, 220),
        "a3": ("act", 0, 10, "Tri thức được dùng trong ngữ cảnh AI", 0.5, 220), "end": ("end", 0, 11, ""),
    }
    edges = [("s", "a1"), ("a1", "s1"), ("s1", "d0"), ("d0", "e0", "Không"), ("e0", "x0"), ("d0", "s2", "Có"),
             ("s2", "g1"), ("g1", "d1", "", B, T, [(940, 345), (488, 345)]), ("d1", "s4", "Có"),
             ("d1", "s3", "Không"), ("s3", "s4", "", B, R, [(706, 529)]),
             ("s4", "a2", "", B, T, [(488, 567), (140, 567)]), ("a2", "s5"), ("s5", "d2"), ("d2", "e1", "Không"),
             ("e1", "x1"), ("d2", "s6", "Có"), ("s6", "a3", "", B, R), ("a3", "end")]
    build_activity("DGM-ACT-007-tri-thuc.drawio", ["Quản trị viên", "Hệ thống", "Dịch vụ LLM"],
                   [280, 520, 280], nodes, edges, 12)


def build():
    auth()
    create_trip()
    add_activity()
    generate()
    chat()
    community()
    knowledge()
