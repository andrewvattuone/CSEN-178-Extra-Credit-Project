#!/usr/bin/env zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "Initializing MySQL database for AI Hardware Benchmark Tracker..."

if [[ -n "${MYSQL_ROOT_PASSWORD:-}" ]]; then
  MYSQL_CMD=(mysql -u root -p"$MYSQL_ROOT_PASSWORD")
elif mysql -u root -e 'SELECT 1' >/dev/null 2>&1; then
  MYSQL_CMD=(mysql -u root)
else
  MYSQL_CMD=(sudo mysql)
fi

"${MYSQL_CMD[@]}" < create_dbuser.sql
"${MYSQL_CMD[@]}" < schema.sql
"${MYSQL_CMD[@]}" < seed.sql

echo "Database initialization complete."
