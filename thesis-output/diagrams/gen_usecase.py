"""Biểu đồ Use Case: tổng quan và phân rã."""
from common import SRC
from drawio_lib import Doc, uc_actor, uc_assoc, uc_boundary, uc_case, uc_dep, uc_gen

USER_UCS = ["Xác thực và quản lý tài khoản", "Quản lý chuyến đi", "Quản lý lịch trình",
            "Sinh lịch trình tự động bằng AI", "Trò chuyện với trợ lý AI", "Quản lý ngân sách và chi tiêu",
            "Quản lý danh sách chuẩn bị", "Khám phá và đánh giá địa điểm", "Cộng đồng chia sẻ kinh nghiệm"]


def overview():
    d = Doc("UC tổng quan")
    uc_boundary(d, "Hệ thống TripMind", 200, 10, 300, 800)
    guest = uc_actor(d, "Khách", 40, 40)
    user = uc_actor(d, "Người dùng", 40, 330)
    admin = uc_actor(d, "Quản trị viên", 40, 700)
    ids = []
    for i, name in enumerate(["Đăng ký, đăng nhập"] + USER_UCS + ["Quản trị hệ thống"]):
        ids.append(uc_case(d, name, 245, 45 + i * 68, 210, 46))
    uc_assoc(d, guest, ids[0])
    for k in range(1, 10):
        uc_assoc(d, user, ids[k])
    uc_assoc(d, admin, ids[10])
    uc_gen(d, admin, user, points=[(15, 718), (15, 362)], extra="exitX=0;exitY=0.3;entryX=0;entryY=0.5;")
    llm = uc_actor(d, "Dịch vụ LLM", 600, 300)
    gp = uc_actor(d, "Google Places", 600, 470)
    wt = uc_actor(d, "Open-Meteo", 600, 620)
    for k in (4, 5):
        uc_assoc(d, ids[k], llm, extra="exitX=1;exitY=0.5;")
    for k in (2, 4, 8):
        uc_assoc(d, ids[k], gp, extra="exitX=1;exitY=0.5;")
    for k in (4, 7):
        uc_assoc(d, ids[k], wt, extra="exitX=1;exitY=0.5;")
    d.save(SRC / "DGM-UC-001-tong-quan.drawio")


def side(fname, title, actor_label, cases, actor2=None):
    d = Doc(title)
    h = 40 + len(cases) * 70
    uc_boundary(d, "Hệ thống TripMind", 200, 10, 300, h)
    a = uc_actor(d, actor_label, 40, h / 2 - 30)
    for i, c in enumerate(cases):
        u = uc_case(d, c, 240, 45 + i * 70, 220, 46)
        uc_assoc(d, a, u)
    if actor2:
        g = uc_actor(d, actor2, 40, h / 2 + 90)
        uc_gen(d, a, g)
    d.save(SRC / fname)


def decomp(fname, title, main, subs, extras=(), deps=(), actor="Người dùng", sec=()):
    """subs: tên UC con (tổng quát hóa về main); extras: (tên, hàng) cột phải; deps: (nguồn, đích, kind)."""
    d = Doc(title)
    n = len(subs)
    top = 20
    ids = {}
    for i, s in enumerate(subs):
        ids[s] = uc_case(d, s, 400, top + i * 72, 230, 52, size=14)
    my = top + (n - 1) * 72 / 2
    ids[main] = uc_case(d, main, 120, my, 230, 56, size=14)
    a = uc_actor(d, actor, 10, my - 6)
    uc_assoc(d, a, ids[main])
    for s in subs:
        uc_gen(d, ids[s], ids[main])
    for name, row in extras:
        ids[name] = uc_case(d, name, 690, top + row * 72, 230, 52, size=14)
    for src, tgt, kind in deps:
        uc_dep(d, ids[src], ids[tgt], kind)
    for label, row, links in sec:
        s = uc_actor(d, label, 970, top + row * 72 - 6)
        for l in links:
            uc_assoc(d, ids[l], s, extra="exitX=1;exitY=0.5;")
    d.save(SRC / fname)


def build():
    overview()
    side("DGM-UC-002-nguoi-dung.drawio", "UC phía người dùng", "Người dùng", USER_UCS)
    side("DGM-UC-003-quan-tri.drawio", "UC phía quản trị", "Quản trị viên",
         ["Quản lý người dùng", "Quản lý chuyến đi (toàn hệ thống)", "Quản lý tri thức điểm đến",
          "Trích xuất tri thức từ bài viết", "Xem thống kê sử dụng AI", "Xem nhật ký công cụ AI"])
    decomp("DGM-UC-004-xac-thuc.drawio", "UC Xác thực", "Xác thực và quản lý tài khoản",
           ["Đăng ký", "Đăng nhập", "Đăng xuất", "Cập nhật hồ sơ", "Đổi mật khẩu"],
           extras=[("Cấp lại phiên đăng nhập", 4)], deps=[("Đổi mật khẩu", "Cấp lại phiên đăng nhập", "include")],
           actor="Tác nhân")
    decomp("DGM-UC-005-chuyen-di.drawio", "UC Quản lý chuyến đi", "Quản lý chuyến đi",
           ["Tạo chuyến đi", "Xem danh sách chuyến đi", "Xem chi tiết chuyến đi", "Thay đổi giai đoạn",
            "Nhân bản chuyến đi", "Xóa chuyến đi", "Xuất lịch trình ra tệp lịch"],
           extras=[("Phân tích mô tả bằng AI", 0), ("Xác định điểm đến", 1)],
           deps=[("Phân tích mô tả bằng AI", "Tạo chuyến đi", "extend"), ("Tạo chuyến đi", "Xác định điểm đến", "include")],
           sec=[("Google Places", 1, ["Xác định điểm đến"])])
    decomp("DGM-UC-006-lich-trinh.drawio", "UC Quản lý lịch trình", "Quản lý lịch trình",
           ["Xem lịch trình theo ngày", "Thêm hoạt động", "Xóa hoạt động", "Xem phương án thay thế",
            "Xem lý do AI đề xuất"],
           extras=[("Xem bản đồ trong ngày", 0), ("Gợi ý chi phí", 1), ("Chọn phương án thay thế", 3)],
           deps=[("Xem bản đồ trong ngày", "Xem lịch trình theo ngày", "extend"),
                 ("Gợi ý chi phí", "Thêm hoạt động", "extend"),
                 ("Chọn phương án thay thế", "Xem phương án thay thế", "extend")])
    decomp("DGM-UC-007-sinh-lich-trinh.drawio", "UC Sinh lịch trình AI", "Sinh lịch trình tự động bằng AI",
           ["Yêu cầu sinh lịch trình", "Theo dõi tiến độ sinh lịch trình", "Xem kết quả và hoạt động bị loại"],
           extras=[("Thu thập địa điểm thực", 0), ("Kiểm chứng địa điểm", 1), ("Áp dụng lịch trình", 2)],
           deps=[("Yêu cầu sinh lịch trình", "Thu thập địa điểm thực", "include"),
                 ("Yêu cầu sinh lịch trình", "Kiểm chứng địa điểm", "include"),
                 ("Yêu cầu sinh lịch trình", "Áp dụng lịch trình", "include")],
           sec=[("Google Places", 0, ["Thu thập địa điểm thực"]), ("Dịch vụ LLM", 1.6, ["Yêu cầu sinh lịch trình"])])
    decomp("DGM-UC-008-tro-ly-ai.drawio", "UC Trợ lý AI", "Trò chuyện với trợ lý AI",
           ["Gửi câu hỏi cho trợ lý", "Xem lịch sử hội thoại", "Áp dụng đề xuất", "Từ chối đề xuất"],
           extras=[("Hoàn tác đề xuất", 2)], deps=[("Hoàn tác đề xuất", "Áp dụng đề xuất", "extend")],
           sec=[("Dịch vụ LLM", 0, ["Gửi câu hỏi cho trợ lý"])])
    decomp("DGM-UC-009-ngan-sach.drawio", "UC Ngân sách", "Quản lý ngân sách và chi tiêu",
           ["Xem tổng hợp ngân sách", "Thêm khoản chi", "Xem lịch sử chi tiêu", "Xóa khoản chi"])
    decomp("DGM-UC-010-checklist.drawio", "UC Checklist", "Quản lý danh sách chuẩn bị",
           ["Xem danh sách chuẩn bị", "Thêm mục chuẩn bị", "Đánh dấu hoàn thành", "Xóa mục chuẩn bị",
            "Nhận gợi ý chuẩn bị"],
           extras=[("Tham chiếu dự báo thời tiết", 4)],
           deps=[("Nhận gợi ý chuẩn bị", "Tham chiếu dự báo thời tiết", "include")],
           sec=[("Open-Meteo", 4, ["Tham chiếu dự báo thời tiết"])])
    decomp("DGM-UC-011-dia-diem.drawio", "UC Địa điểm", "Khám phá và đánh giá địa điểm",
           ["Tìm kiếm địa điểm", "Lưu địa điểm yêu thích", "Xem địa điểm đã lưu", "Đánh giá địa điểm sau chuyến đi"],
           extras=[("Xem địa điểm trên bản đồ", 0)],
           deps=[("Xem địa điểm trên bản đồ", "Tìm kiếm địa điểm", "extend")],
           sec=[("Google Places", 0.9, ["Tìm kiếm địa điểm"])])
    decomp("DGM-UC-012-cong-dong.drawio", "UC Cộng đồng", "Cộng đồng chia sẻ kinh nghiệm",
           ["Xem bảng tin bài viết", "Đăng bài chia sẻ", "Bình luận bài viết", "Bình chọn hữu ích",
            "Sao chép lịch trình từ bài viết", "Xóa bài viết"],
           extras=[("Lọc và tìm kiếm bài viết", 0), ("Tạo bản review bằng AI", 1)],
           deps=[("Lọc và tìm kiếm bài viết", "Xem bảng tin bài viết", "extend"),
                 ("Tạo bản review bằng AI", "Đăng bài chia sẻ", "extend")])
    decomp("DGM-UC-013-quan-tri.drawio", "UC Quản trị", "Quản trị hệ thống",
           ["Quản lý người dùng", "Quản lý chuyến đi", "Quản lý tri thức điểm đến", "Xem thống kê sử dụng AI",
            "Xem nhật ký công cụ AI"],
           extras=[("Trích xuất tri thức từ bài viết", 2)],
           deps=[("Trích xuất tri thức từ bài viết", "Quản lý tri thức điểm đến", "extend")],
           actor="Quản trị viên")
