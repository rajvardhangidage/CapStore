# CapStore backend

Spring Boot 3 · Java 17 · MySQL 8 · Flyway · JWT.  See the root `README.md` for the full guide.

The project loads the ignored `application-local.yml` file automatically when present. It points to `localhost:3306/capstore` and keeps your database password out of tracked source files. Set `JWT_SECRET`, `ADMIN_EMAIL`, and `ADMIN_PASSWORD` in the environment or in that ignored local config before starting; there are no built-in credentials. The root `.env` file is used by Docker Compose and is not automatically loaded by `mvn spring-boot:run`. Start the backend from this directory with:

```bash
mvn spring-boot:run
```

Flyway creates and updates the tables on startup. The local config file is excluded from Git; update it if your database name, port, or credentials change.

```bash
mvn test                         # unit tests
```

If you run the full app with root `docker compose`, its backend connects to the host MySQL using `host.docker.internal`; configure the database connection values in the root `.env` file. The Compose stack does not start a MySQL container. MySQL must be configured to accept connections from Docker Desktop for this mode; running Spring Boot directly on the host avoids that network requirement.

The root Compose setup starts Mailpit for development email: browse messages at http://localhost:8025 (SMTP on port 1025). Set `COURIER_WEBHOOK_SECRET` in the root `.env` to enable signed courier callbacks. Each JSON callback must include `eventId`, `awb`, and `status`; it may also include `description`, `expectedDelivery` (`YYYY-MM-DD`), and `occurredAt` (ISO date/time). Sign the exact raw request body with HMAC-SHA256 and send the hex digest in `X-Signature` (optionally prefixed with `sha256=`). Supported statuses are `CREATED`, `SHIPPED`, `IN_TRANSIT`, `OUT_FOR_DELIVERY`, and `DELIVERED`.

Swagger UI: http://localhost:8080/swagger-ui.html
