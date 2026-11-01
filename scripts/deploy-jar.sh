#!/usr/bin/env bash
# Deploys the built JAR as a background process on this machine (Linux/macOS/Git Bash).
# Stops the previously deployed instance, keeps the previous JAR for rollback, starts the
# new one on the requested port and waits for the health check.
#
# Usage: scripts/deploy-jar.sh <port> [deploy-dir] [jar]
set -euo pipefail

PORT="${1:?usage: deploy-jar.sh <port> [deploy-dir] [jar]}"
DEPLOY_DIR="${2:-$HOME/recipe-deploy}"
JAR="${3:-target/recipe-management-system.jar}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

mkdir -p "$DEPLOY_DIR"
PID_FILE="$DEPLOY_DIR/app.pid"

if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "Stopping previous instance (pid $(cat "$PID_FILE"))"
  kill "$(cat "$PID_FILE")" || true
  for _ in $(seq 1 30); do kill -0 "$(cat "$PID_FILE")" 2>/dev/null || break; sleep 1; done
  kill -9 "$(cat "$PID_FILE")" 2>/dev/null || true
fi

if [ -f "$DEPLOY_DIR/app.jar" ]; then
  cp "$DEPLOY_DIR/app.jar" "$DEPLOY_DIR/app-previous.jar"
fi
cp "$JAR" "$DEPLOY_DIR/app.jar"

# JENKINS_NODE_COOKIE / BUILD_ID stop Jenkins from killing the process when the build ends.
JENKINS_NODE_COOKIE=dontKillMe BUILD_ID=dontKillMe \
  nohup java -jar "$DEPLOY_DIR/app.jar" --server.port="$PORT" > "$DEPLOY_DIR/app.log" 2>&1 &
echo $! > "$PID_FILE"
echo "Started pid $(cat "$PID_FILE") on port $PORT; logs: $DEPLOY_DIR/app.log"

if ! "$SCRIPT_DIR/health-check.sh" "http://localhost:${PORT}/actuator/health" 120; then
  echo "Health check failed; last log lines:" >&2
  tail -n 50 "$DEPLOY_DIR/app.log" >&2 || true
  exit 1
fi
