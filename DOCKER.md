# Docker Guide

The [`Dockerfile`](Dockerfile) is a two-stage build:

| Stage   | Base image                      | What happens                                                           |
|---------|---------------------------------|------------------------------------------------------------------------|
| `build` | `maven:3.9-eclipse-temurin-21`  | Resolves dependencies (cached layer), packages the JAR, splits it into layers |
| runtime | `eclipse-temurin:21-jre-alpine` | Copies the layers, runs as non-root user `app`, exposes 8080, health check |

> The official `openjdk` images are deprecated; Eclipse Temurin is the maintained OpenJDK
> distribution and is used for both stages.

Image features: layered JAR (dependency layers rarely change → fast rebuilds/pulls),
non-root user, `HEALTHCHECK` on `/actuator/health`, OCI labels (`version`, `revision`),
container-aware heap sizing (`-XX:MaxRAMPercentage=75`), graceful shutdown on `SIGTERM`.

---

## 1. Build

```bash
# Simple local build
docker build -t recipe-management-system:local .

# Versioned build with metadata (what Jenkins does)
docker build \
  --build-arg VERSION=42 \
  --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
  -t recipe-management-system:42 \
  -t recipe-management-system:latest .
```

BuildKit (default since Docker 23) is required for the `--mount=type=cache` Maven cache.
On older engines: `DOCKER_BUILDKIT=1 docker build ...`.

## 2. Tag

```bash
# Docker Hub
docker tag recipe-management-system:42 <dockerhub-user>/recipe-management-system:42
docker tag recipe-management-system:42 <dockerhub-user>/recipe-management-system:latest

# Local registry
docker run -d -p 5000:5000 --restart=always --name registry registry:2
docker tag recipe-management-system:42 localhost:5000/recipe-management-system:42
```

## 3. Push / pull

```bash
docker login                                   # use a Docker Hub access token
docker push <dockerhub-user>/recipe-management-system:42
docker push <dockerhub-user>/recipe-management-system:latest

docker push localhost:5000/recipe-management-system:42
docker pull localhost:5000/recipe-management-system:42
```

## 4. Run and port mapping

```bash
# Container port 8080 -> host port 8080
docker run -d --name recipe-app -p 8080:8080 recipe-management-system:local

# Different host port (host 9090 -> container 8080)
docker run -d --name recipe-app -p 9090:8080 recipe-management-system:local

# Bind only to localhost, restart automatically, limit resources
docker run -d --name recipe-app -p 127.0.0.1:8080:8080 \
  --restart unless-stopped --memory 768m --cpus 1.0 \
  recipe-management-system:local

# Override configuration with environment variables
docker run -d --name recipe-app -p 8080:8080 \
  -e APP_ADMIN_PASSWORD='change-me' \
  -e APP_AUTHOR_PASSWORD='change-me-too' \
  -e JAVA_OPTS="-XX:MaxRAMPercentage=60" \
  recipe-management-system:local

# Or keep variables in a file
docker run -d --name recipe-app -p 8080:8080 --env-file ./recipe.env recipe-management-system:local
```

Then open <http://localhost:8080> (demo users: see README).

### With MySQL (docker compose)

```bash
SEED_DATA=always docker compose up -d --build   # first start: create schema + sample data
docker compose up -d                            # later starts
docker compose ps
docker compose down                             # stop (data kept in the mysql_data volume)
docker compose down -v                          # stop and delete the database volume
```

## 5. Inspect

```bash
docker ps                                        # running containers (STATUS shows "healthy")
docker ps -a --filter name=recipe-app            # including stopped ones
docker logs recipe-app                           # all logs
docker logs -f --tail 100 recipe-app             # follow the last 100 lines
docker logs --since 10m recipe-app               # last 10 minutes
docker inspect recipe-app                        # full JSON
docker inspect --format '{{.State.Health.Status}}' recipe-app
docker inspect --format '{{json .State.Health}}' recipe-app
docker inspect --format '{{index .Config.Labels "org.opencontainers.image.version"}}' recipe-app
docker port recipe-app                           # port mappings
docker stats recipe-app                          # live CPU / memory
docker exec -it recipe-app sh                    # shell inside the container
docker exec recipe-app wget -qO- http://localhost:8080/actuator/health
docker image history recipe-management-system:local   # layers and sizes
curl http://localhost:8080/actuator/health
```

## 6. Stop, start, restart

```bash
docker stop recipe-app          # SIGTERM -> graceful shutdown (10 s default timeout)
docker stop -t 30 recipe-app    # allow 30 s
docker start recipe-app
docker restart recipe-app
```

## 7. Upgrade / rollback a running container

```bash
docker pull <registry>/recipe-management-system:43
docker rm -f recipe-app
docker run -d --name recipe-app -p 8080:8080 --restart unless-stopped <registry>/recipe-management-system:43

# Roll back: run the previous tag again
docker rm -f recipe-app
docker run -d --name recipe-app -p 8080:8080 --restart unless-stopped <registry>/recipe-management-system:42
```

Automated rollback with health checks is handled by Ansible — see [`ROLLBACK.md`](ROLLBACK.md).

## 8. Clean up

```bash
docker rm -f recipe-app                              # remove the container
docker rmi recipe-management-system:local            # remove an image
docker image prune -f                                # dangling images
docker images recipe-management-system --format '{{.Tag}}' \
  | sort -n | head -n -5 | xargs -r -I{} docker rmi recipe-management-system:{}   # keep newest 5 numeric tags
docker builder prune -f                              # BuildKit cache (incl. Maven cache mount)
docker system prune -f                               # stopped containers, unused networks, dangling images
docker system df                                     # disk usage summary
```

## 9. Troubleshooting

| Symptom                                       | Cause / fix                                                              |
|-----------------------------------------------|--------------------------------------------------------------------------|
| `port is already allocated`                   | Another process uses the host port: pick another `-p <host>:8080`        |
| Container `unhealthy`                         | `docker logs recipe-app`; app needs ~15-30 s to start (start period 60 s) |
| Exits immediately with code 137               | Out of memory: raise `--memory` or lower `MaxRAMPercentage`              |
| `the --mount option requires BuildKit`        | Enable BuildKit: `DOCKER_BUILDKIT=1`                                     |
| MySQL connection refused in compose           | MySQL still initialising; `depends_on: service_healthy` waits — check `docker compose logs mysql` |
| Windows: `error during connect`               | Start Docker Desktop                                                     |
