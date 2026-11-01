# Recipe Management System — SRS Summary

| Item            | Value                                                        |
|-----------------|--------------------------------------------------------------|
| Document        | Software Requirements Specification (summary)                |
| Version         | 1.0                                                          |
| Product         | Recipe Management System (RMS)                               |
| Tech stack      | Java 21, Spring Boot 3.5, Maven, H2 (default) / MySQL 8, Embedded Tomcat, Thymeleaf |

---

## 1. Purpose and Scope

RMS lets a small editorial team author, review and publish recipes. Authors write
recipes as drafts and submit them for review; reviewers approve (publish) or reject
them. Everyone can search the catalogue, and a dashboard summarises the state of the
catalogue.

**In scope:** recipe CRUD, search/filter, role-based status workflow with audit trail,
summary dashboard, REST API, server-rendered web UI, health endpoint for operations.

**Out of scope (v1):** self-service user registration, image uploads, ratings/comments,
multi-tenancy, external identity providers.

---

## 2. Architecture

### 2.1 Logical view

```
                ┌──────────────────────────── Spring Boot application (fat JAR, embedded Tomcat :8080) ───────────────────────────┐
 Browser  ───►  │  Web UI controllers (Thymeleaf)  ─┐                                                                           │
                │                                   ├─►  Service layer  ──►  Spring Data JPA repositories  ──►  H2 / MySQL      │
 REST client ─► │  REST controllers  (/api/**)     ─┘     • RecipeService (CRUD, search, permissions)                          │
                │                                         • WorkflowAction (state machine + role rules)                        │
                │  Spring Security (form login + HTTP Basic, in-memory users, roles AUTHOR / REVIEWER / ADMIN)                   │
                │  Spring Boot Actuator (/actuator/health, /actuator/info)                                                       │
                └────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Layering and packages (`com.recipes.rms`)

| Package        | Responsibility                                                      |
|----------------|---------------------------------------------------------------------|
| `domain`       | JPA entities and enums (`Recipe`, `RecipeStatus`, `StatusHistory`, `WorkflowAction`) |
| `repository`   | Spring Data JPA repositories + JPA `Specification`s for search      |
| `service`      | Business rules: CRUD, permissions, workflow transitions, dashboard  |
| `web.api`      | REST controllers, DTOs, global JSON error handling                  |
| `web.ui`       | Thymeleaf MVC controllers and form objects                          |
| `config`       | Security configuration and user setup                               |

### 2.3 Deployment view

```
 Developer ─► Git ─► Jenkins (Checkout → Build → Unit tests → Selenium tests → Docker build/push → Deploy)
                                                                         │
                                       Docker registry (Docker Hub / local registry:5000)
                                                                         │
                                  Ansible ─► target nodes: Docker + systemd unit running the container
                                             health check → automatic rollback to last stable image
```

---

## 3. Actors and Roles

| Role       | Demo user  | Capabilities                                                             |
|------------|------------|--------------------------------------------------------------------------|
| `AUTHOR`   | `author`   | Create recipes; edit **own** recipes while `DRAFT`; submit own drafts    |
| `REVIEWER` | `reviewer` | Approve or reject recipes in `PENDING_REVIEW`; unpublish `PUBLISHED`     |
| `ADMIN`    | `admin`    | Everything above on any recipe, plus edit in any status and delete       |

All authenticated users may view and search every recipe and the dashboard.
Demo passwords are configured in `application.yml` and overridable through environment
variables (see README).

---

## 4. Functional Requirements

| ID    | Requirement                                                                                     |
|-------|-------------------------------------------------------------------------------------------------|
| FR-01 | Users authenticate via a login form (UI) or HTTP Basic (API).                                  |
| FR-02 | An AUTHOR or ADMIN can create a recipe; new recipes start in `DRAFT`, `createdBy` = current user. |
| FR-03 | Any user can view a recipe including its status history.                                       |
| FR-04 | Recipe updates follow the edit rule in §3; violations return HTTP 403.                          |
| FR-05 | Users can search by title (contains, case-insensitive), cuisine, category and status; results are paginated and sorted by last update. |
| FR-06 | Status changes happen only through workflow actions (§5); each change is recorded in the history with actor, timestamp and optional comment. |
| FR-07 | Invalid transitions return HTTP 409; unauthorised transitions return HTTP 403.                 |
| FR-08 | The dashboard shows total recipes, count per status, top 5 categories and top 5 cuisines.       |
| FR-09 | Only ADMIN can delete a recipe.                                                                 |
| FR-10 | `/actuator/health` is reachable without authentication for load balancers and Ansible checks.   |

## 5. Status Workflow

```
            SUBMIT (AUTHOR-owner, ADMIN)              APPROVE (REVIEWER, ADMIN)
   ┌───────┐ ───────────────────────────► ┌────────────────┐ ─────────────────────────► ┌───────────┐
   │ DRAFT │                              │ PENDING_REVIEW │                            │ PUBLISHED │
   └───────┘ ◄─────────────────────────── └────────────────┘                            └───────────┘
       ▲         REJECT (REVIEWER, ADMIN)                                                     │
       └──────────────────────────────────────────────────────────────────────────────────────┘
                                   UNPUBLISH (REVIEWER, ADMIN)
```

| Action      | From             | To               | Allowed roles                  |
|-------------|------------------|------------------|--------------------------------|
| `SUBMIT`    | `DRAFT`          | `PENDING_REVIEW` | AUTHOR (owner only), ADMIN     |
| `APPROVE`   | `PENDING_REVIEW` | `PUBLISHED`      | REVIEWER, ADMIN                |
| `REJECT`    | `PENDING_REVIEW` | `DRAFT`          | REVIEWER, ADMIN                |
| `UNPUBLISH` | `PUBLISHED`      | `DRAFT`          | REVIEWER, ADMIN                |

## 6. Non-Functional Requirements

| ID     | Category        | Requirement                                                             |
|--------|-----------------|-------------------------------------------------------------------------|
| NFR-01 | Portability     | Runs anywhere with JDK 21, or as a Docker container.                    |
| NFR-02 | Performance     | Search over 10k recipes responds < 500 ms (indexed status/cuisine/category). |
| NFR-03 | Security        | All routes except login, static assets and health require authentication; CSRF protection on UI forms. |
| NFR-04 | Reliability     | Optimistic locking (`@Version`) prevents lost updates.                  |
| NFR-05 | Operability     | Health endpoint, structured logs to stdout, configuration via env vars. |
| NFR-06 | Testability     | Unit/integration tests plus Selenium end-to-end journeys in CI.         |
| NFR-07 | Deployability   | Immutable versioned Docker images; one-command rollback via Ansible.    |

---

## 7. Data Model

### 7.1 ER diagram

```
┌──────────────────────────────┐        1      *  ┌───────────────────────────────┐
│ recipes                      │──────────────────►│ recipe_status_history         │
├──────────────────────────────┤                   ├───────────────────────────────┤
│ id            BIGINT PK      │                   │ id           BIGINT PK        │
│ title         VARCHAR(150) * │                   │ recipe_id    BIGINT FK *      │
│ description   VARCHAR(1000)  │                   │ from_status  VARCHAR(20)      │
│ cuisine       VARCHAR(60)  * │                   │ to_status    VARCHAR(20) *    │
│ category      VARCHAR(60)  * │                   │ action       VARCHAR(20) *    │
│ ingredients   CLOB         * │                   │ changed_by   VARCHAR(50) *    │
│ instructions  CLOB         * │                   │ comment      VARCHAR(500)     │
│ prep_minutes  INT            │                   │ changed_at   TIMESTAMP *      │
│ cook_minutes  INT            │                   └───────────────────────────────┘
│ servings      INT            │
│ status        VARCHAR(20)  * │   indexes: idx_recipe_status, idx_recipe_cuisine, idx_recipe_category
│ created_by    VARCHAR(50)  * │
│ created_at    TIMESTAMP    * │   (* = NOT NULL)
│ updated_at    TIMESTAMP    * │
│ version       BIGINT         │
└──────────────────────────────┘
```

### 7.2 Validation rules

| Field          | Rule                                   |
|----------------|----------------------------------------|
| `title`        | required, 3–150 characters             |
| `cuisine`      | required, ≤ 60 characters              |
| `category`     | required, ≤ 60 characters              |
| `ingredients`  | required (one ingredient per line)     |
| `instructions` | required                               |
| `prepMinutes`, `cookMinutes` | 0–1440, optional         |
| `servings`     | 1–100, optional                        |

---

## 8. API Specification

Base path `/api`. JSON in/out. Auth: HTTP Basic. Errors use a common body:
`{"timestamp": "...", "status": 409, "error": "Conflict", "message": "...", "path": "/api/..."}`.

| Method | Path                                  | Roles            | Description                                    | Success |
|--------|---------------------------------------|------------------|------------------------------------------------|---------|
| GET    | `/api/recipes`                        | any              | Search. Query: `title`, `cuisine`, `category`, `status`, `page`, `size`, `sort` | 200 page |
| GET    | `/api/recipes/{id}`                   | any              | Get a recipe                                   | 200     |
| POST   | `/api/recipes`                        | AUTHOR, ADMIN    | Create recipe (status `DRAFT`)                 | 201 + `Location` |
| PUT    | `/api/recipes/{id}`                   | owner/ADMIN      | Update recipe content                          | 200     |
| DELETE | `/api/recipes/{id}`                   | ADMIN            | Delete recipe                                  | 204     |
| POST   | `/api/recipes/{id}/transitions`       | per §5           | Body `{"action":"SUBMIT","comment":"..."}`     | 200     |
| GET    | `/api/recipes/{id}/history`           | any              | Status history, newest first                   | 200     |
| GET    | `/api/dashboard`                      | any              | Statistics                                     | 200     |
| GET    | `/actuator/health`                    | public           | Liveness/readiness                             | 200     |

**Create / update request**

```json
{
  "title": "Paneer Butter Masala",
  "description": "Creamy tomato curry",
  "cuisine": "Indian",
  "category": "Main Course",
  "ingredients": "250g paneer\n2 tomatoes\n50g butter",
  "instructions": "1. Make the gravy...\n2. Add paneer...",
  "prepMinutes": 15,
  "cookMinutes": 30,
  "servings": 4
}
```

**Recipe response** — request fields plus `id`, `status`, `createdBy`, `createdAt`, `updatedAt`, `version`.

**Dashboard response**

```json
{
  "totalRecipes": 12,
  "countByStatus": { "DRAFT": 4, "PENDING_REVIEW": 3, "PUBLISHED": 5 },
  "topCategories": [ { "name": "Main Course", "count": 5 } ],
  "topCuisines":   [ { "name": "Indian", "count": 4 } ]
}
```

Status codes: 400 validation error, 401 unauthenticated, 403 forbidden, 404 not found,
409 invalid workflow transition or optimistic-lock conflict.

## 9. Web UI Routes

| Route                         | Purpose                                         |
|-------------------------------|-------------------------------------------------|
| `/login`                      | Login form                                      |
| `/recipes`                    | List + search/filter form                       |
| `/recipes/new`                | Create form                                     |
| `/recipes/{id}`               | Details, workflow buttons, history              |
| `/recipes/{id}/edit`          | Edit form                                       |
| `/dashboard`                  | Statistics                                      |

---

## 10. Use Cases

| UC    | Name                 | Actor     | Main flow                                                                                       | Alternate / error flows |
|-------|----------------------|-----------|-------------------------------------------------------------------------------------------------|-------------------------|
| UC-01 | Log in               | Any       | Enter credentials → redirected to recipe list                                                  | Bad credentials → error message |
| UC-02 | Create recipe        | AUTHOR    | Open *New Recipe* → fill form → save → recipe shown in `DRAFT`                                  | Validation errors shown inline |
| UC-03 | Search recipes       | Any       | Enter title / pick cuisine, category, status → results table filtered                          | No match → "No recipes found" |
| UC-04 | View recipe          | Any       | Click recipe → details + history                                                               | Unknown id → 404 page |
| UC-05 | Update recipe        | AUTHOR    | Open own draft → *Edit* → change → save                                                         | Not owner / not draft → 403 |
| UC-06 | Submit for review    | AUTHOR    | On own draft click *Submit for review* → status `PENDING_REVIEW`                                | Wrong state → 409 message |
| UC-07 | Approve / reject     | REVIEWER  | Open pending recipe → *Approve* (→ `PUBLISHED`) or *Reject* with comment (→ `DRAFT`)            | AUTHOR attempting → 403 |
| UC-08 | Unpublish            | REVIEWER  | Open published recipe → *Unpublish* → `DRAFT`                                                    | — |
| UC-09 | View dashboard       | Any       | Open *Dashboard* → totals, per-status counts, top categories/cuisines                          | Empty DB → zeros |
| UC-10 | Delete recipe        | ADMIN     | Recipe details → *Delete* → confirm → removed                                                   | Non-admin → button hidden, API 403 |

## 11. Acceptance Criteria (traceability)

| Use case | Automated test                                                       |
|----------|----------------------------------------------------------------------|
| UC-02    | `CreateRecipeSeleniumTest`, `RecipeApiControllerTest`                |
| UC-03    | `SearchRecipeSeleniumTest`, `RecipeApiControllerTest`                |
| UC-05–07 | `UpdateRecipeWorkflowSeleniumTest`, `RecipeServiceTest`, `WorkflowActionTest` |
| UC-09    | `DashboardSeleniumTest`, `DashboardServiceTest`                      |
| UC-01    | `LoginSeleniumTest`                                                  |
