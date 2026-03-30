#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# deploy/sync-repos.sh
# Клонирует или обновляет репозитории в /opt/dev-crew/repos/.
# Агенты работают с файлами из этой папки (смонтирована как /projects).
#
# Использование:
#   bash deploy/sync-repos.sh                  # обновить все
#   bash deploy/sync-repos.sh --clone          # первый клон
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

REPOS_DIR="/opt/dev-crew/repos"
mkdir -p "$REPOS_DIR"

# ─── Список репозиториев ───────────────────────────────────────────────────
# Формат: "папка|url"
REPOS=(
  "early-warning-control-system|git@github.com:yourname/Early-Warning-Control-System.git"
  # "другой-проект|git@github.com:yourname/other-project.git"
)

for entry in "${REPOS[@]}"; do
  dir="${entry%%|*}"
  url="${entry##*|}"
  target="$REPOS_DIR/$dir"

  if [[ -d "$target/.git" ]]; then
    echo "==> Pull $dir..."
    git -C "$target" pull --ff-only
  else
    echo "==> Clone $dir..."
    git clone "$url" "$target"
  fi
done

echo ""
echo "✅ Репозитории синхронизированы в $REPOS_DIR/"
echo "   Агенты обращаются к ним через /projects/<папка>/..."
