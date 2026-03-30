#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# deploy/setup-server.sh
# Первоначальная настройка сервера (Ubuntu 22.04 / Debian 12).
# Запуск: bash deploy/setup-server.sh
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

APP_DIR="/opt/dev-crew"
REPOS_DIR="$APP_DIR/repos"
PGDATA_DIR="$APP_DIR/pgdata"

echo "==> [1/5] Установка Docker..."
if ! command -v docker &>/dev/null; then
  curl -fsSL https://get.docker.com | sh
  usermod -aG docker "$USER"
  echo "     Docker установлен. Перелогинься чтобы группа docker применилась."
else
  echo "     Docker уже установлен: $(docker --version)"
fi

echo "==> [2/5] Установка git (если нет)..."
apt-get install -y git curl 2>/dev/null || true

echo "==> [3/5] Создание директорий..."
mkdir -p "$REPOS_DIR" "$PGDATA_DIR"
chmod 755 "$APP_DIR"

echo "==> [4/5] Копирование файлов приложения..."
# Предполагается, что скрипт запускается из корня репозитория dev-crew
cp docker-compose.yml docker-compose.prod.yml "$APP_DIR/"
cp .env.example "$APP_DIR/.env"
echo ""
echo "     ВАЖНО: отредактируй $APP_DIR/.env — заполни ANTHROPIC_API_KEY, DB_PASSWORD и т.д."

echo "==> [5/5] Настройка systemd для автозапуска (опционально)..."
cat > /etc/systemd/system/dev-crew.service <<EOF
[Unit]
Description=Dev Crew AI Agent Team
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$APP_DIR
ExecStart=/usr/bin/docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
ExecStop=/usr/bin/docker compose -f docker-compose.yml -f docker-compose.prod.yml down
TimeoutStartSec=300

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable dev-crew

echo ""
echo "✅ Сервер настроен."
echo ""
echo "Следующие шаги:"
echo "  1. Отредактируй $APP_DIR/.env"
echo "  2. Клонируй репозитории в $REPOS_DIR/"
echo "     cd $REPOS_DIR && git clone <repo-url>"
echo "  3. Запусти: cd $APP_DIR && bash /path/to/deploy/update.sh"
