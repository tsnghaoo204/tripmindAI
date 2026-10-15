"""Thư viện sinh sơ đồ draw.io (XML gốc, từng phần tử chỉnh sửa được) theo phong cách PDF mẫu."""
from xml.sax.saxutils import escape, quoteattr

YELLOW = "#FFF2CC"
YELLOW_LIGHT = "#FFFFE6"
STROKE = "#000000"
ERD_HEAD = "#2F5597"
FONT = "fontFamily=Arial;"


class Doc:
    def __init__(self, name, page_w=1600, page_h=1200):
        self.name = name
        self.cells = []
        self.counter = 0
        self.page_w, self.page_h = page_w, page_h

    def _id(self, prefix):
        self.counter += 1
        return f"{prefix}{self.counter}"

    def vertex(self, value, x, y, w, h, style, parent="1", prefix="v"):
        cid = self._id(prefix)
        self.cells.append(
            f'<mxCell id="{cid}" value={quoteattr(value)} style={quoteattr(style)} vertex="1" parent="{parent}">'
            f'<mxGeometry x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" as="geometry"/></mxCell>')
        return cid

    def edge(self, src=None, tgt=None, value="", style="", points=None, sp=None, tp=None, parent="1", prefix="e"):
        cid = self._id(prefix)
        attrs = f'id="{cid}" value={quoteattr(value)} style={quoteattr(style)} edge="1" parent="{parent}"'
        if src:
            attrs += f' source="{src}"'
        if tgt:
            attrs += f' target="{tgt}"'
        geo = '<mxGeometry relative="1" as="geometry">'
        if sp:
            geo += f'<mxPoint x="{sp[0]:.1f}" y="{sp[1]:.1f}" as="sourcePoint"/>'
        if tp:
            geo += f'<mxPoint x="{tp[0]:.1f}" y="{tp[1]:.1f}" as="targetPoint"/>'
        if points:
            geo += '<Array as="points">' + "".join(f'<mxPoint x="{px:.1f}" y="{py:.1f}"/>' for px, py in points) + '</Array>'
        geo += '</mxGeometry>'
        self.cells.append(f'<mxCell {attrs}>{geo}</mxCell>')
        return cid

    def line(self, x1, y1, x2, y2, style="endArrow=none;html=1;strokeColor=#000000;"):
        return self.edge(style=style, sp=(x1, y1), tp=(x2, y2))

    def text(self, value, x, y, w, h, size=12, align="left", bold=False, italic=False, color="#000000", extra=""):
        fs = (1 if bold else 0) + (2 if italic else 0)
        style = (f"text;html=1;{FONT}fontSize={size};align={align};verticalAlign=middle;whiteSpace=wrap;"
                 f"fontStyle={fs};fontColor={color};strokeColor=none;fillColor=none;{extra}")
        return self.vertex(value, x, y, w, h, style, prefix="t")

    def to_xml(self):
        body = "".join(self.cells)
        return ('<mxfile host="drawio" type="device">'
                f'<diagram id="{escape(self.name)}" name="{escape(self.name)}">'
                f'<mxGraphModel dx="1400" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" '
                f'arrows="1" fold="1" page="0" pageScale="1" pageWidth="{self.page_w}" pageHeight="{self.page_h}" '
                f'background="#FFFFFF" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>'
                f'{body}</root></mxGraphModel></diagram></mxfile>')

    def save(self, path):
        with open(path, "w", encoding="utf-8") as f:
            f.write(self.to_xml())


# ---------------------------------------------------------------- Use case
def uc_actor(d, label, x, y):
    return d.vertex(label, x, y, 34, 64,
                    f"shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;"
                    f"{FONT}fontSize=12;fillColor={YELLOW};strokeColor={STROKE};whiteSpace=nowrap;", prefix="actor")


def uc_case(d, label, x, y, w=180, h=50, size=12):
    return d.vertex(label, x, y, w, h,
                    f"ellipse;whiteSpace=wrap;html=1;{FONT}fontSize={size};fillColor={YELLOW};strokeColor={STROKE};",
                    prefix="uc")


def uc_assoc(d, a, b, points=None, extra=""):
    return d.edge(a, b, style="endArrow=none;html=1;strokeColor=#000000;" + extra, points=points)


def uc_dep(d, a, b, kind="include", points=None):
    return d.edge(a, b, value=f"&lt;&lt;{kind}&gt;&gt;",
                  style=f"endArrow=open;endSize=10;dashed=1;html=1;strokeColor=#000000;{FONT}fontSize=11;"
                        "labelBackgroundColor=#FFFFFF;", points=points)


def uc_gen(d, child, parent, points=None, extra=""):
    return d.edge(child, parent, style="endArrow=block;endFill=0;endSize=12;html=1;strokeColor=#000000;" + extra,
                  points=points)


def uc_boundary(d, label, x, y, w, h):
    return d.vertex(label, x, y, w, h,
                    f"rounded=0;whiteSpace=wrap;html=1;fillColor=none;strokeColor={STROKE};verticalAlign=top;"
                    f"{FONT}fontSize=13;fontStyle=1;", prefix="sys")


# ---------------------------------------------------------------- Activity
class Lanes:
    """Swimlane: header + thân, toạ độ cột trung tâm mỗi lane."""

    def __init__(self, d, names, widths, x=0, y=0, height=800, head=28):
        self.d, self.x, self.y, self.head = d, x, y, head
        self.bounds = []
        cx = x
        for name, w in zip(names, widths):
            d.vertex(name, cx, y, w, head, f"rounded=0;whiteSpace=wrap;html=1;{FONT}fontSize=12;fillColor=#FFFFFF;"
                                           f"strokeColor={STROKE};", prefix="lh")
            d.vertex("", cx, y + head, w, height - head,
                     f"rounded=0;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor={STROKE};", prefix="lb")
            self.bounds.append((cx, w))
            cx += w
        self.width = cx - x

    def cx(self, lane, frac=0.5):
        x0, w = self.bounds[lane]
        return x0 + w * frac


def act_start(d, cx, y):
    return d.vertex("", cx - 12, y, 24, 24, f"ellipse;html=1;fillColor=#000000;strokeColor={STROKE};", prefix="st")


def act_end(d, cx, y):
    return d.vertex("", cx - 14, y, 28, 28,
                    "ellipse;html=1;shape=endState;fillColor=#000000;strokeColor=#000000;", prefix="en")


def act_action(d, label, cx, y, w=170, h=44):
    return d.vertex(label, cx - w / 2, y, w, h,
                    f"rounded=1;arcSize=30;whiteSpace=wrap;html=1;{FONT}fontSize=13;fillColor={YELLOW};"
                    f"strokeColor={STROKE};", prefix="a")


def act_decision(d, cx, y, label="", s=34):
    did = d.vertex("", cx - s / 2, y, s, s, f"rhombus;html=1;fillColor={YELLOW};strokeColor={STROKE};", prefix="dc")
    if label:
        d.text(label, cx - s / 2 - 142, y - 18, 140, 24, size=12, align="right")
    return did


def act_bar(d, cx, y, w=160, horizontal=True):
    if horizontal:
        return d.vertex("", cx - w / 2, y, w, 6, "rounded=0;html=1;fillColor=#000000;strokeColor=#000000;", prefix="bar")
    return d.vertex("", cx - 3, y, 6, w, "rounded=0;html=1;fillColor=#000000;strokeColor=#000000;", prefix="bar")


def act_flow(d, a, b, label="", points=None, exit=None, entry=None):
    style = (f"edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow=open;endSize=8;strokeColor={STROKE};"
             f"{FONT}fontSize=12;labelBackgroundColor=#FFFFFF;")
    if exit:
        style += f"exitX={exit[0]};exitY={exit[1]};exitDx=0;exitDy=0;"
    if entry:
        style += f"entryX={entry[0]};entryY={entry[1]};entryDx=0;entryDy=0;"
    return d.edge(a, b, value=label, style=style, points=points)


# ---------------------------------------------------------------- Sequence
SEQ_SHAPES = {"actor": "umlActor", "boundary": "umlBoundary", "control": "umlControl", "entity": "umlEntity"}


class Seq:
    def __init__(self, name, participants, gap=135, top=20, step=32):
        """participants: list (key, label, kind) với kind ∈ actor|boundary|control|entity."""
        self.d = Doc(name)
        self.p = {}
        self.order = []
        self.gap, self.top, self.step = gap, top, step
        for i, (k, label, kind) in enumerate(participants):
            self.p[k] = dict(x=60 + i * gap, label=label, kind=kind)
            self.order.append(k)
        self.y = top + 110
        self.ops = []
        self.frames = []
        self.stack = []
        self.n = 0

    def _x(self, k):
        return self.p[k]["x"]

    def msg(self, a, b, label, ret=False):
        self.n += 1
        self.ops.append(("msg", a, b, f"{self.n}: {label}", ret, self.y))
        self.y += self.step

    def ret(self, a, b, label="trả về kết quả"):
        self.msg(a, b, label, ret=True)

    def self_(self, a, label):
        self.n += 1
        self.ops.append(("self", a, a, f"{self.n}: {label}", False, self.y))
        self.y += self.step + 10

    def note(self, a, label):
        self.ops.append(("note", a, a, label, False, self.y))
        self.y += 40

    def alt_begin(self, kind, guard, keys):
        self.y += 8
        fr = dict(kind=kind, keys=keys, start=self.y, guards=[(self.y, guard)], depth=len(self.stack))
        self.stack.append(fr)
        self.y += 40

    def alt_else(self, guard):
        fr = self.stack[-1]
        self.y += 4
        fr["guards"].append((self.y, guard))
        self.y += 40

    def alt_end(self):
        fr = self.stack.pop()
        self.y += 6
        fr["end"] = self.y
        self.frames.append(fr)
        self.y += 24

    def build(self):
        d = self.d
        bottom = self.y + 20
        for k in self.order:
            p = self.p[k]
            shape = SEQ_SHAPES[p["kind"]]
            w = 40 if p["kind"] == "actor" else 50
            hh = 60 if p["kind"] == "actor" else 50
            d.vertex(p["label"], p["x"] - w / 2, self.top, w, hh,
                     f"shape={shape};html=1;verticalLabelPosition=bottom;verticalAlign=top;labelPosition=center;labelWidth=128;"
                     f"align=center;{FONT}fontSize=12;fillColor={YELLOW};strokeColor={STROKE};whiteSpace=wrap;",
                     prefix="head")
            y0 = self.top + hh + 34
            if p["kind"] == "actor":
                d.vertex("", p["x"] - 5, y0, 10, bottom - y0,
                         f"rounded=0;html=1;fillColor={YELLOW};strokeColor={STROKE};", prefix="act")
            else:
                d.line(p["x"], y0, p["x"], bottom, "endArrow=none;dashed=1;html=1;strokeColor=#000000;")
        for fr in self.frames:
            xs = [self._x(k) for k in fr["keys"]]
            pad = 70 - fr["depth"] * 10
            x0, x1 = min(xs) - pad, max(xs) + pad
            d.vertex(fr["kind"], x0, fr["start"], x1 - x0, fr["end"] - fr["start"],
                     f"shape=umlFrame;whiteSpace=wrap;html=1;width=44;height=20;{FONT}fontSize=11;fontStyle=1;"
                     f"fillColor=none;strokeColor={STROKE};swimlaneFillColor=none;", prefix="frame")
            for i, (gy, guard) in enumerate(fr["guards"]):
                if i > 0:
                    d.line(x0, gy, x1, gy, "endArrow=none;dashed=1;html=1;strokeColor=#000000;")
                d.text(f"[{guard}]", x0 + 48, gy + 2, 300, 20, size=11)
        for op, a, b, label, ret, y in self.ops:
            if op == "note":
                x = self._x(a)
                d.vertex(label, x - 210, y, 150, 32,
                         f"shape=note;size=10;whiteSpace=wrap;html=1;{FONT}fontSize=11;fillColor={YELLOW};"
                         f"strokeColor={STROKE};align=left;spacingLeft=4;", prefix="note")
                continue
            xa, xb = self._x(a), self._x(b)
            if op == "self":
                d.vertex("", xa - 4, y - 4, 8, 34, f"rounded=0;html=1;fillColor={YELLOW};strokeColor={STROKE};",
                         prefix="act")
                d.edge(value=label, style=f"html=1;endArrow=block;endFill=1;endSize=7;strokeColor={STROKE};"
                                         f"{FONT}fontSize=11;align=left;spacingLeft=4;rounded=0;labelBackgroundColor=none;",
                       sp=(xa + 4, y), tp=(xa + 4, y + 22), points=[(xa + 40, y), (xa + 40, y + 22)])
                continue
            if not ret and self.p[b]["kind"] != "actor":
                d.vertex("", xb - 4, y - 4, 8, 22, f"rounded=0;html=1;fillColor={YELLOW};strokeColor={STROKE};",
                         prefix="act")
            off = 4 if xb > xa else -4
            if ret:
                style = f"html=1;endArrow=open;endSize=7;dashed=1;strokeColor={STROKE};"
            else:
                style = f"html=1;endArrow=block;endFill=1;endSize=7;strokeColor={STROKE};"
            style += f"{FONT}fontSize=11;verticalAlign=bottom;labelBackgroundColor=none;"
            d.edge(value=label, style=style, sp=(xa + off, y), tp=(xb - off, y))
        return d


# ---------------------------------------------------------------- Class
def uml_class(d, name, x, y, w=220, attrs=(), methods=(), stereo=None, row=18):
    head = 40 if stereo else 26
    title = (f"&lt;&lt;{stereo}&gt;&gt;<br><b>{name}</b>" if stereo else f"<b>{name}</b>")
    n_a, n_m = max(len(attrs), 0), max(len(methods), 0)
    h = head + row * n_a + 8 + row * n_m + (6 if n_m else 0)
    cid = d.vertex(title, x, y, w, h,
                   f"swimlane;html=1;fontStyle=0;childLayout=stackLayout;horizontal=1;startSize={head};"
                   f"horizontalStack=0;resizeParent=1;resizeLast=0;collapsible=0;marginBottom=0;{FONT}fontSize=11;"
                   f"fillColor={YELLOW};swimlaneFillColor={YELLOW_LIGHT};strokeColor={STROKE};whiteSpace=wrap;",
                   prefix="cls")
    cy = head
    for a in attrs:
        d.vertex(escape(a), 0, cy, w, row, f"text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;"
                                           f"spacingLeft=4;{FONT}fontSize=10;whiteSpace=wrap;", parent=cid, prefix="ca")
        cy += row
    d.vertex("", 0, cy, w, 8, "line;strokeWidth=1;fillColor=none;align=left;verticalAlign=middle;spacingTop=-1;"
                              "spacingLeft=3;spacingRight=3;rotatable=0;labelPosition=right;points=[];portConstraint=eastwest;"
                              "strokeColor=#000000;", parent=cid, prefix="cl")
    cy += 8
    for m in methods:
        d.vertex(escape(m), 0, cy, w, row, f"text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;"
                                           f"spacingLeft=4;{FONT}fontSize=10;whiteSpace=wrap;", parent=cid, prefix="cm")
        cy += row
    return cid


def uml_assoc(d, a, b, label="", m1="", m2="", points=None, kind="assoc", exit=None, entry=None):
    ends = {"assoc": "endArrow=none;", "dep": "endArrow=open;dashed=1;", "nav": "endArrow=open;",
            "comp": "endArrow=none;startArrow=diamondThin;startFill=1;startSize=14;",
            "agg": "endArrow=none;startArrow=diamondThin;startFill=0;startSize=14;"}[kind]
    style = (f"edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;{ends}strokeColor={STROKE};{FONT}fontSize=10;"
             "labelBackgroundColor=#FFFFFF;")
    if exit:
        style += f"exitX={exit[0]};exitY={exit[1]};exitDx=0;exitDy=0;"
    if entry:
        style += f"entryX={entry[0]};entryY={entry[1]};entryDx=0;entryDy=0;"
    eid = d.edge(a, b, value=label, style=style, points=points)
    lbl = f"resizable=0;html=1;{FONT}fontSize=10;labelBackgroundColor=#FFFFFF;"
    if m1:
        d.cells.append(f'<mxCell id="{eid}s" value="{m1}" style="{lbl}align=left;verticalAlign=bottom;" vertex="1" '
                       f'connectable="0" parent="{eid}"><mxGeometry x="-0.85" relative="1" as="geometry">'
                       f'<mxPoint x="4" y="-2" as="offset"/></mxGeometry></mxCell>')
    if m2:
        d.cells.append(f'<mxCell id="{eid}t" value="{m2}" style="{lbl}align=right;verticalAlign=bottom;" vertex="1" '
                       f'connectable="0" parent="{eid}"><mxGeometry x="0.85" relative="1" as="geometry">'
                       f'<mxPoint x="-4" y="-2" as="offset"/></mxGeometry></mxCell>')
    return eid


# ---------------------------------------------------------------- ERD
def erd_table(d, name, x, y, cols, w=230, row=22):
    """cols: list of (key, name, type) với key ∈ 'PK','FK','PK,FK','UQ',''. Trả về (table_id, {col: row_id})."""
    h = 28 + row * len(cols)
    tid = d.vertex(f"<b>{name}</b>", x, y, w, h,
                   f"swimlane;html=1;fontStyle=0;childLayout=stackLayout;horizontal=1;startSize=28;horizontalStack=0;"
                   f"resizeParent=1;resizeLast=0;collapsible=0;marginBottom=0;{FONT}fontSize=12;fillColor={ERD_HEAD};"
                   f"fontColor=#FFFFFF;swimlaneFillColor=#FFFFFF;strokeColor={ERD_HEAD};rounded=0;", prefix="tbl")
    rows = {}
    for i, (key, cname, ctype) in enumerate(cols):
        bg = "#DEEAF6" if "PK" in key else "#FFFFFF"
        label = (f'<table style="width:100%;border-collapse:collapse;font-size:10px"><tr>'
                 f'<td style="width:34px;font-weight:bold">{key}</td>'
                 f'<td style="{"font-weight:bold;" if "PK" in key else ""}">{escape(cname)}</td>'
                 f'<td style="text-align:right;color:#555555">{escape(ctype)}</td></tr></table>')
        rid = d.vertex(label, 0, 28 + i * row, w, row,
                       f"text;html=1;strokeColor=#BFBFBF;fillColor={bg};align=left;verticalAlign=middle;spacingLeft=4;"
                       f"spacingRight=4;{FONT}fontSize=10;overflow=hidden;whiteSpace=wrap;",
                       parent=tid, prefix="col")
        rows[cname] = rid
    return tid, rows


def erd_rel(d, many_row, one_row, many="ERmany", one="ERmandOne", points=None, extra=""):
    es = "edgeStyle=orthogonalEdgeStyle;" if points else "edgeStyle=entityRelationEdgeStyle;"
    return d.edge(many_row, one_row,
                  style=f"{es}{extra}html=1;endArrow={one};startArrow={many};endFill=0;"
                        f"startFill=0;strokeColor=#2F5597;endSize=10;startSize=10;rounded=0;", points=points)


# ---------------------------------------------------------------- Block / architecture
def box(d, label, x, y, w=150, h=50, size=12, bold=False, fill="#FFFFFF", dashed=False, rounded=False, valign="middle"):
    style = (f"rounded={1 if rounded else 0};whiteSpace=wrap;html=1;{FONT}fontSize={size};fillColor={fill};"
             f"strokeColor={STROKE};fontStyle={1 if bold else 0};verticalAlign={valign};")
    if dashed:
        style += "dashed=1;dashPattern=8 4;"
    return d.vertex(label, x, y, w, h, style, prefix="b")


def cylinder(d, label, x, y, w=120, h=70):
    return d.vertex(label, x, y, w, h, f"shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;size=10;{FONT}fontSize=12;"
                                       f"fillColor=#FFFFFF;strokeColor={STROKE};", prefix="db")


def arrow(d, a, b, label="", both=False, dashed=False, points=None, exit=None, entry=None, ortho=True):
    style = (("edgeStyle=orthogonalEdgeStyle;" if ortho else "") +
             f"rounded=0;html=1;endArrow=block;endFill=1;endSize=8;strokeColor={STROKE};{FONT}fontSize=11;"
             "labelBackgroundColor=#FFFFFF;")
    if both:
        style += "startArrow=block;startFill=1;startSize=8;"
    if dashed:
        style += "dashed=1;"
    if exit:
        style += f"exitX={exit[0]};exitY={exit[1]};exitDx=0;exitDy=0;"
    if entry:
        style += f"entryX={entry[0]};entryY={entry[1]};entryDx=0;entryDy=0;"
    return d.edge(a, b, value=label, style=style, points=points)


# ---------------------------------------------------------------- Wireframe
def wf_grid(d, x, y, w, h, step=20):
    d.vertex("", x, y, w, h, "rounded=0;html=1;fillColor=#FFFFFF;strokeColor=none;locked=1;", prefix="bg")
    gx = x
    while gx <= x + w:
        d.line(gx, y, gx, y + h, "endArrow=none;html=1;strokeColor=#E6E6E6;strokeWidth=1;locked=1;")
        gx += step
    gy = y
    while gy <= y + h:
        d.line(x, gy, x + w, gy, "endArrow=none;html=1;strokeColor=#E6E6E6;strokeWidth=1;locked=1;")
        gy += step


def wf_screen(d, x, y, w, h):
    return d.vertex("", x, y, w, h, "rounded=0;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#000000;",
                    prefix="scr")


def wf_rect(d, x, y, w, h, label="", rounded=True, size=10, align="center", fill="#FFFFFF", bold=False, valign="middle"):
    return d.vertex(label, x, y, w, h,
                    f"rounded={1 if rounded else 0};arcSize=12;whiteSpace=wrap;html=1;{FONT}fontSize={size};"
                    f"fillColor={fill};strokeColor=#000000;align={align};spacingLeft=4;fontStyle={1 if bold else 0};"
                    f"verticalAlign={valign};", prefix="wr")


def wf_img(d, x, y, w, h):
    return d.vertex("", x, y, w, h, "verticalLabelPosition=bottom;shadow=0;dashed=0;align=center;html=1;"
                                    "verticalAlign=top;strokeWidth=1;shape=mxgraph.mockup.graphics.simpleIcon;"
                                    "strokeColor=#000000;fillColor=#FFFFFF;", prefix="img")


def wf_lines(d, x, y, w, n=3, gap=9):
    for i in range(n):
        ww = w if i < n - 1 else w * 0.6
        d.line(x, y + i * gap, x + ww, y + i * gap, "endArrow=none;html=1;strokeColor=#000000;strokeWidth=1;")


def wf_btn(d, x, y, w, h, label, size=10, pill=True, fill="#FFFFFF"):
    return d.vertex(label, x, y, w, h,
                    f"rounded=1;arcSize={50 if pill else 15};whiteSpace=wrap;html=1;{FONT}fontSize={size};"
                    f"fillColor={fill};strokeColor=#000000;", prefix="btn")


def wf_input(d, x, y, w, label="", h=18, size=9):
    if label:
        d.text(label, x, y - 15, w, 14, size=size)
    return d.vertex("", x, y, w, h, "rounded=0;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#000000;",
                    prefix="in")


def wf_label(d, label, x, y, w, h=16, size=10, bold=False, align="left"):
    return d.text(label, x, y, w, h, size=size, bold=bold, align=align)


def wf_flow(d, a, b, points=None, exit=None, entry=None):
    style = "edgeStyle=none;curved=1;html=1;endArrow=classic;endSize=7;strokeColor=#000000;"
    if exit:
        style += f"exitX={exit[0]};exitY={exit[1]};exitDx=0;exitDy=0;"
    if entry:
        style += f"entryX={entry[0]};entryY={entry[1]};entryDx=0;entryDy=0;"
    return d.edge(a, b, style=style, points=points)
