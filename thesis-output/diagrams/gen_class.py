"""Biểu đồ lớp phân tích theo chức năng (bằng chứng: spec/data-model.md, spec/key-flows.md)."""
from common import SRC
from drawio_lib import Doc, uml_assoc, uml_class


def layout(fname, boundary, controls, entities, links, pos=None):
    """boundary: (name, methods); controls: [(name, methods)]; entities: [(name, attrs, methods)];
    links: (src, dst, label, m1, m2, kind) theo tên lớp."""
    d = Doc(fname)
    ids = {}
    bname, bmeth = boundary
    pos = pos or {}
    ids[bname] = uml_class(d, bname, 20, 20, 240, methods=bmeth, stereo="boundary")
    for i, (cname, cmeth) in enumerate(controls):
        x, y = pos.get(cname, (330, 20 + i * 230))
        ids[cname] = uml_class(d, cname, x, y, 240, methods=cmeth, stereo="control")
    for i, (ename, attrs, meth) in enumerate(entities):
        x, y = pos.get(ename, (660, 20 + i * 240))
        ids[ename] = uml_class(d, ename, x, y, 250, attrs=attrs, methods=meth, stereo="entity")
    for link in links:
        src, dst, label, m1, m2, kind = link[:6]
        ex = link[6] if len(link) > 6 else None
        en = link[7] if len(link) > 7 else None
        uml_assoc(d, ids[src], ids[dst], label, m1, m2, kind=kind, exit=ex, entry=en)
    d.save(SRC / fname)


def build():
    layout("DGM-CLS-001-xac-thuc.drawio",
           ("GiaoDienXacThuc", ["+hiểnThịBiểuMẫuĐăngKý()", "+hiểnThịBiểuMẫuĐăngNhập()", "+gửiYêuCầu()",
                                "+hiểnThịLỗi()", "+lưuPhiên()", "+chuyểnTrang()"]),
           [("DieuKhienXacThuc", ["+đăngKý()", "+đăngNhập()", "+kiểmTraHợpLệ()", "+đốiChiếuMậtKhẩu()"]),
            ("BoGioiHanDangNhap", ["+kiểmTraSốLầnSai()", "+ghiNhậnThấtBại()", "+xóaBộĐếm()"]),
            ("BoCapMaPhien", ["+tạoMãPhiên()", "+xácThựcMãPhiên()"])],
           [("TaiKhoan", ["-id : bigint", "-email : varchar", "-matKhauBam : varchar", "-hoTen : varchar",
                          "-vaiTro : USER | ADMIN", "-dangHoatDong : boolean", "-phienBanMa : int"],
             ["+tìmTheoEmail()", "+tồnTạiEmail()", "+tạoTàiKhoản()"])],
           [("GiaoDienXacThuc", "DieuKhienXacThuc", "gọi", "1", "1", "assoc"),
            ("DieuKhienXacThuc", "BoGioiHanDangNhap", "sử dụng", "1", "1", "dep", (0.25, 1), (0.5, 0)),
            ("DieuKhienXacThuc", "BoCapMaPhien", "sử dụng", "1", "1", "dep", (0.5, 1), (0.5, 0)),
            ("DieuKhienXacThuc", "TaiKhoan", "truy vấn", "1", "*", "assoc")],
           pos={"BoGioiHanDangNhap": (20, 240), "BoCapMaPhien": (330, 240), "TaiKhoan": (660, 20)})

    layout("DGM-CLS-002-tao-chuyen-di.drawio",
           ("GiaoDienTaoChuyenDi", ["+nhậpMôTả()", "+điềnSẵnBiểuMẫu()", "+chọnĐiểmĐến()", "+gửiYêuCầuTạo()",
                                    "+hiểnThịLỗi()", "+cậpNhậtDanhSách()"]),
           [("DieuKhienChuyenDi", ["+phânTíchMôTả()", "+tạoChuyếnĐi()", "+kiểmTraNgày()", "+kiểmTraĐoànĐi()"]),
            ("BoPhanTichMoTa", ["+phânTíchBằngAI()", "+phânTíchDựPhòng()", "+khớpĐiểmĐến()"]),
            ("DichVuDiemDen", ["+tìmKiếmĐiểmĐến()", "+xácĐịnhHoặcTạoĐiểmĐến()"])],
           [("ChuyenDi", ["-id : bigint", "-ten : varchar", "-ngayDi : date", "-ngayVe : date", "-soNguoi : smallint",
                          "-nganSach : bigint", "-tienTe : char(3)", "-hoSoDoan : jsonb", "-keHoachDiChuyen : jsonb"],
             ["+tạoChuyếnĐi()"]),
            ("DiemDen", ["-id : bigint", "-ten : varchar", "-quocGia : varchar", "-viDo, kinhDo : numeric",
                         "-muiGio : varchar"], ["+tìmTheoTên()"]),
            ("NgayLichTrinh", ["-id : bigint", "-thuTuNgay : smallint", "-ngay : date"], ["+tạoTheoChuyếnĐi()"])],
           [("GiaoDienTaoChuyenDi", "DieuKhienChuyenDi", "gọi", "1", "1", "assoc"),
            ("DieuKhienChuyenDi", "BoPhanTichMoTa", "sử dụng", "1", "1", "dep", (0.25, 1), (0.5, 0)),
            ("DieuKhienChuyenDi", "DichVuDiemDen", "sử dụng", "1", "1", "dep", (0.5, 1), (0.5, 0)),
            ("DieuKhienChuyenDi", "ChuyenDi", "tạo", "1", "*", "assoc"),
            ("DichVuDiemDen", "DiemDen", "truy vấn", "1", "*", "assoc", (0.5, 1), (0, 0.5)),
            ("ChuyenDi", "NgayLichTrinh", "", "1", "1..30", "comp")],
           pos={"BoPhanTichMoTa": (20, 260), "DichVuDiemDen": (330, 260), "ChuyenDi": (660, 20), "NgayLichTrinh": (660, 300), "DiemDen": (660, 470)})

    layout("DGM-CLS-003-them-hoat-dong.drawio",
           ("GiaoDienLichTrinh", ["+hiểnThịBiểuMẫu()", "+yêuCầuGợiÝChiPhí()", "+gửiYêuCầuThêm()",
                                  "+hiểnThịLỗi()", "+cậpNhậtBảnĐồ()"]),
           [("DieuKhienLichTrinh", ["+thêmHoạtĐộng()", "+ướcTínhChiPhí()", "+kiểmTraKhungGiờ()"]),
            ("BoUocTinhChiPhi", ["+ướcTính(mứcGiá, loại, sốNgười)"])],
           [("NgayLichTrinh", ["-id : bigint", "-thuTuNgay : smallint", "-ngay : date"], ["+tìmTheoChuyếnĐi()"]),
            ("HoatDong", ["-id : bigint", "-tieuDe : varchar", "-loai : varchar", "-gioBatDau, gioKetThuc : time",
                          "-chiPhiUocTinh : bigint", "-thuTu : smallint", "-trangThai : varchar"],
             ["+lấyThứTựLớnNhất()", "+lưu()"]),
            ("DiaDiem", ["-id : bigint", "-nhaCungCap, maNgoai : varchar", "-ten : varchar", "-viDo, kinhDo : numeric",
                         "-mucGia : smallint"], ["+lấyHoặcGhiNhận()"])],
           [("GiaoDienLichTrinh", "DieuKhienLichTrinh", "gọi", "1", "1", "assoc"),
            ("DieuKhienLichTrinh", "BoUocTinhChiPhi", "sử dụng", "1", "1", "dep"),
            ("DieuKhienLichTrinh", "NgayLichTrinh", "truy vấn", "1", "*", "assoc"),
            ("NgayLichTrinh", "HoatDong", "", "1", "*", "comp"),
            ("HoatDong", "DiaDiem", "tại", "*", "0..1", "assoc")],
           pos={"BoUocTinhChiPhi": (330, 240), "NgayLichTrinh": (660, 20), "HoatDong": (660, 200), "DiaDiem": (660, 450)})

    layout("DGM-CLS-004-sinh-lich-trinh.drawio",
           ("GiaoDienChuyenDi", ["+yêuCầuSinhLịchTrình()", "+hiểnThịTiếnĐộ()", "+tảiLạiLịchTrình()",
                                 "+hiểnThịLỗi()"]),
           [("DieuKhienSinhLichTrinh", ["+bắtĐầu()", "+theoDõiTiếnĐộ()"]),
            ("TienTrinhXuLyNen", ["+xửLýTácVụ()", "+thuThậpĐịaĐiểm()", "+yêuCầuMôHình()", "+kiểmChứng()",
                                  "+phátTiếnĐộ()"]),
            ("DichVuApDungDeXuat", ["+ápDụng()", "+hoànTác()"])],
           [("TacVuSinhLichTrinh", ["-maTacVu : varchar", "-trangThai : varchar", "-buoc : varchar",
                                    "-tienDo : number"], ["+lưuTrạngThái()"]),
            ("DeXuatAI", ["-id : bigint", "-tomTat : text", "-danhSachThayDoi : jsonb", "-trangThai : varchar",
                          "-hetHanLuc : timestamptz", "-nhatKyHoanTac : jsonb"], ["+lưu()", "+đánhDấuHếtHạn()"]),
            ("HoatDong", ["-id : bigint", "-tieuDe : varchar", "-taoBoi : USER | AI", "-tuDeXuat : bigint"],
             ["+tạoTừĐềXuất()"])],
           [("GiaoDienChuyenDi", "DieuKhienSinhLichTrinh", "gọi", "1", "1", "assoc"),
            ("DieuKhienSinhLichTrinh", "TienTrinhXuLyNen", "đưa vào hàng đợi", "1", "1", "dep"),
            ("TienTrinhXuLyNen", "DichVuApDungDeXuat", "sử dụng", "1", "1", "dep"),
            ("TienTrinhXuLyNen", "TacVuSinhLichTrinh", "cập nhật", "1", "*", "assoc"),
            ("DichVuApDungDeXuat", "DeXuatAI", "áp dụng", "1", "*", "assoc"),
            ("DeXuatAI", "HoatDong", "sinh ra", "0..1", "*", "assoc")],
           pos={"TienTrinhXuLyNen": (330, 200), "DichVuApDungDeXuat": (330, 420), "TacVuSinhLichTrinh": (660, 200), "DeXuatAI": (660, 420), "HoatDong": (660, 680)})

    layout("DGM-CLS-005-tro-ly-ai.drawio",
           ("KhungTroLyAI", ["+gửiTinNhắn()", "+hiểnThịCâuTrảLời()", "+hiểnThịThẻĐềXuất()", "+ápDụng()",
                             "+từChối()", "+hoànTác()"]),
           [("DieuKhienTroLyAI", ["+tròChuyện()", "+ápDụngĐềXuất()", "+từChốiĐềXuất()", "+hoànTácĐềXuất()"]),
            ("TacTuAI", ["+xửLýLượtHộiThoại()", "+gọiMôHình()", "+thựcThiCôngCụ()", "+tạoĐềXuất()"])],
           [("HoiThoai", ["-id : bigint", "-tieuDe : varchar"], ["+mởHoặcTạo()", "+lấyLịchSử()"]),
            ("TinNhan", ["-id : bigint", "-vaiTro : varchar", "-noiDung : text", "-luongToken : jsonb"],
             ["+lưu()"]),
            ("DeXuatAI", ["-id : bigint", "-danhSachThayDoi : jsonb", "-trangThai : varchar",
                          "-hetHanLuc : timestamptz"], ["+ápDụng()", "+từChối()", "+hoànTác()"]),
            ("NhatKyCongCu", ["-id : bigint", "-tenCongCu : varchar", "-thamSo, ketQua : jsonb",
                              "-trangThai : varchar", "-thoiGianMs : int"], ["+ghiNhận()"])],
           [("KhungTroLyAI", "DieuKhienTroLyAI", "gọi", "1", "1", "assoc"),
            ("DieuKhienTroLyAI", "TacTuAI", "ủy quyền", "1", "1", "dep"),
            ("TacTuAI", "HoiThoai", "đọc/ghi", "1", "*", "assoc"),
            ("HoiThoai", "TinNhan", "", "1", "*", "comp"),
            ("TacTuAI", "NhatKyCongCu", "ghi", "1", "*", "assoc"),
            ("DieuKhienTroLyAI", "DeXuatAI", "xử lý", "1", "*", "assoc")],
           pos={"TacTuAI": (330, 240), "DeXuatAI": (660, 20), "HoiThoai": (660, 240), "TinNhan": (660, 420), "NhatKyCongCu": (20, 260)})

    layout("DGM-CLS-006-cong-dong.drawio",
           ("GiaoDienCongDong", ["+yêuCầuBảnTổngKết()", "+điềnSẵnBiểuMẫu()", "+gửiBàiViết()", "+hiểnThịLỗi()",
                                 "+cậpNhậtBảngTin()"]),
           [("DieuKhienBaiViet", ["+tạoBảnTổngKết()", "+đăngBài()", "+bìnhLuận()", "+bìnhChọn()", "+xóaBài()"])],
           [("ChuyenDi", ["-id : bigint", "-ten : varchar", "-ngayDi, ngayVe : date"], ["+lấyĐịaĐiểmVàChiTiêu()"]),
            ("BaiViet", ["-id : bigint", "-tieuDe : varchar", "-diemDen : varchar", "-noiDung : text",
                         "-anh : jsonb", "-soBinhChon : int", "-soBinhLuan : int"], ["+lưu()", "+xóa()"]),
            ("BinhLuan", ["-id : bigint", "-noiDung : text", "-soBinhChon : int"], ["+lưu()"]),
            ("BinhChonBaiViet", ["-id : bigint"], ["+bậtTắt()"])],
           [("GiaoDienCongDong", "DieuKhienBaiViet", "gọi", "1", "1", "assoc"),
            ("DieuKhienBaiViet", "ChuyenDi", "đọc", "1", "*", "assoc", (0.5, 1), (0.5, 0)),
            ("DieuKhienBaiViet", "BaiViet", "quản lý", "1", "*", "assoc"),
            ("BaiViet", "ChuyenDi", "liên kết", "*", "0..1", "assoc", (0, 0.85), (1, 0.4)),
            ("BaiViet", "BinhLuan", "", "1", "*", "comp"),
            ("BaiViet", "BinhChonBaiViet", "", "1", "*", "comp")],
           pos={"ChuyenDi": (330, 260), "BaiViet": (660, 20), "BinhLuan": (660, 300), "BinhChonBaiViet": (970, 20)})

    layout("DGM-CLS-007-tri-thuc.drawio",
           ("GiaoDienQuanTri", ["+chọnTríchXuất()", "+hiểnThịBảnNháp()", "+lưuTriThức()", "+hiểnThịLỗi()"]),
           [("DieuKhienQuanTri", ["+tríchXuấtTriThức()", "+chuyểnThànhTriThức()", "+kiểmTraQuyềnQuảnTrị()"]),
            ("DichVuTriThuc", ["+tạo()", "+cậpNhật()", "+bậtTắt()", "+địnhDạngChoNgữCảnhAI()"])],
           [("BaiViet", ["-id : bigint", "-tieuDe : varchar", "-noiDung : text"], ["+lấyNộiDung()",
                                                                                "+liênKếtTriThức()"]),
            ("TriThucDiemDen", ["-id : bigint", "-tenDiemDen : varchar", "-danhMuc : varchar", "-tieuDe : varchar",
                                "-noiDung : text", "-doUuTien : int", "-dangHoatDong : boolean"], ["+lưu()"])],
           [("GiaoDienQuanTri", "DieuKhienQuanTri", "gọi", "1", "1", "assoc"),
            ("DieuKhienQuanTri", "DichVuTriThuc", "sử dụng", "1", "1", "dep"),
            ("DieuKhienQuanTri", "BaiViet", "đọc", "1", "*", "assoc"),
            ("DichVuTriThuc", "TriThucDiemDen", "quản lý", "1", "*", "assoc"),
            ("BaiViet", "TriThucDiemDen", "chuyển thành", "*", "0..1", "assoc")],
           pos={"DichVuTriThuc": (330, 240), "BaiViet": (660, 20), "TriThucDiemDen": (660, 240)})
