# CapStore: online store for a caps business

A complete e-commerce website with a customer storefront and an admin panel.

> **Status:** this monolith is being evolved, step by step, into an event-driven microservices platform (a learning project).
> The audit, target service split and phased roadmap are in [`docs/00-audit.md`](docs/00-audit.md); working rules are in [`CLAUDE.md`](CLAUDE.md).

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, Tailwind CSS, React Router (mobile-first) |
| Backend | Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA |
| Database | MySQL 8 with Flyway migrations |
| Payments | Razorpay (UPI, cards, netbanking, wallets) |
| Extras | GST invoice PDFs (OpenPDF), Excel reports (Apache POI), email notifications |

```
capstore/
├── backend/            Spring Boot API   (src/main/java/com/capstore/...)
├── frontend/           React app (storefront + /admin)
├── docs/               audit and migration roadmap
├── .github/workflows/  CI (backend `mvnw verify`, frontend test + build)
├── docker-compose.yml  MySQL + backend + frontend + Mailpit in one command
└── .env.example        every setting you can change (local placeholders)
```

## 1. Run it (5 minutes)

**Option A: complete local Docker stack.** Needs Docker Desktop; Compose starts MySQL, the backend, frontend, and Mailpit.
```bash
cp .env.example .env        # placeholder credentials, for local development only
docker compose up --build
```
The first build takes a few minutes: the backend image runs the full test suite (`mvn verify`) while building. MySQL data persists in the `capstore-dev-mysql-data` Docker volume; the database port is bound to localhost at `3307`. Flyway creates the application tables at backend startup. Because `.env.example` sets `APP_SEED_ENABLED=true`, sample categories, products, a coupon, a banner and the admin account (`ADMIN_EMAIL` / `ADMIN_PASSWORD`) are created on first start; set it to `false` to start empty. Payments run in simulation mode (`RAZORPAY_DEV_MODE=true`) so checkout works without Razorpay keys. Change every placeholder secret for your own machine and never reuse them in production. To reset the local database, run `docker compose down` and then `docker volume rm capstore-dev-mysql-data` (this deletes the local database).
- Store: http://localhost
- Admin panel: http://localhost/admin  (login with `ADMIN_EMAIL` / `ADMIN_PASSWORD` configured in `.env`)
- API docs (Swagger): http://localhost:8080/swagger-ui.html
- Development email inbox (Mailpit): http://localhost:8025

The Compose stack is for local development only. The backend image uses a Java 17 multi-stage build and runs as a non-root user.

For a versioned backend image, build from the repository root with the source commit as the tag (avoid mutable tags such as `latest`):
```bash
GIT_SHA="$(git rev-parse --short=12 HEAD)"
docker build --pull \
  --build-arg VERSION="$GIT_SHA" \
  --build-arg VCS_REF="$(git rev-parse HEAD)" \
  -t "capstore-backend:$GIT_SHA" ./backend
```
Use a registry-enforced immutable SHA tag and pin the pushed image digest when deploying.

To try authenticated API endpoints in Swagger, use `POST /api/v1/auth/login` to get an access token, click **Authorize**, and paste the access token. Swagger sends it as a Bearer token; log in again to get a new token after it expires.

**Option B: run the backend directly for development.** Use environment variables for local credentials; the backend does not load a local credential file. Configure `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, and `JWT_SECRET` in your shell. To create the initial admin and sample catalog in a development database, also set `APP_SEED_ENABLED=true`, `ADMIN_EMAIL`, and `ADMIN_PASSWORD`. Keep seeding disabled in production; an admin account must be provisioned through a controlled process.

```bash
docker compose up -d mysql mailpit      # MySQL on localhost:3307 (or use your own MySQL), email inbox at :8025
cd backend && ./mvnw spring-boot:run    # start in this terminal and wait for "Started CapStoreApplication"
```
With the Compose MySQL, use `DB_HOST=localhost DB_PORT=3307` and the `DB_USER`/`DB_PASSWORD` from `.env`; with your own MySQL, create the `capstore` database and a least-privilege application user first. The Maven wrapper (`./mvnw`) downloads the right Maven version, so only a JDK 17+ is needed. `JWT_SECRET` must be at least 32 random characters. Set `CONTACT_EMAIL` if you want contact inquiries delivered; Compose falls back to `BUSINESS_EMAIL` if it is not set. Flyway creates the tables on first start. With seeding explicitly enabled, the backend creates the initial admin (only if it does not already exist), sample categories/products, a coupon, and a banner.

**Production configuration:** activate the Spring `prod` profile with `SPRING_PROFILES_ACTIVE=prod`. Supply `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, and `JWT_SECRET` at runtime. The production JDBC URL verifies the database server certificate and hostname. On ECS, configure the task definition to resolve secret values from AWS Secrets Manager into environment variables (`DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, and, when used, the Razorpay, SMTP, and webhook secrets). Grant the task execution role access only to those specific secret ARNs. Do not store secret values in Git, Docker build arguments, image layers, or Terraform source. The app does not call Secrets Manager directly; ECS injects the values when starting the task. Production disables sample-data seeding and payment simulation.

In another terminal, wait until `curl -fsS http://localhost:8080/api/v1/config` returns the store configuration, then start Vite:
```bash
cd frontend && npm ci && npm run dev    # http://localhost:5173, proxies /api and /uploads to :8080
```

**Tests and checks** (the same steps CI runs on every push and pull request, see `.github/workflows/ci.yml`):
```bash
cd backend && ./mvnw verify                       # compile, unit tests, package
cd frontend && npm ci && npm test && npm run build
```

## 2. What is included

**Customer (user) module**
- Register / login / logout, forgot and reset password by email; while the app is open, refresh tokens are renewed before they expire
- Home page (banners, categories, new arrivals), search, filters (category, price), sorting, pagination
- Product page: variants (size/colour), live stock, reviews (verified buyers only), wishlist
- Cart that works before login; it merges into the account cart when the customer logs in
- Checkout with address book, coupon codes, GST and shipping shown before paying
- Razorpay payment, order confirmation page and email
- Orders list, order tracking (progress bar + full history), cancel, return/refund/replacement requests, GST invoice PDF download

**Admin module** (`/admin`)
- Dashboard: today's and monthly sales, orders by status, low-stock list
- Orders: filter by status, update status with a note (customer gets an email), add courier and tracking number, download invoice, refund flag
- Products: add/edit, images, categories, variants, show/hide
- Returns: approve/reject requests, arrange reverse pickup, record quality checks, and confirm COD refunds
- Inventory: stock per variant, adjust with a reason, full stock history log
- Customers: list customers and admins, disable accounts, add other admins
- Coupons (percent or flat, minimum order, cap, expiry, usage limit), home banners
- Shipping & tax: manage the flat shipping charge, free-shipping threshold, and GST rate
- Reports with Excel export: daily sales, product-wise, category-wise, GST report, inventory value

**Safety built in:** stock is reserved at checkout and released on cancel or after 30 minutes unpaid; payment signatures are verified on the server and a configured Razorpay webhook covers closed browsers; late payments on cancelled orders are flagged for refund; no card data ever touches your server; passwords are BCrypt-hashed; admin APIs are role-protected. The in-memory auth rate limiter is disabled by default for development; enable `app.security.rate-limit.enabled` outside local testing.

Customer-facing product and order detail URLs use opaque UUID references instead of sequential database IDs. Order endpoints also enforce authenticated ownership; opaque URLs are not a replacement for authorization.

## 3. Going live checklist

1. **Razorpay:** payment simulation is disabled by default. For local-only simulation, explicitly set `RAZORPAY_DEV_MODE=true`; never enable it in production. For real payments, leave it `false`, fill `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET`, and test with Razorpay test keys first. In the Razorpay dashboard add a webhook to `https://YOUR-DOMAIN/api/v1/payments/webhook` for events `payment.captured` and `order.paid`, and put that webhook's secret in `RAZORPAY_WEBHOOK_SECRET`. Rebuild/restart the backend after changing `.env`.
2. **Secrets:** set a long random `JWT_SECRET`, a strong `DB_PASSWORD`, and a unique strong `ADMIN_PASSWORD`. Keep all credentials out of Git. Admin credentials are only used to create the initial admin account; reset an existing account's password through the application.
3. **GST:** fill the `BUSINESS_*` values (GSTIN, address, state), then set the GST rate and shipping amounts from **Admin → Shipping & tax**. Confirm the correct GST rate for caps with your accountant. Prices you enter are before GST.
4. **Shipping:** configure the flat charge and free-shipping threshold from **Admin → Shipping & tax**. These settings apply to new orders; existing order amounts and invoice tax rates are retained.
5. **Email:** local Docker runs include Mailpit at http://localhost:8025 (SMTP on localhost:1025). For real mail, set `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_AUTH`, and `SMTP_STARTTLS`.
6. **HTTPS:** put the site behind HTTPS (Cloudflare, Caddy, nginx + Let's Encrypt, or a cloud load balancer) and set `PUBLIC_URL` to the https address.
7. **Backups:** schedule daily backups of the MySQL volume (or use managed MySQL such as AWS RDS) and of the `uploads` volume.
8. Add your real products, photos and categories, then place a test order end to end (see checklist below).

## 4. Acceptance test checklist (UAT)

- [ ] Browse, search and filter caps on a phone and a desktop
- [ ] Add to cart while logged out, log in, cart is still there
- [ ] Place an order with a coupon, pay (test mode), receive confirmation
- [ ] Admin sees the order, marks it shipped with a tracking number, customer sees it in the timeline
- [ ] Customer downloads the GST invoice; CGST/SGST vs IGST is right for the delivery state
- [ ] Customer cancels a paid order: stock returns, order shows "refund pending", admin refunds in Razorpay and sets status Refunded
- [ ] Leave an order unpaid for 30+ minutes: it auto-cancels and stock returns
- [ ] Admin reports match the orders you placed; Excel export opens

## 5. Honest notes and limits

- **Testing status:** the backend compiles and its 73 unit tests pass (`./mvnw verify`); the frontend has a small `node --test` suite and builds cleanly; `docker compose up --build` starts the full stack from `.env.example`. There are **no integration tests yet**: database queries, migrations and security rules are not exercised against a real database by the test suite. Adding them (Testcontainers) is the next step in the roadmap.
- Local development stores product images on disk (`uploads`). The production Spring profile uses S3-compatible object storage; uploaded URLs use `S3_PUBLIC_BASE_URL` (normally a CloudFront hostname). Keep the bucket private and configure its read policy for the CDN; the ECS task role needs only object-write permissions for the configured bucket/prefix.
- Multiple product images can be uploaded in the admin editor; the first image is the cover and customers can browse the gallery on the product page. One warehouse; shipping within India with a flat-rate rule.
- Refunds are done manually in the Razorpay dashboard; the app tracks the status. Courier tracking is manual (courier + number + link); automatic courier APIs (Shiprocket, Delhivery) are a possible next step.
- SMS/WhatsApp notifications are not included (email only).
- Admins all have the same access (no separate Order Manager / Inventory Manager roles yet).
- The optional rate limiter is in memory (fine for one server). Use a gateway or Redis if you run several servers.

## 6. API overview

Base path `/api/v1`. Full interactive docs at `/swagger-ui.html`.

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/forgot-password`, `/auth/reset-password`, `GET /me` |
| Catalog | `GET /products`, `/products/{id}`, `/categories`, `/banners`, `/config`, `GET/POST /products/{id}/reviews` |
| Cart | `GET /cart`, `POST /cart/items`, `PUT/DELETE /cart/items/{id}`, `POST /coupons/validate` |
| Orders | `POST /orders`, `GET /orders`, `/orders/{id}`, `/orders/{id}/tracking`, `/orders/{id}/returns`, `/orders/{id}/invoice`, `POST /orders/{id}/cancel` |
| Returns | `POST /returns`, `GET /returns/{id}`, `GET /returns/orders/{orderId}` (owner-only; request body includes order ID, order item ID, type, reason and quantity). Legacy unversioned aliases `/api/returns/...` and `/api/orders/{orderId}/returns` still work |
| Payments | `POST /payments/create-order`, `/payments/verify`, `/payments/webhook` |
| Tracking | `GET /api/v1/orders/{id}/tracking` (also `/api/orders/{id}/tracking`, owner-only); courier callbacks at `POST /webhooks/courier/{courierName}` with an HMAC-SHA256 `X-Signature` |
| Account | `GET/POST/PUT/DELETE /me/addresses`, `GET/POST/DELETE /wishlist` |
| Admin | `/admin/dashboard`, `/admin/orders`, `/admin/products`, `/admin/variants`, `/admin/returns` (list, approve/reject, quality check, COD refund confirmation), `/admin/inventory/low-stock`, `/admin/customers`, `/admin/coupons`, `/admin/banners`, `/admin/store-settings`, `/admin/reports/{type}` (+ `/export`), `/admin/admins`, `POST /uploads` (images) |

## 7. Order lifecycle

`PENDING_PAYMENT → CONFIRMED → PROCESSING → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED`
Return flow: `REQUESTED → APPROVED → PICKUP_SCHEDULED → PICKED_UP → RECEIVED → QC_PASSED → REFUND_INITIATED → REFUNDED` or `REPLACEMENT_CREATED → CLOSED`. Admin can reject requests or record failed quality checks. A refund is recorded from the original item price after allocating the order discount; payment refunds are confirmed by the signed payment webhook. Admins configure return eligibility per product in the product editor. Every order change is logged and shown to the customer.
