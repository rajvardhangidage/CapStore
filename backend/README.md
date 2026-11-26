# CapStore backend

Spring Boot 3 · Java 17 · MySQL 8 · Flyway · JWT.  See the root `README.md` for the full guide.

The backend reads database and application credentials from environment variables; it does not load a local credential file. Set `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, and `JWT_SECRET` before starting. `CONTACT_EMAIL` receives messages submitted through the public contact form. The root `.env` file is used by Docker Compose and is not automatically loaded by `mvn spring-boot:run`. To seed a local development database, explicitly set `APP_SEED_ENABLED=true`, `ADMIN_EMAIL`, and `ADMIN_PASSWORD`. Seeding is disabled by default and in the `prod` profile.

```bash
./mvnw spring-boot:run           # Maven wrapper: only a JDK 17+ is required
```

Flyway creates and updates the tables on startup. Use `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD` to configure a local MySQL instance.

For production, activate `SPRING_PROFILES_ACTIVE=prod` and set `CORS_ORIGINS` to the exact HTTPS frontend origin(s). ECS should resolve Secrets Manager values into environment variables at task startup. The production profile requires database connection settings, enables JDBC certificate/hostname verification and the application rate limiter, disables sample-data seeding and Razorpay simulation, and disables Swagger/OpenAPI. Do not pass secret values as build arguments or commit them to source.

Actuator health probes are available at `/actuator/health/liveness` and `/actuator/health/readiness`; readiness includes database health. Prometheus metrics are available at `/actuator/prometheus`. The management server listens on port `8081` and defaults to loopback only. Keep that port off the public ALB; if Prometheus needs network access, set `MANAGEMENT_ADDRESS` deliberately and restrict access to trusted monitoring sources. Graceful shutdown allows up to 30 seconds for in-flight requests. Hikari pool size is configurable with `DB_POOL_MAX_SIZE`; size it across all application tasks to stay within the database connection budget.

Flyway validates migrations and refuses clean or out-of-order migration by default. Production disables Flyway in ordinary app tasks; run a one-off migration task with `FLYWAY_ENABLED=true` and a dedicated least-privilege migration account supplied through `FLYWAY_USER` and `FLYWAY_PASSWORD` before deploying the new application version. Migration files already applied to an environment must not be edited; add new versioned migrations instead. The current migration set has been reviewed through V8, with query-oriented indexes in V9 and removal of the former B2B quote/bulk-pricing tables and user business columns in V10.

Local uploads remain on disk by default. The `prod` profile switches to S3-compatible storage and requires `S3_BUCKET`, `AWS_REGION`, and `S3_PUBLIC_BASE_URL`; `S3_ENDPOINT` is optional for compatible endpoints. The application uses the AWS SDK default credential chain, so on ECS it should receive credentials from its task role. Set the public base URL to a CDN/custom domain that can read the private bucket; configure bucket encryption, lifecycle/retention, and a CloudFront origin access policy outside the application.

```bash
./mvnw test                      # unit tests (Mockito; no database needed)
./mvnw verify                    # tests + packaged jar (what CI and the Docker build run)
```

The root `docker compose` starts a self-contained local stack, including MySQL; its backend connects to the `mysql` service and waits for the database health check before starting. Configure credentials in the ignored root `.env` file. The database is persisted in the `capstore-dev-mysql-data` volume and published only on localhost port `3307`.

The backend Dockerfile builds and tests with Java 17, then runs the packaged application on the `eclipse-temurin:17-jre` runtime as UID 10001. Its readiness health check uses the loopback-only management port; the local Compose service health check uses the same endpoint. Set `IMAGE_TAG` to a source revision for a versioned local build. For releases, publish immutable source-revision tags and deploy by registry digest.

The root Compose setup starts Mailpit for development email: browse messages at http://localhost:8025 (SMTP on port 1025). Set `COURIER_WEBHOOK_SECRET` in the root `.env` to enable signed courier callbacks. Each JSON callback must include `eventId`, `awb`, and `status`; it may also include `description`, `expectedDelivery` (`YYYY-MM-DD`), and `occurredAt` (ISO date/time). Sign the exact raw request body with HMAC-SHA256 and send the hex digest in `X-Signature` (optionally prefixed with `sha256=`). Supported statuses are `CREATED`, `SHIPPED`, `IN_TRANSIT`, `OUT_FOR_DELIVERY`, and `DELIVERED`.

Swagger UI: http://localhost:8080/swagger-ui.html
