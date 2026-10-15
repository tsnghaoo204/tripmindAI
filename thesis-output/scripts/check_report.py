"""Kiểm tra báo cáo DOCX: rò rỉ mã nguồn, tên tệp, đường dẫn, ghi chú nội bộ, placeholder. Ghi kết quả ra qa/check-report.md."""
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DOCX = ROOT / "report" / "final-report.docx"

RULES = [
    ("Tên tệp mã nguồn/cấu hình", r"\b[\w\-]+\.(java|tsx?|jsx?|py|md|ya?ml|sql|json|xml|html|properties|env|sh|drawio|gradle|lock)\b"),
    ("Đường dẫn thư mục", r"(?<![\w:/])(?:[\w.\-]+/){2,}[\w.\-]*|\b(src|backend|frontend|scripts|docs|chapters|spec|qa)/"),
    ("Ký hiệu mã (backtick, ngoặc nhọn, mũi tên lập trình)", r"`|\{|\}|=>|->|::|;\s*$"),
    ("Chú thích/annotation", r"(?<![\w.])@[A-Z]\w+"),
    ("Tên lớp kỹ thuật dạng CamelCase", r"\b[A-Z][a-z]+(?:[A-Z][a-z]+)+(?:Controller|Service|Repository|Entity|Impl|Config|Filter|Gateway|Provider|Exception|Test)\b"),
    ("Tên định danh dạng snake_case kỹ thuật", r"\b[a-z]+_[a-z_]+\b"),
    ("Câu lệnh SQL/shell", r"\b(SELECT|INSERT INTO|UPDATE \w+ SET|DELETE FROM|CREATE TABLE|ALTER TABLE|docker compose|mvn |npm |git )\b"),
    ("Ghi chú nội bộ / mã quản lý", r"\b(DGM-|UI-0|SHOT-0|ASSET-|VERIFIED|INFERRED|UNVERIFIED|TODO|FIXME|xem file|trong class|tại phương thức)"),
    ("Placeholder chưa thay", r"\[(HỌ VÀ TÊN|MÃ SINH VIÊN|HỌC HÀM|LỚP|LOGO|ẢNH CẦN BỔ SUNG)"),
]
ALLOWED_SNAKE = {"users", "user_preferences", "destinations", "trips", "itinerary_days", "activities",
                 "activity_alternatives", "places", "saved_places", "place_ratings", "expenses",
                 "trip_checklist_items", "conversations", "messages", "ai_proposals", "ai_tool_executions",
                 "destination_knowledge", "blog_posts", "blog_comments", "blog_post_upvotes",
                 "blog_comment_upvotes"}


def docx_paragraphs(path):
    with zipfile.ZipFile(path) as z:
        xml = z.read("word/document.xml").decode("utf-8")
        media = [n for n in z.namelist() if n.startswith("word/media/")]
    paras = []
    for p in re.findall(r"<w:p[ >].*?</w:p>", xml, flags=re.S):
        if "<w:instrText" in p:
            continue
        t = "".join(re.findall(r"<w:t(?: [^>]*)?>(.*?)</w:t>", p, flags=re.S))
        t = t.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", '"')
        if t.strip():
            paras.append(t)
    return paras, media


def main():
    paras, media = docx_paragraphs(DOCX)
    findings = []
    for idx, text in enumerate(paras):
        if text.lstrip().startswith("[") and re.match(r"^\[\d+\]", text.strip()):
            body = re.sub(r"https?://\S+", "", text)
        else:
            body = re.sub(r"https?://\S+", "", text)
        for name, pat in RULES:
            for m in re.finditer(pat, body, flags=re.M):
                tok = m.group(0)
                if name.startswith("Tên định danh") and tok in ALLOWED_SNAKE:
                    continue
                findings.append((name, tok, text[:140]))
    figs = sum(1 for t in paras if re.match(r"^Hình \d+\.\d+\. ", t))
    tabs = sum(1 for t in paras if re.match(r"^Bảng \d+\.\d+\. ", t))
    refs = re.findall(r"Hình \d+\.\d+|Bảng \d+\.\d+", " ".join(paras))
    state = json.loads((ROOT / "qa" / "build-state.json").read_text(encoding="utf-8"))
    out = ["# Kết quả kiểm tra tự động báo cáo (NỘI BỘ)", "",
           f"- Số đoạn văn bản: {len(paras)}; ảnh nhúng: {len(media)}; chú thích hình: {figs}; chú thích bảng: {tabs}",
           f"- Tham chiếu chéo không phân giải được khi build: {len(state['warnings'])}",
           f"- Placeholder ảnh đang chờ bổ sung: {len(state['placeholders'])}", "", "## Phát hiện theo quy tắc", ""]
    if not findings:
        out.append("Không có phát hiện.")
    else:
        out.append("| Quy tắc | Chuỗi | Ngữ cảnh |")
        out.append("|---|---|---|")
        for name, tok, ctx in findings:
            out.append(f"| {name} | {tok.replace('|', '/')} | {ctx.replace('|', '/')} |")
    (ROOT / "qa" / "check-report.md").write_text("\n".join(out) + "\n", encoding="utf-8")
    by = {}
    for name, _, _ in findings:
        by[name] = by.get(name, 0) + 1
    print(f"{len(findings)} phát hiện", by)
    return 0


if __name__ == "__main__":
    sys.exit(main())
