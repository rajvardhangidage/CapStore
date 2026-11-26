# CLAUDE.md — CapStore

## Project
CapStore is an e-commerce store for caps, built as a **learning project**. It is evolving from a Spring Boot monolith into an
event-driven microservices platform using the **strangler pattern**.

- **Today:** a single Spring Boot 3.3 / Java 17 / MySQL 8 backend (`backend/`) and a React 18 + Vite + Tailwind SPA (`frontend/`),
  run with `docker compose up --build` (mysql, mailpit, backend, nginx frontend).
- **Target:** Java 21, Spring Boot 3, Hibernate, PostgreSQL, Redis, Kafka, Docker, Kubernetes, OpenTelemetry, React.
  Services: gateway, user, product, cart, inventory, order, payment, notification.
- **Source of truth for the plan:** `docs/00-audit.md` (audit, service split, migration order, phased roadmap).
  Later phase docs go in `docs/NN-*.md`.

## Commands
- Backend tests: `cd backend && ./mvnw test` · full check (what CI runs): `./mvnw verify`
- Full local stack: `cp .env.example .env && docker compose up --build` (store :80, API :8080, Mailpit :8025)
- Frontend: `cd frontend && npm ci && npm run dev` · tests: `npm test` · build: `npm run build`
- Swagger UI: `http://localhost:8080/swagger-ui.html`, Mailpit: `http://localhost:8025`

## Backend layout (current, layer-based; becomes domain-based in Phase 3)
`com.capstore.{config,controller,dto,entity,exception,repository,security,service}`
- Controllers return **DTO records** from `dto/`, never JPA entities.
- Errors: throw `ApiException` (`badRequest/notFound/conflict/unauthorized` or a status); `GlobalExceptionHandler`
  and `JsonSecurityErrorHandler` render `{timestamp,status,message,errors}`.
- Schema changes only through new **Flyway** migrations (`db/migration/V{n}__*.sql`); never edit an applied migration;
  `ddl-auto: none`.
- External systems sit behind interfaces (`PaymentGateway`, `CourierClient`, `StorageService`). Keep it that way.
- Public resources use opaque UUID `publicId` in URLs; don't expose new numeric ids.

## Working rules
1. **Plan first.** For any non-trivial change, write or update the plan (in the relevant `docs/` file or in chat)
   and get approval before editing code. One roadmap step at a time; don't run ahead into the next phase.
2. **Small, focused commits.** One logical change per commit, and the build must stay green after each one.
   Never mix a refactor (move/rename) with a behaviour change. Don't commit or push unless asked.
3. **Ask before adding dependencies.** Any new Maven/npm dependency, Docker image, or infrastructure component needs
   explicit approval, with a one-line reason and the alternative considered.
4. **Tests required.** Every behaviour change or bug fix comes with tests (unit tests, plus integration tests with
   Testcontainers once they exist). Run the suite before declaring a step done and report the real result.
5. **Strangler discipline.** The monolith keeps working at every step. New services take traffic only through the gateway,
   one route group at a time, with a way to roll back.
6. **Service boundaries.** A service owns its tables. No cross-service DB joins or foreign keys; reference other
   domains by ID plus a snapshot. State changes cross services as events (transactional outbox → Kafka), not by sharing a DB.
7. **Events** are versioned, idempotent to consume (dedupe by event id), and documented in `docs/`.
8. **Secrets** come only from env vars or k8s secrets. Never commit `.env`, keys or credentials; keep `.env.example` current.
9. **Don't silently change public API contracts** (paths, payload shapes) that the frontend uses. If one must change,
   update the frontend in the same step.
