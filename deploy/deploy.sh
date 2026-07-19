#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -f deploy/.env ]]; then
  echo "Missing deploy/.env — copy deploy/.env.example and fill secrets first."
  exit 1
fi

echo "==> Pull latest code"
git pull --ff-only origin master

echo "==> Build & restart containers"
cd deploy
docker compose --env-file .env up -d --build --remove-orphans

echo "==> Prune dangling images"
docker image prune -f

echo "==> Done. Check: docker compose ps"
docker compose ps
