#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
docker compose up -d
echo ""
echo "Databases ready:"
echo "  Consulting:  localhost:5433 / consulting"
echo "  LocalBoost:  localhost:5432 / fixna"
echo ""
echo "Start APIs (run npm run bootstrap:maven once first):"
echo "  mvn -f backend/localboost-api/pom.xml spring-boot:run"
echo "  mvn -f backend/consulting-api/pom.xml spring-boot:run"
echo ""
echo "Start web:"
echo "  npm --prefix frontend/localboost-web run dev"
