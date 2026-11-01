# Recipe Management System

A Spring Boot application for authoring, reviewing and publishing recipes, delivered
with a complete DevOps toolchain: Jenkins pipeline-as-code, Selenium end-to-end tests,
Docker images and Ansible-based deployment with automatic rollback.

| Area            | Technology                                                    |
|-----------------|---------------------------------------------------------------|
| Language        | Java 21                                                       |
| Framework       | Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator, Thymeleaf) |
| Build           | Maven (wrapper included: `mvnw` / `mvnw.cmd`)                 |
| Database        | H2 in-memory (default) · MySQL 8 (`mysql` profile)            |
| Server          | Embedded Tomcat                                               |
| Testing         | JUnit 5, Spring MockMvc, Selenium WebDriver + WebDriverManager |
| CI/CD           | Jenkins declarative pipeline (`Jenkinsfile`)                  |
| Containers      | Docker multi-stage build (`Dockerfile`)                       |
| Config mgmt     | Ansible (`ansible/`)                                          |

---

## Features

- **Recipe CRUD** — create, view, update and (admin) delete recipes via UI and REST API.
- **Search & filter** — by title (partial match), cuisine, category and status, paginated.
- **Role-based workflow** — `DRAFT → PENDING_REVIEW → PUBLISHED`, with reject/unpublish
  paths and a full status-change audit trail.
- **Dashboard** — total recipes, counts per status, top categories and cuisines.
- **Ops-ready** — `/actuator/health`, env-var configuration, Docker health check.

## Architecture

```
Browser ──► Thymeleaf UI controllers ─┐
                                      ├─► Services (CRUD, permissions, workflow, dashboard) ─► Spring Data JPA ─► H2 / MySQL
REST    ──► /api/** controllers ──────┘
            Spring Security: form login (UI) + HTTP Basic (API) · roles AUTHOR / REVIEWER / ADMIN
```

```
src/main/java/com/recipes/rms
├── config/        Security configuration, user properties
├── domain/        Recipe, RecipeStatus, StatusHistory, WorkflowAction
├── repository/    JPA repositories and search Specifications
├── service/       RecipeService, DashboardService, exceptions
└── web/
    ├── api/       REST controllers, DTOs, JSON error handler
    └── ui/        Thymeleaf controllers and form objects
```

Full requirements, data model and API specification:
[`architecture/SRS_SUMMARY.md`](architecture/SRS_SUMMARY.md).
End-to-end report: [`FINAL_PROJECT_REPORT.md`](FINAL_PROJECT_REPORT.md).

## Repository layout

| Path                      | Contents                                              |
|---------------------------|-------------------------------------------------------|
| `architecture/`           | SRS summary                                           |
| `src/`                    | Application code, templates, seed data, tests         |
| `jenkins/`                | Jenkins installation & job setup guides               |
| `Jenkinsfile`             | Declarative CI/CD pipeline                            |
| `Dockerfile`, `DOCKER.md` | Container image and lifecycle guide                   |
| `ansible/`                | Inventory, playbook, templates, rollback              |
| `ROLLBACK.md`             | Rollback procedures                                   |

---

## Local setup

### Prerequisites

- JDK 21 (`java -version`)
- Maven 3.9+ — or use the bundled wrapper `./mvnw` (Linux/macOS) / `mvnw.cmd` (Windows)
- Google Chrome — only for Selenium tests
- Docker — optional, for container runs

### Run

```bash
./mvnw spring-boot:run
```

Open <http://localhost:8080> and sign in with a demo user:

| Username   | Password      | Roles                       |
|------------|---------------|-----------------------------|
| `author`   | `author123`   | AUTHOR                      |
| `reviewer` | `reviewer123` | REVIEWER                    |
| `admin`    | `admin123`    | ADMIN, AUTHOR, REVIEWER     |

These are demo credentials for local use. Override them in any shared environment with
`APP_AUTHOR_PASSWORD`, `APP_REVIEWER_PASSWORD`, `APP_ADMIN_PASSWORD` (and `*_USER`).

The H2 database is seeded from `src/main/resources/data.sql` on every start.

### Build and run the JAR

```bash
./mvnw clean package
java -jar target/recipe-management-system.jar --server.port=9090
```

### Run against MySQL

```bash
docker run -d --name recipes-mysql -e MYSQL_DATABASE=recipes -e MYSQL_USER=recipes \
  -e MYSQL_PASSWORD=recipes -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 mysql:8.4
SPRING_PROFILES_ACTIVE=mysql SEED_DATA=always ./mvnw spring-boot:run
```

Use `SEED_DATA=always` only on the first start against an empty schema.

### Configuration reference

| Variable                  | Default                               | Purpose                        |
|---------------------------|---------------------------------------|--------------------------------|
| `SERVER_PORT`             | `8080`                                | HTTP port                      |
| `SPRING_PROFILES_ACTIVE`  | *(none)*                              | `mysql` to use MySQL           |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | H2 in-memory           | Datasource                     |
| `JPA_DDL_AUTO`            | `create-drop` (H2) / `update` (MySQL) | Schema management              |
| `SEED_DATA`               | `always` (H2) / `never` (MySQL)       | Run `data.sql`                 |
| `H2_CONSOLE`              | `false`                               | Enable `/h2-console`           |

### Tests

```bash
./mvnw test                 # unit + integration tests (fast, no browser)
./mvnw test -Pselenium      # Selenium end-to-end journeys (needs Chrome)
./mvnw test -Pselenium -Dselenium.headless=false   # watch the browser
```

Failure screenshots are written to `target/screenshots/`; JUnit XML reports to
`target/surefire-reports/`.

### API quick test

```bash
curl -u author:author123 "http://localhost:8080/api/recipes?cuisine=Italian"
curl -u author:author123 -H "Content-Type: application/json" -X POST http://localhost:8080/api/recipes \
  -d '{"title":"Lemon Rice","cuisine":"Indian","category":"Main Course","ingredients":"rice\nlemon","instructions":"Mix."}'
curl -u author:author123 -H "Content-Type: application/json" -X POST http://localhost:8080/api/recipes/1/transitions \
  -d '{"action":"SUBMIT"}'
curl -u reviewer:reviewer123 http://localhost:8080/api/dashboard
```

### Docker / Jenkins / Ansible

- Docker: see [`DOCKER.md`](DOCKER.md)
- Jenkins: see [`jenkins/README.md`](jenkins/README.md) and the root [`Jenkinsfile`](Jenkinsfile)
- Ansible deployment and rollback: see [`ansible/README.md`](ansible/README.md) and [`ROLLBACK.md`](ROLLBACK.md)

---

## Branching policy

We use a lightweight Git Flow:

| Branch           | Purpose                                         | Merges into        |
|------------------|-------------------------------------------------|--------------------|
| `main`           | Production-ready, tagged releases (`vX.Y.Z`)    | —                  |
| `develop`        | Integration branch; always builds green         | `main` (release)   |
| `feature/<name>` | New work, e.g. `feature/dashboard`              | `develop`          |
| `fix/<name>`     | Non-urgent bug fixes                            | `develop`          |
| `hotfix/<name>`  | Urgent production fixes, branched from `main`   | `main` and `develop` |
| `release/<x.y.z>`| Release hardening (optional)                    | `main` and `develop` |

Rules:

1. Never commit directly to `main` or `develop`; open a pull request.
2. PRs need one approval and a green Jenkins build (unit + Selenium).
3. Merge feature branches with `--no-ff` so features stay visible in history.
4. Tag every release on `main` (`git tag -a v1.1.0 -m "..."`); the Jenkins build number
   becomes the Docker image tag.

## Contribution guidelines

1. Create an issue using the bug or feature template in `.github/ISSUE_TEMPLATE/`.
2. Branch from `develop`: `git checkout -b feature/<short-name> develop`.
3. Write code plus tests. Keep controllers thin; business rules belong in services.
4. Use [Conventional Commits](https://www.conventionalcommits.org/):
   `feat:`, `fix:`, `docs:`, `test:`, `ci:`, `build:`, `refactor:`, `chore:`.
5. Run `./mvnw test` and `./mvnw test -Pselenium` before pushing.
6. Open a PR against `develop` using the PR template and link the issue.
7. Never commit secrets; use environment variables or Ansible Vault.

## License

Internal project — all rights reserved.
