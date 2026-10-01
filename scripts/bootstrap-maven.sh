#!/usr/bin/env bash
# Install parent POM and fixna-platform-common into the local Maven repository.
# Required before building a single product API from its own pom.xml.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mvn -f pom.xml install -N -B
mvn -f backend/libs/fixna-platform-common/pom.xml clean install -DskipTests -B
echo "Maven bootstrap complete (parent POM + fixna-platform-common:1.0.0)."
