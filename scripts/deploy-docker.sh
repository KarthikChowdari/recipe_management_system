#!/usr/bin/env bash
# Replaces the running application container with a new image and waits for Docker's health
# check. If the new container does not become healthy, the previous image is started again.
#
# Usage: scripts/deploy-docker.sh <image> <host-port> [container-name]
# Extra `docker run` options can be passed via DOCKER_RUN_OPTS (e.g. "--env-file /etc/recipe.env").
set -euo pipefail

IMAGE="${1:?usage: deploy-docker.sh <image> <host-port> [container-name]}"
PORT="${2:?usage: deploy-docker.sh <image> <host-port> [container-name]}"
NAME="${3:-recipe-app}"
TIMEOUT="${HEALTH_TIMEOUT:-150}"

start_container() {
  # shellcheck disable=SC2086
  docker run -d --name "$NAME" -p "${PORT}:8080" --restart unless-stopped \
    --health-interval 5s --health-start-period 120s \
    ${DOCKER_RUN_OPTS:-} "$1" > /dev/null
}

wait_healthy() {
  local deadline=$(( $(date +%s) + TIMEOUT )) status
  while [ "$(date +%s)" -lt "$deadline" ]; do
    status="$(docker inspect --format '{{.State.Health.Status}}' "$NAME" 2>/dev/null || echo missing)"
    case "$status" in
      healthy) return 0 ;;
      unhealthy|missing) return 1 ;;
    esac
    if [ "$(docker inspect --format '{{.State.Running}}' "$NAME")" != "true" ]; then
      return 1
    fi
    sleep 3
  done
  return 1
}

PREVIOUS="$(docker inspect --format '{{.Config.Image}}' "$NAME" 2>/dev/null || true)"
echo "Current image: ${PREVIOUS:-<none>}  ->  new image: $IMAGE"

docker rm -f "$NAME" > /dev/null 2>&1 || true
start_container "$IMAGE"
echo "Started $NAME on port $PORT, waiting for health check (timeout ${TIMEOUT}s)..."

if wait_healthy; then
  echo "Deployment healthy: $IMAGE on http://localhost:${PORT}"
  exit 0
fi

echo "ERROR: $IMAGE did not become healthy. Last log lines:" >&2
docker logs --tail 80 "$NAME" >&2 || true
docker rm -f "$NAME" > /dev/null 2>&1 || true

if [ -n "$PREVIOUS" ] && [ "$PREVIOUS" != "$IMAGE" ]; then
  echo "Rolling back to $PREVIOUS" >&2
  start_container "$PREVIOUS"
  if wait_healthy; then
    echo "Rollback succeeded: $PREVIOUS is serving on port $PORT" >&2
  else
    echo "Rollback FAILED: $PREVIOUS is not healthy either" >&2
  fi
fi
exit 1
