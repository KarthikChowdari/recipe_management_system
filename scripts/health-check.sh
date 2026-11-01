#!/usr/bin/env bash
# Polls the Spring Boot health endpoint until it reports UP.
# Usage: scripts/health-check.sh [url] [timeout-seconds]
set -euo pipefail

URL="${1:-http://localhost:8080/actuator/health}"
TIMEOUT="${2:-90}"
deadline=$(( $(date +%s) + TIMEOUT ))

echo "Waiting for ${URL} (timeout ${TIMEOUT}s)..."
while [ "$(date +%s)" -lt "$deadline" ]; do
  body="$(curl -fsS --max-time 5 "$URL" 2>/dev/null || true)"
  if echo "$body" | grep -q '"status":"UP"'; then
    echo "Healthy: $body"
    exit 0
  fi
  sleep 3
done

echo "ERROR: ${URL} did not report UP within ${TIMEOUT}s (last response: ${body:-none})" >&2
exit 1
