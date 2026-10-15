#!/usr/bin/env bash
# Quy trình dựng báo cáo: sinh sơ đồ -> xuất PNG -> ghép DOCX -> cập nhật mục lục (LibreOffice) -> kiểm tra.
# Yêu cầu: python3 + python-docx + Pillow, draw.io desktop (biến DRAWIO), LibreOffice (soffice + python uno).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
python3 diagrams/build_all.py
python3 ui-design/build_wireframes.py
scripts/export_diagrams.sh
python3 scripts/build_report.py
python3 scripts/update_fields.py
python3 scripts/gen_inventory.py
python3 scripts/check_report.py
echo "Xong: report/final-report.docx, report/final-report.pdf; xem qa/check-report.md"
