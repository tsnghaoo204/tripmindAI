"""Ghép các chương nguồn (Markdown mở rộng) thành báo cáo DOCX theo định dạng PDF mẫu.

Cú pháp nguồn: xem scripts/README-build.md.
Chạy: python3 scripts/build_report.py  (từ thư mục thesis-output)
"""
import json
import re
import sys
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK, WD_LINE_SPACING
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
CH_DIR = ROOT / "chapters"
EXPORT = ROOT / "diagrams" / "export"
UI_EXPORT = ROOT / "ui-design" / "export"
SHOTS = ROOT / "assets" / "screenshots"
OUT = ROOT / "report" / "final-report.docx"
META = json.loads((CH_DIR / "meta.json").read_text(encoding="utf-8"))

ORDER = ["01-introduction.md", "02-theory.md", "03-requirements.md", "04-design.md",
         "05-implementation.md", "06-testing.md", "07-conclusion.md", "08-references.md"]

FONT = "Times New Roman"
TEXT_W_CM = 16.0
MAX_IMG_H_CM = 20.0

state = {"chapter": 0, "fig": 0, "tab": 0, "figs": {}, "tabs": {}, "placeholders": [], "warnings": []}


# ------------------------------------------------------------------ helpers
def set_cell_shading(cell, hex_color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), hex_color)
    tcPr.append(shd)


def set_table_borders(table, sz=4):
    tbl = table._tbl
    tblPr = tbl.tblPr
    borders = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        el = OxmlElement(f"w:{edge}")
        el.set(qn("w:val"), "single")
        el.set(qn("w:sz"), str(sz))
        el.set(qn("w:space"), "0")
        el.set(qn("w:color"), "000000")
        borders.append(el)
    tblPr.append(borders)


def set_repeat_header(row):
    trPr = row._tr.get_or_add_trPr()
    el = OxmlElement("w:tblHeader")
    el.set(qn("w:val"), "true")
    trPr.append(el)


def set_col_widths(table, widths_cm):
    """Cố định lưới cột (tblLayout, tblW, tblGrid, tcW theo gridSpan) để Word và LibreOffice tôn trọng."""
    table.autofit = False
    tbl = table._tbl
    tblPr = tbl.tblPr
    for tag in ("w:tblLayout", "w:tblW"):
        el = tblPr.find(qn(tag))
        if el is not None:
            tblPr.remove(el)
    lay = OxmlElement("w:tblLayout")
    lay.set(qn("w:type"), "fixed")
    tblPr.append(lay)
    tw = OxmlElement("w:tblW")
    tw.set(qn("w:w"), str(int(sum(widths_cm) * 567)))
    tw.set(qn("w:type"), "dxa")
    tblPr.append(tw)
    grid = tbl.find(qn("w:tblGrid"))
    for gc in list(grid):
        grid.remove(gc)
    for w in widths_cm:
        gc = OxmlElement("w:gridCol")
        gc.set(qn("w:w"), str(int(w * 567)))
        grid.append(gc)
    for tr in tbl.findall(qn("w:tr")):
        col = 0
        for tc in tr.findall(qn("w:tc")):
            tcPr = tc.get_or_add_tcPr()
            span_el = tcPr.find(qn("w:gridSpan"))
            span = int(span_el.get(qn("w:val"))) if span_el is not None else 1
            w = sum(widths_cm[col:col + span])
            tcW = tcPr.find(qn("w:tcW"))
            if tcW is None:
                tcW = OxmlElement("w:tcW")
                tcPr.insert(0, tcW)
            tcW.set(qn("w:w"), str(int(w * 567)))
            tcW.set(qn("w:type"), "dxa")
            col += span


def auto_widths(rows, total=TEXT_W_CM):
    n = len(rows[0])
    lens = [max(min(len(r[j]) if j < len(r) else 0, 60) for r in rows) for j in range(n)]
    weights = [max(l, 4) ** 0.8 for l in lens]
    ws = [total * w / sum(weights) for w in weights]
    ws = [max(w, 1.4) for w in ws]
    k = total / sum(ws)
    return [w * k for w in ws]


def add_field(paragraph, instr, placeholder=""):
    run = paragraph.add_run()
    b = OxmlElement("w:fldChar")
    b.set(qn("w:fldCharType"), "begin")
    b.set(qn("w:dirty"), "true")
    run._r.append(b)
    run2 = paragraph.add_run()
    it = OxmlElement("w:instrText")
    it.set(qn("xml:space"), "preserve")
    it.text = f" {instr} "
    run2._r.append(it)
    run3 = paragraph.add_run()
    s = OxmlElement("w:fldChar")
    s.set(qn("w:fldCharType"), "separate")
    run3._r.append(s)
    if placeholder:
        paragraph.add_run(placeholder)
    run4 = paragraph.add_run()
    e = OxmlElement("w:fldChar")
    e.set(qn("w:fldCharType"), "end")
    run4._r.append(e)


INLINE = re.compile(r"(\*\*[^*]+\*\*|\*[^*]+\*|\{fig:[A-Za-z0-9_-]+\}|\{tab:[A-Za-z0-9_-]+\})")


def resolve_ref(tok):
    kind, key = tok[1:-1].split(":", 1)
    table = state["figs"] if kind == "fig" else state["tabs"]
    if key not in table:
        state["warnings"].append(f"Tham chiếu tới {kind} không tồn tại: {key}")
        return "??"
    return table[key]


def add_inline(paragraph, text, size=None, bold=False, italic=False):
    for part in INLINE.split(text):
        if not part:
            continue
        b, i, t = bold, italic, part
        if part.startswith("**") and part.endswith("**"):
            b, t = True, part[2:-2]
        elif part.startswith("{fig:") or part.startswith("{tab:"):
            t = resolve_ref(part)
        elif part.startswith("*") and part.endswith("*") and len(part) > 2:
            i, t = True, part[1:-1]
        r = paragraph.add_run(t)
        r.bold = b or None
        r.italic = i or None
        if size:
            r.font.size = Pt(size)


def para(doc_or_cell, text="", style=None, align=None, indent=True, size=None, bold=False, italic=False,
         space_after=None, keep_next=False):
    p = doc_or_cell.add_paragraph(style=style) if style else doc_or_cell.add_paragraph()
    if text:
        add_inline(p, text, size=size, bold=bold, italic=italic)
    pf = p.paragraph_format
    if align is not None:
        p.alignment = align
    if not indent:
        pf.first_line_indent = Cm(0)
    if space_after is not None:
        pf.space_after = Pt(space_after)
    if keep_next:
        pf.keep_with_next = True
    return p


def cell_text(cell, text, bold=False, align=None, size=13, italic=False):
    cell.text = ""
    lines = text.split("<br>")
    for k, line in enumerate(lines):
        p = cell.paragraphs[0] if k == 0 else cell.add_paragraph()
        p.paragraph_format.first_line_indent = Cm(0)
        p.paragraph_format.space_after = Pt(0)
        p.paragraph_format.line_spacing = 1.15
        p.alignment = align if align is not None else WD_ALIGN_PARAGRAPH.LEFT
        add_inline(p, line, size=size, bold=bold, italic=italic)


# ------------------------------------------------------------------ styles & sections
def setup_styles(doc):
    st = doc.styles
    normal = st["Normal"]
    normal.font.name = FONT
    normal.font.size = Pt(14)
    normal.element.rPr.rFonts.set(qn("w:eastAsia"), FONT)
    pf = normal.paragraph_format
    pf.line_spacing = 1.5
    pf.space_before = Pt(0)
    pf.space_after = Pt(3)
    pf.first_line_indent = Cm(1.27)
    pf.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    specs = {"Heading 1": (16, True, False, WD_ALIGN_PARAGRAPH.CENTER, 12),
             "Heading 2": (14, True, False, WD_ALIGN_PARAGRAPH.LEFT, 6),
             "Heading 3": (14, True, False, WD_ALIGN_PARAGRAPH.LEFT, 6),
             "Heading 4": (14, True, True, WD_ALIGN_PARAGRAPH.LEFT, 6)}
    for name, (size, bold, italic, align, before) in specs.items():
        s = st[name]
        s.font.name = FONT
        s.font.size = Pt(size)
        s.font.bold = bold
        s.font.italic = italic
        s.font.color.rgb = RGBColor(0, 0, 0)
        rpr = s.element.get_or_add_rPr()
        rf = rpr.find(qn("w:rFonts"))
        if rf is None:
            rf = OxmlElement("w:rFonts")
            rpr.append(rf)
        for a in ("w:ascii", "w:hAnsi", "w:eastAsia", "w:cs"):
            rf.set(qn(a), FONT)
        for a in ("w:asciiTheme", "w:hAnsiTheme", "w:eastAsiaTheme", "w:cstheme"):
            if rf.get(qn(a)) is not None:
                del rf.attrib[qn(a)]
        s.paragraph_format.alignment = align
        s.paragraph_format.space_before = Pt(before)
        s.paragraph_format.space_after = Pt(6)
        s.paragraph_format.first_line_indent = Cm(0)
        s.paragraph_format.line_spacing = 1.5
        s.paragraph_format.keep_with_next = True
    for name in ("FigureCaption", "TableCaption"):
        s = st.add_style(name, 1)
        s.base_style = normal
        s.font.italic = True
        s.font.size = Pt(14)
        s.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
        s.paragraph_format.first_line_indent = Cm(0)
        s.paragraph_format.space_after = Pt(6)
    st["TableCaption"].paragraph_format.keep_with_next = True
    for lvl in (1, 2, 3):
        try:
            s = st[f"TOC {lvl}"]
        except KeyError:
            s = st.add_style(f"TOC {lvl}", 1)
        s.base_style = normal
        s.paragraph_format.first_line_indent = Cm(0)
        s.paragraph_format.left_indent = Cm(0.5 * (lvl - 1))
        s.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.LEFT


def set_margins(section):
    section.page_width, section.page_height = Cm(21), Cm(29.7)
    section.top_margin = Cm(2)
    section.bottom_margin = Cm(2)
    section.left_margin = Cm(3)
    section.right_margin = Cm(2)
    section.header_distance = Cm(1.0)


def set_page_numbering(section, fmt, start=1):
    sectPr = section._sectPr
    pg = sectPr.find(qn("w:pgNumType"))
    if pg is None:
        pg = OxmlElement("w:pgNumType")
        sectPr.append(pg)
    pg.set(qn("w:fmt"), fmt)
    pg.set(qn("w:start"), str(start))
    section.header.is_linked_to_previous = False
    hdr = section.header
    p = hdr.paragraphs[0]
    p.text = ""
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.first_line_indent = Cm(0)
    add_field(p, "PAGE", "1")


def set_page_border(section, style="single", sz=8, color="000000", space=24):
    sectPr = section._sectPr
    b = OxmlElement("w:pgBorders")
    b.set(qn("w:offsetFrom"), "page")
    for edge in ("top", "left", "bottom", "right"):
        el = OxmlElement(f"w:{edge}")
        el.set(qn("w:val"), style)
        el.set(qn("w:sz"), str(sz))
        el.set(qn("w:space"), str(space))
        el.set(qn("w:color"), color)
        b.append(el)
    # pgBorders phải đứng trước các phần tử khác theo schema; chèn sau pgMar
    pgMar = sectPr.find(qn("w:pgMar"))
    pgMar.addnext(b)


def clear_page_border(section):
    for b in section._sectPr.findall(qn("w:pgBorders")):
        section._sectPr.remove(b)


def clear_header(section):
    section.header.is_linked_to_previous = False
    for p in section.header.paragraphs:
        p.text = ""


# ------------------------------------------------------------------ cover pages
def cover_outer(doc):
    m = META
    t = doc.add_table(rows=1, cols=2)
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(t, sz=8)
    set_col_widths(t, [2.6, 13.4])
    tr = t.rows[0]._tr
    trPr = tr.get_or_add_trPr()
    h = OxmlElement("w:trHeight")
    h.set(qn("w:val"), str(int(Cm(25).twips if hasattr(Cm(25), "twips") else 14170)))
    h.set(qn("w:hRule"), "exact")
    trPr.append(h)
    left, right = t.rows[0].cells
    tcPr = left._tc.get_or_add_tcPr()
    td = OxmlElement("w:textDirection")
    td.set(qn("w:val"), "btLr")
    tcPr.append(td)
    left.text = ""
    lp = left.paragraphs[0]
    lp.paragraph_format.first_line_indent = Cm(0)
    lp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = lp.add_run(f"{m['major'].upper()}" + " " * 40 + f"{m['student'].upper()}")
    r.font.size = Pt(14)
    right.text = ""
    lines = [("BỘ CÔNG THƯƠNG", True), ("ĐẠI HỌC CÔNG NGHIỆP HÀ NỘI", True), ("---------------------------------------", False),
             ("", False), ("", False), ("ĐỒ ÁN TỐT NGHIỆP", False), (f"NGÀNH {m['major'].upper()}", False), ("", False),
             ("", False), ("ĐỀ TÀI:", True), (m["title"].upper(), True), ("", False), ("", False), ("", False)]
    first = True
    for text, bold in lines:
        p = right.paragraphs[0] if first else right.add_paragraph()
        first = False
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.first_line_indent = Cm(0)
        r = p.add_run(text)
        r.bold = bold
    for k, v in [("GVHD:", m["advisor"]), ("Sinh viên:", m["student"]), ("Mã sinh viên:", m["student_id"])]:
        p = right.add_paragraph()
        p.paragraph_format.first_line_indent = Cm(0)
        p.paragraph_format.left_indent = Cm(1.5)
        p.paragraph_format.tab_stops.add_tab_stop(Cm(5.3))
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        r = p.add_run(f"{k}\t{v}")
        r.bold = True
    for _ in range(4):
        p = right.add_paragraph()
        p.paragraph_format.first_line_indent = Cm(0)
    p = right.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.first_line_indent = Cm(0)
    p.add_run(f"Hà Nội - Năm {m['year']}")


def cover_inner(doc):
    m = META
    def c(text, bold=False, size=14, color=None, after=0):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.first_line_indent = Cm(0)
        p.paragraph_format.space_after = Pt(after)
        r = p.add_run(text)
        r.bold = bold
        r.font.size = Pt(size)
        if color:
            r.font.color.rgb = RGBColor.from_string(color)
        return p
    c("BỘ CÔNG THƯƠNG", True, 15)
    c("ĐẠI HỌC CÔNG NGHIỆP HÀ NỘI", True, 15)
    c("---------------------------------------", after=12)
    p = c("[LOGO TRƯỜNG — người dùng bổ sung]", size=12, color="C00000", after=24)
    p.runs[0].italic = True
    state["placeholders"].append(("Bìa trong", "Logo trường/khoa", "Chèn logo chính thức tại vị trí đánh dấu"))
    c("")
    c("ĐỒ ÁN TỐT NGHIỆP ĐẠI HỌC", True, 16)
    c(f"NGÀNH {m['major'].upper()}", True, 16, after=36)
    c(m["title"].upper(), True, 16, color="2F5597", after=48)
    info = doc.add_table(rows=4, cols=2)
    info.alignment = WD_TABLE_ALIGNMENT.CENTER
    for i, (k, v) in enumerate([("GVHD:", m["advisor"]), ("Sinh viên:", m["student"]),
                                ("Mã sinh viên:", m["student_id"]), ("Lớp:", m["class"])]):
        cell_text(info.rows[i].cells[0], k, bold=True, size=14)
        cell_text(info.rows[i].cells[1], v, size=14)
    set_col_widths(info, [4.0, 8.0])
    for _ in range(7):
        c("")
    c(f"Hà Nội, {m['year']}", True)


# ------------------------------------------------------------------ figures / tables
def find_image(fid):
    for base in (EXPORT, UI_EXPORT, SHOTS):
        for ext in (".png", ".jpg", ".jpeg"):
            f = base / f"{fid}{ext}"
            if f.exists():
                return f
    return None


def add_figure(doc, fid, caption):
    num = state["figs"][fid]
    img = find_image(fid)
    if img:
        with Image.open(img) as im:
            w, h = im.size
        width = TEXT_W_CM
        if width * h / w > MAX_IMG_H_CM:
            width = MAX_IMG_H_CM * w / h
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.first_line_indent = Cm(0)
        p.paragraph_format.keep_with_next = True
        p.paragraph_format.line_spacing = 1.0
        p.add_run().add_picture(str(img), width=Cm(width))
    else:
        t = doc.add_table(rows=1, cols=1)
        t.alignment = WD_TABLE_ALIGNMENT.CENTER
        set_table_borders(t, sz=8)
        cell = t.rows[0].cells[0]
        set_cell_shading(cell, "F2F2F2")
        cell_text(cell, f"[ẢNH CẦN BỔ SUNG] {caption}", italic=True, align=WD_ALIGN_PARAGRAPH.CENTER, size=12)
        for _ in range(4):
            cell.add_paragraph()
        for cp_ in cell.paragraphs:
            cp_.paragraph_format.keep_with_next = True
        trPr = t.rows[0]._tr.get_or_add_trPr()
        cs = OxmlElement("w:cantSplit")
        cs.set(qn("w:val"), "true")
        trPr.append(cs)
        state["placeholders"].append((f"Hình {num}", fid, caption))
    cp = doc.add_paragraph(style="FigureCaption")
    cp.add_run(f"Hình {num}. {caption}")


def add_md_table(doc, key, caption, rows, widths=None):
    if caption:
        cp = doc.add_paragraph(style="TableCaption")
        cp.add_run(f"Bảng {state['tabs'][key]}. {caption}")
    header = rows[0]
    t = doc.add_table(rows=len(rows), cols=len(header))
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(t)
    for i, r in enumerate(rows):
        for j in range(len(header)):
            txt = r[j] if j < len(r) else ""
            al = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else None
            cell_text(t.rows[i].cells[j], txt, bold=(i == 0), align=al)
    set_repeat_header(t.rows[0])
    set_col_widths(t, widths or auto_widths(rows))
    doc.add_paragraph().paragraph_format.space_after = Pt(0)


def merge_row(row, start, end):
    a = row.cells[start]
    return a.merge(row.cells[end])


def add_usecase(doc, key, caption, spec):
    cp = doc.add_paragraph(style="TableCaption")
    cp.add_run(f"Bảng {state['tabs'][key]}. {caption}")
    order = [("Tác nhân", "actors"), ("Mô tả", "desc"), ("Sự kiện kích hoạt chức năng", "trigger"),
             ("Tiền điều kiện", "pre"), ("Luồng sự kiện chính", "main"), ("Luồng sự kiện thay thế", "alt"),
             ("Hậu điều kiện", "post")]
    t = doc.add_table(rows=1 + len(order), cols=4)
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(t)
    r0 = t.rows[0].cells
    cell_text(r0[0], "Tên use case")
    cell_text(r0[1], spec.get("name", ""))
    cell_text(r0[2], "Mã Use Case")
    cell_text(r0[3], spec.get("code", ""))
    for i, (label, k) in enumerate(order, start=1):
        row = t.rows[i]
        cell_text(row.cells[0], label)
        c = merge_row(row, 1, 3)
        if k in ("main", "alt"):
            c.text = ""
            flows = spec.get(k, [])
            nt = c.add_table(rows=1, cols=3)
            set_table_borders(nt)
            cell_text(nt.rows[0].cells[0], "#", bold=True)
            cell_text(nt.rows[0].cells[1], "Thực hiện bởi", bold=True)
            cell_text(nt.rows[0].cells[2], "Hành động", bold=True)
            for item in flows:
                nr = nt.add_row()
                if isinstance(item, str):
                    m = nr.cells[0].merge(nr.cells[2])
                    cell_text(m, item, align=WD_ALIGN_PARAGRAPH.CENTER)
                    set_cell_shading(m, "BDD7EE")
                else:
                    for j in range(3):
                        cell_text(nr.cells[j], item[j])
            set_col_widths(nt, [1.1, 2.6, 8.8])
            # bỏ đoạn trống đầu ô do python-docx tạo
            first = c.paragraphs[0]
            if not first.text:
                first._p.getparent().remove(first._p)
            c.add_paragraph().paragraph_format.space_after = Pt(0)
        else:
            cell_text(c, spec.get(k, ""))
    set_col_widths(t, [3.0, 5.5, 3.5, 4.0])
    doc.add_paragraph().paragraph_format.space_after = Pt(0)


def add_testcase(doc, key, caption, spec):
    cp = doc.add_paragraph(style="TableCaption")
    cp.add_run(f"Bảng {state['tabs'][key]}. {caption}")
    rows = spec["rows"]
    t = doc.add_table(rows=4 + len(rows), cols=5)
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(t)
    for i, (k, v) in enumerate([("ID", spec["id"]), ("Mô tả", spec["desc"]), ("Nhánh", spec["branch"])]):
        cell_text(t.rows[i].cells[0], k)
        cell_text(merge_row(t.rows[i], 1, 4), v)
    for j, h in enumerate(["STT", "Kịch bản", "Dữ liệu vào", "Đầu ra kỳ vọng", "Đầu ra thực tế"]):
        cell_text(t.rows[3].cells[j], h, bold=True)
    for i, r in enumerate(rows):
        for j in range(5):
            cell_text(t.rows[4 + i].cells[j], r[j] if j < len(r) else "")
    set_col_widths(t, [1.7, 3.0, 3.5, 4.3, 3.5])
    doc.add_paragraph().paragraph_format.space_after = Pt(0)


# ------------------------------------------------------------------ parser
def parse_block(lines, i):
    """Đọc khối @usecase/@testcase tới @end: trả về (spec, i_next)."""
    spec, cur = {}, None
    while i < len(lines) and lines[i].strip() != "@end":
        ln = lines[i].rstrip("\n")
        m = re.match(r"^(\w+):\s*(.*)$", ln)
        if m and m.group(1) in ("name", "code", "actors", "desc", "trigger", "pre", "post", "main", "alt",
                                "id", "branch", "rows"):
            cur = m.group(1)
            if cur in ("main", "alt", "rows"):
                spec[cur] = []
            else:
                spec[cur] = m.group(2)
        elif ln.strip().startswith("=="):
            spec[cur].append(ln.strip()[2:].strip())
        elif "|" in ln and cur in ("main", "alt", "rows"):
            spec[cur].append([c.strip() for c in ln.strip().strip("|").split("|")])
        elif ln.strip() and cur:
            if isinstance(spec[cur], str):
                spec[cur] += "<br>" + ln.strip()
        i += 1
    return spec, i + 1


FIG_RE = re.compile(r"^@fig\{([^}]+)\}\{(.+)\}\s*$")
TAB_RE = re.compile(r"^@(table|usecase|testcase)\[([^\]]+)\]\{(.+?)\}(?:\{([0-9., ]+)\})?\s*$")


def prescan(texts):
    """Gán số hình/bảng theo thứ tự xuất hiện cuối cùng."""
    chap = 0
    for text in texts:
        for ln in text.splitlines():
            if ln.startswith("# "):
                m = re.match(r"^# CHƯƠNG (\d+)", ln)
                if m:
                    chap = int(m.group(1))
                    fig = tab = 0
            mf = FIG_RE.match(ln)
            if mf:
                fig += 1
                state["figs"][mf.group(1)] = f"{chap}.{fig}"
            mt = TAB_RE.match(ln)
            if mt:
                tab += 1
                state["tabs"][mt.group(2)] = f"{chap}.{tab}"


def render(doc, text):
    lines = text.splitlines()
    i = 0
    while i < len(lines):
        ln = lines[i]
        s = ln.strip()
        if not s or s.startswith("<!--"):
            if s.startswith("<!--"):
                while "-->" not in lines[i]:
                    i += 1
            i += 1
            continue
        if s == "@pagebreak":
            doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)
            i += 1
            continue
        m = re.match(r"^(#{1,4}) (.+)$", s)
        if m:
            level = len(m.group(1))
            h = doc.add_heading(level=level)
            add_inline(h, m.group(2))
            if level == 1:
                h.paragraph_format.page_break_before = True
            i += 1
            continue
        mf = FIG_RE.match(s)
        if mf:
            add_figure(doc, mf.group(1), mf.group(2))
            i += 1
            continue
        mt = TAB_RE.match(s)
        if mt:
            kind, key, caption, widths = mt.groups()
            widths = [float(w) for w in widths.split(",")] if widths else None
            if kind == "table":
                rows = []
                i += 1
                while i < len(lines) and lines[i].strip().startswith("|"):
                    cells = [c.strip() for c in lines[i].strip().strip("|").split("|")]
                    if not all(re.match(r"^:?-{2,}:?$", c) for c in cells):
                        rows.append(cells)
                    i += 1
                add_md_table(doc, key, caption, rows, widths)
            else:
                spec, i = parse_block(lines, i + 1)
                (add_usecase if kind == "usecase" else add_testcase)(doc, key, caption, spec)
            continue
        if s.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                cells = [c.strip() for c in lines[i].strip().strip("|").split("|")]
                if not all(re.match(r"^:?-{2,}:?$", c) for c in cells):
                    rows.append(cells)
                i += 1
            add_md_table(doc, None, None, rows)
            continue
        mb = re.match(r"^(\s*)([-+•]) (.+)$", ln)
        if mb:
            depth = len(mb.group(1)) // 2
            sym = {"-": "-", "+": "+", "•": "•"}[mb.group(2)]
            p = doc.add_paragraph()
            pf = p.paragraph_format
            pf.left_indent = Cm(1.0 + depth * 0.75)
            pf.first_line_indent = Cm(-0.5)
            p.add_run(f"{sym}\t")
            pf.tab_stops.add_tab_stop(Cm(1.0 + depth * 0.75))
            add_inline(p, mb.group(3))
            if mb.group(3).rstrip("*").endswith(":"):
                pf.keep_with_next = True
            i += 1
            continue
        if s.startswith(">"):
            p = para(doc, s[1:].strip(), align=WD_ALIGN_PARAGRAPH.CENTER, indent=False, italic=True)
            i += 1
            continue
        # đoạn văn: gộp các dòng liền nhau
        buf = [s]
        i += 1
        while i < len(lines) and lines[i].strip() and not re.match(r"^(#|@|\||\s*[-+•] |>)", lines[i].strip()):
            buf.append(lines[i].strip())
            i += 1
        para(doc, " ".join(buf))


def unnumbered_heading(doc, text, toc=True):
    h = doc.add_heading(level=1) if toc else doc.add_paragraph()
    r = h.add_run(text)
    if not toc:
        r.bold = True
        r.font.size = Pt(16)
        h.alignment = WD_ALIGN_PARAGRAPH.CENTER
        h.paragraph_format.first_line_indent = Cm(0)
    return h


def build():
    texts = [(CH_DIR / f).read_text(encoding="utf-8") for f in ORDER]
    front = (CH_DIR / "00-front-matter.md").read_text(encoding="utf-8")
    prescan(texts)

    doc = Document()
    setup_styles(doc)
    s0 = doc.sections[0]
    set_margins(s0)
    clear_header(s0)
    cover_outer(doc)

    s1 = doc.add_section(WD_SECTION.NEW_PAGE)
    clear_page_border(s1)
    set_margins(s1)
    clear_header(s1)
    set_page_border(s1, "thinThickSmallGap", 24, "1F3864", 24)
    cover_inner(doc)

    s2 = doc.add_section(WD_SECTION.NEW_PAGE)
    clear_page_border(s2)
    set_margins(s2)
    set_page_numbering(s2, "lowerRoman", 1)
    # front matter: LỜI NÓI ĐẦU + DANH MỤC VIẾT TẮT (từ nguồn), MỤC LỤC/HÌNH/BẢNG (trường Word)
    parts = re.split(r"^@section (.+)$", front, flags=re.M)
    blocks = {parts[k].strip(): parts[k + 1] for k in range(1, len(parts), 2)}
    h = unnumbered_heading(doc, "LỜI NÓI ĐẦU")
    h.paragraph_format.page_break_before = False
    render(doc, blocks["LỜI NÓI ĐẦU"])

    h = unnumbered_heading(doc, "MỤC LỤC")
    h.paragraph_format.page_break_before = True
    p = doc.add_paragraph()
    p.paragraph_format.first_line_indent = Cm(0)
    add_field(p, 'TOC \\o "1-3" \\h \\z \\u', "[Mục lục — cập nhật trường để hiển thị]")

    h = unnumbered_heading(doc, "DANH MỤC CÁC THUẬT NGỮ, KÝ HIỆU VÀ CÁC CHỮ VIẾT TẮT")
    h.paragraph_format.page_break_before = True
    render(doc, blocks["DANH MỤC VIẾT TẮT"])

    h = unnumbered_heading(doc, "DANH MỤC HÌNH ẢNH")
    h.paragraph_format.page_break_before = True
    p = doc.add_paragraph()
    p.paragraph_format.first_line_indent = Cm(0)
    add_field(p, 'TOC \\h \\z \\t "FigureCaption,1"', "[Danh mục hình — cập nhật trường]")

    h = unnumbered_heading(doc, "DANH MỤC BẢNG BIỂU")
    h.paragraph_format.page_break_before = True
    p = doc.add_paragraph()
    p.paragraph_format.first_line_indent = Cm(0)
    add_field(p, 'TOC \\h \\z \\t "TableCaption,1"', "[Danh mục bảng — cập nhật trường]")

    s3 = doc.add_section(WD_SECTION.NEW_PAGE)
    clear_page_border(s3)
    set_margins(s3)
    set_page_numbering(s3, "decimal", 1)
    first = True
    for text in texts:
        if first:
            # tiêu đề đầu tiên của thân bài không cần page break (đã có section break)
            text = text.replace("\n", "\n", 1)
        render(doc, text)
        first = False
    # bỏ page_break_before ở heading đầu tiên sau section break
    body_heads = [p for p in doc.paragraphs if p.style.name == "Heading 1" and p.text.strip() == "MỞ ĐẦU"]
    for p in body_heads:
        p.paragraph_format.page_break_before = False

    settings = doc.settings.element
    uf = OxmlElement("w:updateFields")
    uf.set(qn("w:val"), "true")
    settings.append(uf)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    doc.save(OUT)

    rep = {"figures": state["figs"], "tables": state["tabs"], "placeholders": state["placeholders"],
           "warnings": state["warnings"]}
    (ROOT / "qa").mkdir(exist_ok=True)
    (ROOT / "qa" / "build-state.json").write_text(json.dumps(rep, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"OK {OUT} — {len(state['figs'])} hình, {len(state['tabs'])} bảng, "
          f"{len(state['placeholders'])} placeholder, {len(state['warnings'])} cảnh báo")
    for w in state["warnings"]:
        print("  WARN", w)


if __name__ == "__main__":
    sys.exit(build())
