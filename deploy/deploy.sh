#!/usr/bin/env bash
# Manual deploy on the server (when you already updated the code by other means).
# CI/CD uploads code via GitHub Actions scp — it does not call this git pull path.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR/deploy"

if [[ ! -f .env ]]; then
  echo "Missing deploy/.env — copy deploy/.env.example and fill secrets first."
  exit 1
fi

echo "==> Build & restart containers"
docker compose --env-file .env up -d --build --remove-orphans
docker image prune -f
docker compose ps
