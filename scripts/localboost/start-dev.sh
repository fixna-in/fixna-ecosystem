#!/usr/bin/env bash
# LocalBoost dev helpers — monorepo paths
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
docker compose up -d
echo "LocalBoost infra up. Start API: mvn -f backend/localboost-api/pom.xml spring-boot:run"
echo "Start web: npm --prefix frontend/localboost-web run dev"
