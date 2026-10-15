#!/usr/bin/env bash
# Xuất mọi file .drawio sang PNG bằng draw.io desktop CLI. DRAWIO=đường dẫn binary draw.io
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DRAWIO="${DRAWIO:-drawio}"
export_dir() {
  local src="$1" out="$2"; mkdir -p "$out"
  for f in "$src"/*.drawio; do
    [ -e "$f" ] || continue
    local png="$out/$(basename "$f" .drawio).png"
    [ "$png" -nt "$f" ] && continue
    "$DRAWIO" --no-sandbox --disable-gpu -x -f png --scale 2 --border 16 -o "$out/$(basename "$f" .drawio).png" "$f" >/dev/null 2>&1
  done
}
export_dir "$ROOT/diagrams/src" "$ROOT/diagrams/export"
export_dir "$ROOT/ui-design/src" "$ROOT/ui-design/export"
echo "export done"
