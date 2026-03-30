#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# deploy/update.sh
# Обновление приложения на сервере: pull → build → restart.
# Запуск на сервере из /opt/dev-crew/:
#   bash /path/to/deploy/update.sh [--skip-build]
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

APP_DIR="/opt/dev-crew"
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"  # корень dev-crew репозитория
SKIP_BUILD=false

for arg in "$@"; do
  [[ "$arg" == "--skip-build" ]] && SKIP_BUILD=true
done

echo "==> [1/4] Pull последних изменений..."
cd "$REPO_DIR"
git pull --ff-only

echo "==> [2/4] Синхронизация compose-файлов в $APP_DIR..."
cp docker-compose.yml docker-compose.prod.yml "$APP_DIR/"

if [[ "$SKIP_BUILD" == false ]]; then
  echo "==> [3/4] Сборка Docker-образа..."
  docker compose -f "$APP_DIR/docker-compose.yml" \
                 -f "$APP_DIR/docker-compose.prod.yml" \
                 --env-file "$APP_DIR/.env" \
                 build --no-cache app
else
  echo "==> [3/4] Сборка пропущена (--skip-build)"
fi

echo "==> [4/4] Перезапуск контейнеров..."
docker compose -f "$APP_DIR/docker-compose.yml" \
               -f "$APP_DIR/docker-compose.prod.yml" \
               --env-file "$APP_DIR/.env" \
               up -d --remove-orphans

echo ""
echo "✅ Обновление завершено."
echo "   Логи: docker logs dev-crew-app --follow"
echo "   Статус: docker compose -f $APP_DIR/docker-compose.yml ps"
