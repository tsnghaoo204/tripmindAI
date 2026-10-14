#!/usr/bin/env bash
set -e

# Tìm thư mục gốc của repository
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Đọc cấu hình từ file .env nếu có
if [ -f "$REPO_ROOT/.env" ]; then
  # Nạp các biến môi trường DB_* từ .env
  eval "$(grep -E '^(DB_HOST|DB_PORT|DB_NAME|DB_USER|DB_PASSWORD)=' "$REPO_ROOT/.env" || true)"
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-tripmind}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-password}"

ACTION="${1:-migrate}"

case "$ACTION" in
  migrate|info|repair|validate)
    ;;
  *)
    echo "Cách dùng: $0 [migrate|info|repair|validate]"
    echo "  migrate  : Chạy các file db migration mới (mặc định)"
    echo "  info     : Hiển thị bảng trạng thái tất cả các migration"
    echo "  repair   : Sửa checksum hoặc bản ghi migration lỗi"
    echo "  validate : Kiểm tra tính hợp lệ của các file migration"
    exit 1
    ;;
esac

echo "==> Đang thực hiện Flyway $ACTION trên jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME} (user: ${DB_USER})..."

"$REPO_ROOT/backend/mvnw" -f "$REPO_ROOT/backend/pom.xml" "flyway:${ACTION}" \
  -Dflyway.url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
  -Dflyway.user="${DB_USER}" \
  -Dflyway.password="${DB_PASSWORD}"
