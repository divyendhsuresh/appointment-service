#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIRECTORY="$PROJECT_ROOT/logs/kafka"
LOG_FILE="$LOG_DIRECTORY/kafka-broker.log"

mkdir -p "$LOG_DIRECTORY"

echo "Writing Kafka broker logs to: $LOG_FILE"

docker compose \
  -f "$PROJECT_ROOT/docker-compose.yml" \
  logs \
  --follow \
  --timestamps \
  kafka \
  | tee -a "$LOG_FILE"