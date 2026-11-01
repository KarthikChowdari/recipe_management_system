# ---------------------------------------------------------------------------
# Recipe Management System - multi-stage production image
#   Stage 1 (build): Maven + JDK 21 compiles and packages the Spring Boot JAR
#   Stage 2 (run):   slim JRE 21 runs the app as a non-root user
#
#   docker build -t recipe-management-system:local .
#   docker run -d -p 8080:8080 --name recipe-app recipe-management-system:local
# ---------------------------------------------------------------------------

# ======== Stage 1: build ========
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Resolve dependencies first so this layer is cached until pom.xml changes.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
# Tests run in the CI pipeline before the image is built; skip them here to keep builds fast.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# Split the fat JAR into layers (dependencies change rarely, application code often).
RUN java -Djarmode=tools -jar target/recipe-management-system.jar \
        extract --layers --launcher --destination target/extracted

# ======== Stage 2: runtime ========
FROM eclipse-temurin:21-jre-alpine

ARG VERSION=dev
ARG GIT_COMMIT=unknown
LABEL org.opencontainers.image.title="recipe-management-system" \
      org.opencontainers.image.description="Recipe Management System (Spring Boot)" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.revision="${GIT_COMMIT}"

RUN addgroup -S app && adduser -S -G app -h /app app
WORKDIR /app

COPY --from=build --chown=app:app /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=app:app /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /workspace/target/extracted/application/ ./

USER app

ENV SERVER_PORT=8080 \
    APP_VERSION=${VERSION} \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- "http://localhost:${SERVER_PORT}/actuator/health" | grep -q '"status":"UP"' || exit 1

# exec so the JVM is PID 1 and receives SIGTERM for graceful shutdown
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher \"$@\"", "--"]
