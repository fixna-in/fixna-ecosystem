#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
mvn -f backend/localboost-api/pom.xml -B test
npm run build:localboost
echo "LocalBoost tests and web build passed."
