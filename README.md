# cycling-lab

A personal cycling-training lab. Combines a long-term rider profile, weekly
plans, FIT files, training notes, screenshots, reviews, and reusable workout
files. Two layers coexist in this repository:

- **legacy** — the existing `profile/`, `plans/`, `training/`, `review/`,
  `workouts/`, `fitting/` Markdown / FIT content at the repo root.
- **web (M0+)** — `backend/` (Spring Boot 3.3, Java 17) and `frontend/`
  (React 19 + Vite + TS), plus a `docker-compose.yml` for one-shot setup.

See [`doc/ARCHITECTURE.md`](doc/ARCHITECTURE.md) for the full design.

## Status

| Milestone | Description | Commit |
| --- | --- | --- |
| M1 | Rider profile + reusable workout library | `d4194d8` |
| M2 | ZWO workout file generator + persistence | `055a721` |
| M3 | FIT upload + analyzer (Garmin SDK), inline charts, training list/detail UI | `49a35ce` |
| M4 | Weekly / phase reviews with a Markdown editor + GFM preview | `473b083` |
| M7 | Admin user management (list, role/status, delete) | `3c023a1` |

Tests: **56 backend + 41 frontend** (all green). See "Tests" below.

## Directory layout

```text
cycling-lab/
├── backend/                 # Spring Boot 3.3 + Java 17
├── frontend/                # React 19 + Vite + TS
├── doc/                     # architecture design notes
├── docker-compose.yml       # postgres / redis / minio / backend / frontend
├── profile/                 # rider profile (legacy Markdown)
├── plans/                   # weekly plans + daily plans (legacy)
├── training/                # per-week FIT files + notes (legacy)
├── review/                  # weekly / phase reviews (legacy)
├── workouts/                # importable workout files (legacy)
├── fitting/                 # bike fitting notes (legacy)
├── scripts/                 # helper scripts
└── skills/                  # Codex project skill descriptions
```

## Quick start (M0 baseline)

### Prerequisites

- **JDK 17+**
- **Maven 3.9+**
- **Node.js 22+** and **npm 10+**
- **Docker + Docker Compose** (only required for the Docker flow)

### A) Local (no Docker)

Start the backend:

```bash
cd backend
mvn spring-boot:run
```

> Requires `postgres`, `redis`, `minio` to be reachable. Easiest path:
> `docker compose up -d postgres redis minio` and then start the Spring
> process.

Start the frontend:

```bash
cd frontend
npm install
npm run dev
```

- Frontend: <http://localhost:5173>
- Backend: <http://localhost:8080>
- OpenAPI / Swagger UI: <http://localhost:8080/swagger-ui.html>
- MinIO console: <http://localhost:9001> (`minioadmin` / `minioadmin`)

### B) Docker Compose one-shot

```bash
docker compose up --build
```

Open <http://localhost:5173>. There is no seed user — register from the UI
to create your first account, then promote it to ADMIN with a SQL update or
by using the admin account created during bootstrap.

### Default ports

| Service | Port | Notes |
| --- | --- | --- |
| Frontend (nginx) | 5173 | |
| Backend (Spring) | 8080 | |
| PostgreSQL | 5432 | `cycling_lab` / `cycling_lab` |
| Redis | 6379 | |
| MinIO API | 9000 | `minioadmin` / `minioadmin` |
| MinIO console | 9001 | |

## Tests

```bash
# Backend (uses an in-memory H2 database)
cd backend
mvn test

# Frontend
cd frontend
npm install
npm test
```

A passing run looks like:

```text
[INFO] Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
 Test Files  7 passed (7)
      Tests  41 passed (41)
```

## Notable design choices

- **No Python at runtime.** The FIT parser is pure Java (Garmin FIT SDK
  21.205.0) so the backend has no external script dependency. The legacy
  Python `analyze_fit.py` skill is kept for local Obsidian-friendly notes
  but does not run on the server.
- **Multi-tenant isolation via Hibernate `@Filter`.** Every service
  automatically enables the `tenantFilter` Hibernate filter and the JPA
  queries are also constrained by `user_id` at the repository level
  (defense in depth).
- **Local filesystem storage in M3**, abstracted behind
  `StorageService`. A MinIO / S3 implementation can be dropped in later
  without changing the training or workout services.
- **Markdown round-trip for reviews.** The detail page uses
  `react-markdown` + `remark-gfm` for a live preview; the saved content
  is plain Markdown so it can be exported to Obsidian without conversion.