#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# deploy/update.sh
# Обновление приложения на сервере: pull образа → restart.
# Запуск на сервере из /opt/dev-crew/:
#   bash /path/to/deploy/update.sh [--tag <image-tag>]
#
# Образ собирается в GitHub Actions и пушится в ghcr.io.
# На сервере только: docker pull + docker compose up.
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

APP_DIR="/opt/dev-crew"
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"  # корень dev-crew репозитория
IMAGE_TAG="latest"

for arg in "$@"; do
  case "$arg" in
    --tag) shift; IMAGE_TAG="$1" ;;
  esac
done

echo "==> [1/3] Синхронизация compose-файлов в $APP_DIR..."
cp "$REPO_DIR/docker/docker-compose.yml" \
   "$REPO_DIR/docker/docker-compose.prod.yml" \
   "$APP_DIR/"

echo "==> [2/3] Pull образа из ghcr.io (tag: $IMAGE_TAG)..."
IMAGE_TAG="$IMAGE_TAG" docker compose \
  -f "$APP_DIR/docker-compose.yml" \
  -f "$APP_DIR/docker-compose.prod.yml" \
  --env-file "$APP_DIR/.env" \
  pull app

echo "==> [3/3] Перезапуск контейнеров..."
IMAGE_TAG="$IMAGE_TAG" docker compose \
  -f "$APP_DIR/docker-compose.yml" \
  -f "$APP_DIR/docker-compose.prod.yml" \
  --env-file "$APP_DIR/.env" \
  up -d --remove-orphans

docker image prune -f

echo ""
echo "✅ Обновление завершено."
echo "   Логи: docker logs dev-crew-app --follow"
echo "   Статус: docker compose -f $APP_DIR/docker-compose.yml ps"
