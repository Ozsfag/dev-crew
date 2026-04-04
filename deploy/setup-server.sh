#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# deploy/setup-server.sh
# Первоначальная настройка сервера (Ubuntu 22.04 / Debian 12).
# Запуск: bash deploy/setup-server.sh
#
# Требования: Ubuntu 22.04, 2 GB RAM, 20 GB SSD.
# Образ загружается из ghcr.io — JDK на сервере не нужен.
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

APP_DIR="/opt/dev-crew"
REPOS_DIR="$APP_DIR/repos"
PGDATA_DIR="$APP_DIR/pgdata"
SSH_DIR="$APP_DIR/ssh"

echo "==> [1/7] Установка Docker..."
if ! command -v docker &>/dev/null; then
  curl -fsSL https://get.docker.com | sh
  usermod -aG docker "$USER"
  echo "     Docker установлен. Перелогинься чтобы группа docker применилась."
else
  echo "     Docker уже установлен: $(docker --version)"
fi

echo "==> [2/7] Установка git (если нет)..."
apt-get install -y git curl 2>/dev/null || true

echo "==> [3/7] Создание директорий..."
mkdir -p "$REPOS_DIR" "$PGDATA_DIR" "$SSH_DIR"
chmod 700 "$SSH_DIR"
chmod 755 "$APP_DIR"

echo "==> [4/7] Копирование файлов приложения..."
# Предполагается, что скрипт запускается из корня репозитория dev-crew
cp docker/docker-compose.yml docker/docker-compose.prod.yml "$APP_DIR/"

if [[ ! -f "$APP_DIR/.env" ]]; then
  cat > "$APP_DIR/.env" <<'ENVEOF'
# ─── Обязательные ────────────────────────────────────────────────
ANTHROPIC_API_KEY=sk-ant-...
DB_PASSWORD=change_me_strong_password
DEVCREW_AUTH_JWT_SECRET=change_me_32_plus_chars_secret_key

# ─── Telegram ────────────────────────────────────────────────────
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
TELEGRAM_ENABLED=false
TELEGRAM_BOT_ENABLED=false
TELEGRAM_BOT_USERNAME=
TELEGRAM_ALLOWED_CHAT_ID=0

# ─── Stripe ──────────────────────────────────────────────────────
STRIPE_WEBHOOK_SECRET=whsec_...

# ─── OpenAI / Whisper (П8) ───────────────────────────────────────
OPENAI_API_KEY=

# ─── Docker image ────────────────────────────────────────────────
APP_IMAGE=ghcr.io/ozsfag/dev-crew:latest
IMAGE_TAG=latest
ENVEOF
  echo ""
  echo "     ВАЖНО: отредактируй $APP_DIR/.env — заполни все ключи!"
else
  echo "     $APP_DIR/.env уже существует — пропускаем."
fi

echo "==> [5/7] Аутентификация в ghcr.io..."
echo "     Для загрузки образа нужен GHCR_TOKEN (GitHub PAT с правом read:packages)."
echo "     Выполни вручную:"
echo "       echo \$GHCR_TOKEN | docker login ghcr.io -u <github-username> --password-stdin"

echo "==> [6/7] SSH-ключ для git операций агентов..."
if [[ ! -f "$SSH_DIR/id_ed25519" ]]; then
  ssh-keygen -t ed25519 -C "dev-crew-agent" -f "$SSH_DIR/id_ed25519" -N ""
  echo ""
  echo "     SSH публичный ключ (добавь в GitHub Deploy Keys репозиториев):"
  cat "$SSH_DIR/id_ed25519.pub"
else
  echo "     SSH ключ уже существует: $SSH_DIR/id_ed25519.pub"
fi

echo "==> [7/7] Настройка systemd для автозапуска..."
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
echo "  2. Аутентифицируйся в ghcr.io (см. шаг 5)"
echo "  3. Добавь $SSH_DIR/id_ed25519.pub в GitHub Deploy Keys нужных репо"
echo "  4. Клонируй репозитории в $REPOS_DIR/"
echo "     cd $REPOS_DIR && GIT_SSH_COMMAND='ssh -i $SSH_DIR/id_ed25519' git clone <repo-url>"
echo "  5. Запусти: systemctl start dev-crew"
echo "     Или вручную: cd $APP_DIR && docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d"
