# Recipe Management System — Final Project Report

| Item        | Value                                                                    |
|-------------|--------------------------------------------------------------------------|
| Release     | v1.0.0                                                                   |
| Stack       | Java 21 · Spring Boot 3.5 · Maven · H2/MySQL · Embedded Tomcat · Thymeleaf |
| CI/CD       | Jenkins declarative pipeline · Selenium · Docker · Ansible               |
| Repository  | Git, lightweight Git Flow (`main`, `develop`, `feature/*`, `release/*`)  |

---

## 1. Executive summary

The project delivers a working recipe management web application and a complete delivery
pipeline around it:

- **Application** — recipe CRUD, search/filter, a role-based review workflow
  (`DRAFT → PENDING_REVIEW → PUBLISHED`) with an audit trail, and a statistics dashboard,
  exposed both as a Thymeleaf web UI and a REST API.
- **Quality gates** — 50 unit/integration tests (JUnit 5, Mockito, MockMvc, `@DataJpaTest`)
  and 11 Selenium end-to-end tests covering five user journeys, with failure screenshots.
- **Delivery** — a cross-platform Jenkinsfile (build → unit tests → Selenium → Docker build
  → push → deploy), a hardened multi-stage Docker image, and Ansible playbooks for
  provisioning, health-checked rolling deployment and automatic/manual rollback.

## 2. Requirements coverage

| Roadmap step | Deliverable                                                                 | Location |
|--------------|-----------------------------------------------------------------------------|----------|
| 1 Architecture & SRS | Requirements, API spec, data model, use cases                       | `architecture/SRS_SUMMARY.md` |
| 2 Repo & skeleton | Git repo, `.gitignore`, README, issue/PR templates, Maven project, wrapper | root, `.github/` |
| 3 Core feature | Recipe entity, repository, CRUD service, REST + UI controllers            | `src/main/java/.../{domain,repository,service,web}` |
| 4 MVP | Search/filter, role-based transitions, dashboard, seed data                        | `RecipeSpecifications`, `WorkflowAction`, `DashboardService`, `data.sql` |
| 5 CI config | Jenkins install/config for Windows & Linux, freestyle + pipeline guides      | `jenkins/` |
| 6 Pipeline as code | Declarative `Jenkinsfile` with Checkout/Build/Test/Deploy, port parameter | `Jenkinsfile`, `scripts/deploy-jar.*` |
| 7 Selenium | Selenium + WebDriverManager, 5 journeys, page objects, screenshot utility       | `src/test/java/.../selenium/` |
| 8 Continuous testing | Selenium stage, JUnit publishing, screenshot archiving, deploy gating | `Jenkinsfile` |
| 9 Docker | Multi-stage Dockerfile, `.dockerignore`, lifecycle guide                           | `Dockerfile`, `DOCKER.md`, `docker-compose.yml` |
| 10 Jenkins + Docker CD | Versioned image build, push, auto-deploy with rollback              | `Jenkinsfile`, `scripts/deploy-docker.*` |
| 11 Ansible | Inventory, playbook: JDK, Docker, folders, systemd, ports, env vars              | `ansible/` |
| 12 Idempotency & rollback | Health checks, block/rescue auto-rollback, rollback tags/playbook | `ansible/tasks/*.yml`, `ROLLBACK.md` |
| 13 Release & docs | Verified build, this report, tagged release                              | `FINAL_PROJECT_REPORT.md`, tag `v1.0.0` |

## 3. System architecture

### 3.1 Application

```
                 ┌───────────────────────────── Spring Boot (embedded Tomcat :8080) ──────────────────────────────┐
  Browser ─────► │ web.ui   RecipeUiController, DashboardUiController, LoginController  (Thymeleaf templates)    │
                 │                     │                                                                          │
  REST client ─► │ web.api  RecipeApiController, DashboardApiController, ApiExceptionHandler (JSON errors)       │
                 │                     ▼                                                                          │
                 │ service  RecipeService ── permissions (canEdit/canDelete/canPerform), transitions, search     │
                 │          DashboardService ── counts per status, top categories/cuisines                        │
                 │                     ▼                                                                          │
                 │ repository  RecipeRepository (+ Specifications), StatusHistoryRepository ── Spring Data JPA  │
                 │                     ▼                                                                          │
                 │ domain   Recipe ─1:N─ StatusHistory · RecipeStatus · WorkflowAction · Role                     │
                 │                                                                                                │
                 │ config   SecurityConfig: form login (UI) + HTTP Basic (API), CSRF for UI only, 401 for API    │
                 │ actuator /actuator/health (public), /actuator/info                                             │
                 └───────────────────────────────────────────────┬────────────────────────────────────────────────┘
                                                                 ▼
                                          H2 in-memory (default)  |  MySQL 8 (profile "mysql")
```

Design decisions:

- **State machine as data.** `WorkflowAction` declares each transition's source, target,
  allowed roles and whether only the owner may perform it. The service enforces it; the UI
  asks the same service which buttons to show, so UI and API can never disagree.
- **One input model** (`RecipeInput`) is validated identically for JSON and HTML forms.
- **Explicit errors.** 400 validation (with field errors), 401 unauthenticated API call,
  403 role/ownership violation, 404 unknown recipe, 409 invalid transition or stale version.
- **Optimistic locking** (`@Version`) plus an optional client-supplied version prevents lost updates.
- **Configuration through environment variables** (port, datasource, demo passwords, seeding),
  so the same artifact runs locally, in Docker and under Ansible.

### 3.2 Roles and workflow

| Action      | From → To                       | Allowed                        |
|-------------|---------------------------------|--------------------------------|
| Create/edit | — → `DRAFT` / edit own draft    | AUTHOR (owner), ADMIN (any)    |
| `SUBMIT`    | `DRAFT → PENDING_REVIEW`        | AUTHOR (owner), ADMIN          |
| `APPROVE`   | `PENDING_REVIEW → PUBLISHED`    | REVIEWER, ADMIN                |
| `REJECT`    | `PENDING_REVIEW → DRAFT`        | REVIEWER, ADMIN                |
| `UNPUBLISH` | `PUBLISHED → DRAFT`             | REVIEWER, ADMIN                |
| Delete      | any                             | ADMIN                          |

### 3.3 Deployment topology

```
 ┌──────────┐  push   ┌────────────┐  webhook/poll  ┌────────────────────────── Jenkins agent ──────────────────────────┐
 │Developer │───────► │ Git (main/ │──────────────► │ mvn package → mvn test → mvn test -Pselenium (headless Chrome)    │
 └──────────┘         │  develop)  │                │ docker build → docker push → deploy-docker.sh (health + rollback)  │
                      └────────────┘                └───────────────┬───────────────────────────────┬────────────────────┘
                                                                    │ image :BUILD_NUMBER           │ local deploy
                                                                    ▼                               ▼
                                                      ┌─────────────────────────┐        container "recipe-app"
                                                      │ Docker Hub / registry   │        on APP_PORT
                                                      └────────────┬────────────┘
                                                                   │ docker pull
                                       ansible-playbook --tags deploy -e app_version=N
                                                                   ▼
                     ┌──────────── staging-app-01 ────────────┐   ┌──────── prod-app-01 / prod-app-02 (serial: 1) ────────┐
                     │ systemd recipe-app.service → container │   │ systemd recipe-app.service → container                 │
                     │ /etc/recipe-app/{app,release}.env      │   │ health check → stable/previous markers → auto-rollback │
                     └────────────────────────────────────────┘   └────────────────────────────────────────────────────────┘
```

## 4. End-to-end DevOps pipeline

```
 ┌──────────┐   ┌────────────────┐   ┌────────────────┐   ┌───────────────┐   ┌──────────────┐   ┌─────────────┐   ┌──────────────────┐
 │ Checkout │──►│ Build & Package│──►│ Run Unit Tests │──►│ Selenium Test │──►│ Docker Build │──►│ Docker Push │──►│ Deploy           │
 │ git sha  │   │ mvn package    │   │ mvn test       │   │ mvn test      │   │ :BUILD_NUMBER│   │ (optional)  │   │ docker | jar     │
 │          │   │ -DskipTests    │   │ 50 tests       │   │ -Pselenium    │   │ + :latest    │   │ Hub / :5000 │   │ health check     │
 │          │   │ archive *.jar  │   │ JUnit report   │   │ 11 tests      │   │ OCI labels   │   │             │   │ auto-rollback    │
 │          │   │                │   │                │   │ screenshots   │   │              │   │             │   │ on APP_PORT      │
 └──────────┘   └────────────────┘   └────────────────┘   └───────────────┘   └──────────────┘   └─────────────┘   └──────────────────┘
        any stage fails ─────────────────────────────────────► pipeline stops: no image pushed, nothing deployed
```

| Stage           | Gate / evidence                                                                 |
|-----------------|---------------------------------------------------------------------------------|
| Build & Package | Compiles; `target/recipe-management-system.jar` archived with fingerprint       |
| Run Unit Tests  | Non-zero Maven exit fails the build; JUnit trend published                      |
| Selenium Test   | Same; only Selenium XML published (no double counting); `target/screenshots/**` archived |
| Docker Build/Push | Runs only if `currentBuild.currentResult == 'SUCCESS'`                       |
| Deploy          | Same guard; Docker health status must become `healthy` or the previous image is restored |

Parameters: `APP_PORT` (default 8085), `DEPLOY_MODE` (`docker`/`jar`/`none`),
`RUN_SELENIUM`, `PUSH_IMAGE`, `DOCKER_REGISTRY`, `DOCKER_CREDENTIALS_ID`.

The `run()` helper chooses `sh` or `bat` with `isUnix()`, and deploy scripts exist for both
bash and PowerShell, so the pipeline runs on Linux and Windows agents.

## 5. Testing strategy

| Level                 | Tooling                              | Tests | Scope |
|-----------------------|--------------------------------------|-------|-------|
| Domain unit           | JUnit 5                              | 3     | `WorkflowAction` state machine |
| Service unit          | Mockito                              | 18    | permissions, ownership, versions, transitions, dashboard aggregation |
| Repository            | `@DataJpaTest` (H2)                  | 5     | search specifications (incl. LIKE escaping), aggregate queries |
| API integration       | `@SpringBootTest` + MockMvc          | 13    | CRUD, 400/401/403/404/409, workflow, history, dashboard, public health |
| UI integration        | MockMvc + `@WithMockUser`            | 10    | every template renders; role-dependent buttons; validation; flash errors |
| Context               | `@SpringBootTest`                    | 1     | application context starts |
| **End-to-end**        | Selenium 4 + WebDriverManager, headless Chrome | **11** | five journeys below |

Selenium journeys (`mvn test -Pselenium`):

| # | Class                             | Journey |
|---|-----------------------------------|---------|
| 0 | `LoginSeleniumTest`               | valid login lands on list; invalid login shows error |
| 1 | `CreateRecipeSeleniumTest`        | author creates recipe → draft, visible in list; validation errors shown |
| 2 | `SearchRecipeSeleniumTest`        | partial title search; cuisine+status filters; reset; empty state |
| 3 | `UpdateRecipeWorkflowSeleniumTest`| edit → submit → reviewer rejects → author fixes → resubmit → approve; history verified |
| 4 | `DashboardSeleniumTest`           | counts add up and rankings sorted; new recipe increments counts; drill-down links |

Design: Page Object Model (`selenium/pages`), explicit waits only, unique titles per test
so tests sharing the DB don't interfere, fresh browser per test,
`ScreenshotOnFailureExtension` (an `AfterTestExecutionCallback`, which runs before
`@AfterEach` closes the browser) saving `target/screenshots/<Class>_<method>_<timestamp>.png`
plus the page HTML.

## 6. Step-by-step setup

### 6.1 Developer workstation

```bash
git clone <repo-url> recipe_management_system && cd recipe_management_system
./mvnw spring-boot:run                 # Windows: mvnw.cmd spring-boot:run
# http://localhost:8080  — author/author123, reviewer/reviewer123, admin/admin123
./mvnw test                            # 50 unit/integration tests
./mvnw test -Pselenium                 # 11 browser tests (Chrome required)
```

### 6.2 Docker

```bash
docker build -t recipe-management-system:local .
docker run -d --name recipe-app -p 8080:8080 recipe-management-system:local
docker inspect --format '{{.State.Health.Status}}' recipe-app
# MySQL stack:
SEED_DATA=always docker compose up -d --build
```

### 6.3 Jenkins

1. Install Jenkins, JDK 21, Maven, Git, Chrome, Docker (`jenkins/README.md`).
2. Configure tools `jdk21`, `maven3`; credential `dockerhub-credentials`.
3. Create a Pipeline job from SCM with script path `Jenkinsfile` (`jenkins/MAVEN_BUILD_GUIDE.md`).
4. *Build with Parameters* → e.g. `APP_PORT=8085`, `DEPLOY_MODE=docker`.

### 6.4 Ansible

```bash
cd ansible
ansible-galaxy collection install -r requirements.yml
# edit inventory.ini and group_vars/all.yml (app_image_repo), create a vault for secrets
ansible-playbook playbook.yml -e app_version=42            # provision + deploy
ansible-playbook playbook.yml --tags deploy -e app_version=43
ansible-playbook playbook.yml --tags rollback               # back to 42
```

## 7. Verification results (this release)

| Check                                                    | Result |
|----------------------------------------------------------|--------|
| `mvn clean package` (incl. 50 unit/integration tests)    | Passed |
| `mvn test -Pselenium` (11 Selenium tests, headless Chrome 156) | Passed |
| Failure screenshots written on test failure              | Verified (PNG + HTML in `target/screenshots/` during development) |
| `docker build` multi-stage image                         | Built; container runs as non-root `app`, reports `healthy` |
| REST API against the container (`/api/dashboard`)        | Returned seeded statistics |
| `scripts/deploy-docker.sh` / `.ps1` happy path           | Deployed and healthy |
| `scripts/deploy-docker.sh` rollback path                 | Broken image detected, previous image restored and healthy, exit 1 |
| `scripts/deploy-jar.sh` / `.ps1`                         | Deployed JAR, health check passed |
| `ansible-playbook --syntax-check` + `ansible-lint`       | Passed (production profile, 0 findings) |

Not executed in this environment: a live Jenkins controller run, a push to Docker Hub
(requires credentials), and an Ansible run against real target servers. The pipeline steps
and playbooks were exercised through their underlying commands/scripts and static checks.

## 8. Troubleshooting

| Area      | Symptom                                         | Resolution |
|-----------|-------------------------------------------------|------------|
| Build     | `invalid target release: 21`                    | `JAVA_HOME` / Jenkins `jdk21` tool must point to JDK 21 |
| App       | Port 8080 in use                                | `--server.port=9090` or `SERVER_PORT=9090` |
| App       | API returns 401                                 | Use HTTP Basic (`curl -u author:author123 ...`) |
| App       | API POST returns 403 from the browser session   | UI endpoints need the CSRF token; use `/api/**` with Basic auth for scripts |
| App       | 409 on transition                               | Action not valid from the current status (e.g. approving a draft) |
| Selenium  | `SessionNotCreatedException`                    | Install Chrome; WebDriverManager downloads the matching driver (falls back to Selenium Manager offline) |
| Selenium  | Typing/clicks ignored after login               | Chrome password bubbles; already disabled in `DriverFactory` — keep those options if you customise it |
| Selenium  | Fails only in CI containers                     | Use `--shm-size=512m`, run as non-root or keep `--no-sandbox`; inspect archived screenshots |
| Jenkins   | `docker: permission denied`                     | Add `jenkins` to the `docker` group and restart Jenkins |
| Jenkins   | Windows `bat` cannot find `mvn`                 | Configure the `maven3` tool, or use `mvnw.cmd` |
| Docker    | Container `unhealthy`                           | `docker logs recipe-app`; check env vars and memory limit |
| Docker    | `--mount requires BuildKit`                     | `DOCKER_BUILDKIT=1` or Docker ≥ 23 |
| Ansible   | `UNREACHABLE`                                   | Check `ansible_host`, `ansible_user`, SSH key; `ansible all -m ping` |
| Ansible   | Health check retries exhausted                  | `journalctl -u recipe-app -n 100`; the play rolls back automatically |
| Ansible   | `couldn't resolve module community.general.ufw` | `ansible-galaxy collection install -r requirements.yml` |
| Rollback  | "No previous release recorded"                  | First deploy on that host; pass `-e rollback_version=<tag>` |

## 9. Known limitations

- **Authentication** uses in-memory users from configuration; no registration, password reset or SSO.
- **H2 default** is in-memory: data resets on restart (use the `mysql` profile for persistence).
- **Schema management** relies on Hibernate `ddl-auto`; no versioned migrations yet.
- **No image upload**, ratings, comments or recipe versioning.
- **Single-node app state**: sessions are in-memory, so multi-node deployments behind a
  load balancer need sticky sessions or Spring Session.
- **Selenium** runs on Chrome only.
- **Pipeline** builds the image from source in Docker (re-compiles) instead of reusing the
  tested JAR; contents are the same commit, but it duplicates build time.
- **Ansible** targets systemd-based Linux hosts; it was statically validated but not run
  against real servers in this environment.

## 10. Future enhancements

1. Flyway migrations and MySQL as the default persistent store; Testcontainers-based integration tests.
2. OAuth2/OIDC login (Keycloak, Azure AD) with role mapping; per-user recipe visibility.
3. Recipe images (object storage), ratings/comments, favourites and nutrition data.
4. OpenAPI/Swagger UI (`springdoc-openapi`) and API versioning.
5. Observability: Micrometer → Prometheus/Grafana, structured JSON logs, tracing.
6. Supply-chain security: OWASP dependency-check / Trivy image scan stage, SBOM, signed images (cosign).
7. Quality gates: JaCoCo coverage threshold and SonarQube stage in the Jenkinsfile.
8. Cross-browser Selenium Grid (Chrome, Firefox, Edge) running in parallel.
9. Kubernetes manifests/Helm chart with readiness/liveness probes on the existing health groups;
   blue-green or canary releases.
10. Ansible role packaging (`roles/recipe_app`) with Molecule tests.

## 11. Repository history

Development followed the branching policy in the README: work happened on
`feature/core-crud`, `feature/mvp-workflow`, `feature/ci-pipeline`, `feature/selenium-tests`,
`feature/docker` and `feature/ansible`, each merged into `develop` with `--no-ff`; the release
was stabilised on `release/1.0.0`, merged into `main` and tagged `v1.0.0`.
Commits use Conventional Commit prefixes (`feat`, `test`, `ci`, `build`, `docs`, `chore`).

```bash
git log --oneline --graph --all
```
