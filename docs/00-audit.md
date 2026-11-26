# 00 — CapStore codebase audit

_Audit date: 2026-11-24. Scope: the whole repo at `main` (commit `0a5f020`) plus the uncommitted working tree. Read-only: no code was changed for this audit._

> **Heads-up on repo state.** The working tree has a large set of uncommitted changes, both staged and unstaged: deleted `.env.example`, a deleted `.github/workflows/ci.yml`, a deleted `backend/docker-compose.yml`, removed B2B quote code, and the exception-handling changes from the previous session. Commit or stash these to get a clean baseline **before** starting the migration (see Phase 0).

---

## 1. Tech stack (what is in use, not the target)

| Layer | In use | Target | Gap |
|---|---|---|---|
| Language | **Java 17** (`pom.xml` `java.version`) | Java 21 | upgrade |
| Framework | **Spring Boot 3.3.5** (web, data-jpa, security, validation, actuator, mail, thymeleaf) | Spring Boot 3 | ✓ (bump to latest 3.x) |
| ORM | Hibernate 6.5 (through Boot), `open-in-view: false`, `default_batch_fetch_size: 50` | Hibernate | ✓ |
| Database | **MySQL 8.0** (`mysql-connector-j`, `flyway-mysql`) | PostgreSQL | migrate |
| Migrations | Flyway, `V1`–`V10` in `backend/src/main/resources/db/migration` | — | must be ported to PG dialect |
| Cache | none | Redis | new |
| Messaging | none (in-process Spring `ApplicationEvent` + `@Async`) | Kafka | new |
| Auth | Spring Security + JJWT 0.12.6 (HS256, stateless) | — | see §5 |
| Payments | Razorpay Java SDK 1.4.8 behind the `PaymentGateway` interface | — | — |
| Storage | Local disk or S3 (AWS SDK v2 2.25.60) behind `StorageService` | — | — |
| Docs/PDF/Excel | springdoc-openapi 2.6.0, OpenPDF 1.3.43, Apache POI 5.3.0 | — | — |
| Observability | Actuator + Micrometer Prometheus registry; management port 8081 bound to 127.0.0.1 | OpenTelemetry | new |
| Build | **Maven** (no wrapper `mvnw` in repo), single module | — | add wrapper and multi-module |
| Frontend | **React 18.3 + Vite 5.4 + Tailwind 3.4**, React Router 6, axios, lucide-react; JavaScript (no TypeScript) | React | ✓ |
| Frontend tests | `node --test` with 1 file (`frontend/test/format.test.js`) | — | thin |
| Containers | Multi-stage Dockerfiles (backend on Temurin 17, frontend on nginx 1.27), root `docker-compose.yml` (mysql, mailpit, backend, frontend) | Docker + Kubernetes | no k8s manifests |
| CI | `.github/workflows/ci.yml` **deleted in the working tree** | — | restore |

## 2. Current architecture

**A single deployable monolith** (`capstore-backend`) plus a SPA served by nginx. nginx proxies `/api/` and `/uploads/` to the backend.

Packages are organised **by technical layer, not by domain**. Every domain shares one `entity`, one `repository` and one `service` package:

```
com.capstore
├── CapStoreApplication        @EnableScheduling @EnableAsync
├── config/       DataSeeder, OpenApiConfig, S3StorageConfiguration, SecurityConfig, WebConfig
├── controller/   21 controllers (public, account, admin, webhooks)
├── dto/          8 files of Java records (AdminDtos, AuthDtos, CartDtos, CatalogDtos, ExtraDtos, OrderDtos, ReturnDtos, PageResponse)
├── entity/       26 JPA entities + 11 enums
├── exception/    ApiException, GlobalExceptionHandler
├── repository/   22 Spring Data repositories
├── security/     JwtService, JwtAuthFilter, AuthFacade, RateLimitFilter, JsonSecurityErrorHandler
└── service/      ~40 classes (domain services, gateways, schedulers, listeners, storage)
```

**Coupling hot spots** (constructor dependencies) show where extraction will hurt most:

| Service | Depends on |
|---|---|
| `ReturnService` | **13**: ReturnRequest/Refund/Order/OrderItem/OrderStatusHistory/Shipment/Payment/ProductVariant repos, OrderService, TrackingService, PaymentGateway, AuthFacade, EventPublisher |
| `OrderService` | **11**: Order/Cart/ProductVariant/OrderStatusHistory repos, PricingService, InventoryService, CouponService, NotificationService, TrackingService, AuthFacade, StoreSettingsService |
| `PaymentService` | 8: Payment/Order repos, **OrderService, ReturnService, TrackingService**, gateway, … |
| `AdminService` | 7: spans orders, users, inventory, tracking, notifications |

Two call chains already behave like a distributed workflow, but they run in one DB transaction: `PaymentService → OrderService/ReturnService/TrackingService`, and `ReturnService → OrderService → InventoryService`.

**What's already good and worth keeping:** DTO records at the API boundary (no JPA entity is returned from a controller); a global exception handler; Flyway with `validate-on-migrate`; opaque UUID `publicId` for products, orders and returns; checkout idempotency key (`uk_orders_user_checkout_request`); idempotent payment webhook and refund keys; pessimistic locks on stock, payment, shipment and return rows; after-commit notification events with a de-dup log table; ports-and-adapters seams (`PaymentGateway`, `CourierClient`, `StorageService`, `S3ObjectStorage`); graceful shutdown; liveness/readiness probes; non-root container user.

## 3. Domain model

### Entities → tables

| Domain | Entity (table) | Key relationships |
|---|---|---|
| Identity | `User` (`users`) | role `USER`/`ADMIN`, `enabled` |
| | `Address` (`addresses`) | N:1 User |
| | `PasswordResetToken` (`password_reset_tokens`) | N:1 User, stores a token hash |
| Catalog | `Category` (`categories`) | `parentId` (a plain Long, not a relation) |
| | `Product` (`products`) | N:1 Category; 1:N ProductVariant (cascade); `@ElementCollection` imageUrls → `product_images`; `publicId`, `returnable` |
| | `ProductVariant` (`product_variants`) | N:1 Product; **`stockQty` lives here** (inventory mixed into catalog) |
| | `Review` (`reviews`) | N:1 Product, N:1 User; unique (user, product) |
| | `Banner` (`banners`) | — |
| Inventory | `InventoryLog` (`inventory_logs`) | `variantId` as a plain Long (good for splitting); FK in DB |
| Cart | `Cart` (`carts`) | 1:1 User; 1:N CartItem |
| | `CartItem` (`cart_items`) | N:1 Cart, **N:1 ProductVariant** |
| | `WishlistItem` (`wishlist_items`) | N:1 User, N:1 Product |
| Order | `CustomerOrder` (`orders`) | N:1 User; 1:N OrderItem; self-ref `originalOrder` (replacement orders); shipping address denormalised; money fields; `couponCode`; `gstRatePercent` snapshot |
| | `OrderItem` (`order_items`) | N:1 Order, **N:1 ProductVariant** (plus a name/sku/price snapshot) |
| | `OrderStatusHistory` (`order_status_history`) | `orderId` as a plain Long |
| | `Coupon` (`coupons`) | — (atomic `usedCount` update query) |
| | `StoreSettings` (`store_settings`) | singleton row (id = 1): GST %, shipping rates |
| Fulfilment | `Shipment` (`shipments`) | N:1 Order; N:1 ReturnRequest (reverse pickup); `direction` FORWARD/REVERSE |
| | `TrackingEvent` (`tracking_events`) | N:1 Shipment; unique (courier, eventId) |
| Returns | `ReturnRequest` (`return_requests`) | N:1 Order, N:1 OrderItem, N:1 replacement Order; `@Version` |
| Payment | `Payment` (`payments`) | **N:1 Order** |
| | `Refund` (`refunds`) | 1:1 ReturnRequest, N:1 Payment; idempotency key |
| Notification | `NotificationLog` (`notification_logs`) | `orderId` Long; unique (order, type, awb) used for de-dup |

Enums: `OrderStatus` (PENDING_PAYMENT → CONFIRMED → PROCESSING → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED, plus CANCELLED, RETURN_REQUESTED, RETURNED, REFUNDED), `PaymentStatus`, `ReturnStatus` (12 states), `ShipmentStatus`, `RefundStatus`, `ReturnType`, `OrderType`, `DiscountType`, `ShipmentDirection`, `Role`.

### Cross-domain foreign keys to break when splitting

`cart_items.variant_id → product_variants` · `order_items.variant_id → product_variants` · `orders.user_id → users` · `reviews.user_id/product_id` · `wishlist_items.user_id/product_id` · `payments.order_id → orders` · `refunds.payment_id → payments` and `refunds.return_request_id → return_requests` · `notification_logs.order_id → orders` · `inventory_logs.variant_id → product_variants` · `carts.user_id → users`.

### Migration history

V1 init · V2 coupons/addresses/reviews/wishlist/banners/reset tokens (+ quote_requests) · V3 product_images · V4 shipments/tracking/notification_logs · V5 opaque public ids · V6 store_settings · V7 checkout idempotency · V8 returns/refunds/reverse shipments · V9 indexes · V10 drop B2B (quote_requests, bulk_price_tiers, user company columns).

## 4. REST endpoints

Base path `/api/v1` unless noted. 🔓 = public, 👤 = authenticated user, 🛡 = ADMIN.

**Auth & profile** (`AuthController`, `AddressController`)
- 🔓 `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/forgot-password`, `/auth/reset-password`
- 👤 `GET /me`, `PUT /me`
- 👤 `GET/POST /me/addresses`, `PUT/DELETE /me/addresses/{id}`

**Catalog** (`CatalogController`, `ReviewController`, `StoreController`)
- 🔓 `GET /categories`, `GET /products` (search, filter, paging), `GET /products/{publicId}`
- 🔓 `GET /products/{productId}/reviews` · 👤 `POST /products/{productId}/reviews` (numeric id ⚠)
- 🔓 `GET /banners`, `GET /config`
- 👤 `POST /coupons/validate`, `GET /coupons/available`

**Cart & wishlist** (`CartController`, `WishlistController`)
- 👤 `GET /cart`, `POST /cart/items`, `PUT /cart/items/{itemId}`, `DELETE /cart/items/{itemId}`, `DELETE /cart`
- 👤 `GET /wishlist`, `POST /wishlist/{productId}`, `DELETE /wishlist/{productId}`

**Orders, tracking, invoices** (`OrderController`, `InvoiceController`, `OrderTrackingController`, `VersionedOrderReturnsController`)
- 👤 `POST /orders`, `GET /orders`, `GET /orders/{id}`, `GET /orders/{id}/tracking`, `POST /orders/{id}/cancel`, `GET /orders/{id}/invoice`, `GET /orders/{orderId}/returns`
- 👤 unversioned duplicates: `GET /api/orders/{orderId}/tracking`, `GET /api/orders/{orderId}/returns` ⚠

**Returns** (`ReturnController`, mapped to both `/api/returns` and `/api/v1/returns`)
- 👤 `POST /returns`, `GET /returns/{id}`, `GET /returns/orders/{orderId}`

**Payments** (`PaymentController`)
- 👤 `POST /payments/create-order`, `POST /payments/verify`
- 🔓 `POST /payments/webhook` (Razorpay HMAC signature)

**Webhooks & misc**
- 🔓 `POST /webhooks/courier/{courierName}` (HMAC `X-Signature`)
- 🔓 `POST /contact` (always rate-limited)
- 🔓 `GET /uploads/**` (static images, local storage)

**Admin** (🛡, under `/api/v1/admin` unless noted)
- Dashboard & customers: `GET /dashboard`, `GET /customers`, `PUT /customers/{id}/enabled`, `POST /admins`
- Catalog: `POST/PUT /categories[/{id}]`, `GET/POST /products`, `GET/PUT/DELETE /products/{id}`, `POST /products/{id}/variants`, `PUT /variants/{variantId}`
- Inventory: `POST /variants/{variantId}/stock`, `GET /variants/{variantId}/stock-log`, `GET /inventory/low-stock`
- Orders: `GET /orders`, `GET /orders/{id}`, `GET /orders/{id}/tracking`, `PUT /orders/{id}/status`, `PUT /orders/{id}/shipment`, `GET /orders/{id}/invoice`
- Returns: `GET /returns`, `POST /returns/{id}/approve | reject | quality-check | refund-confirmed`
- Store: `GET/PUT /store-settings`, `GET/POST/PUT /coupons[/{id}]`, `GET/POST/PUT/DELETE /banners[/{id}]`
- Reports: `GET /reports/{type}`, `GET /reports/{type}/export` (Excel)
- Uploads: `POST /api/v1/uploads` (image upload)

Ops: `/actuator/health/**`, `/actuator/prometheus` (port 8081), `/swagger-ui.html`, `/v3/api-docs/**`.

## 5. Auth, config, tests, Docker

**Auth**
- Stateless JWT (HS256, `JWT_SECRET` ≥ 32 bytes enforced). The access token and the refresh token are both JWTs carrying `type`. The subject is the **email**.
- `JwtAuthFilter` loads the user from the DB on **every** request (to check `enabled`), and `AuthFacade.currentUser()` loads it **again**: two user queries per authenticated request.
- Role checks: URL rule `/api/v1/admin/** → ROLE_ADMIN` plus `@PreAuthorize` on the admin controllers (`@EnableMethodSecurity`).
- The frontend stores both tokens in **`localStorage`** (`frontend/src/api.js`).
- Refresh tokens can't be revoked: there is no rotation store, no logout, and a password reset doesn't invalidate existing tokens.

**Config & secrets**
- All secrets come from env vars (`application.yml` uses `${DB_PASSWORD}`, `${JWT_SECRET}`, `${RAZORPAY_*}` and others). docker-compose fails fast with `:?` when required ones are missing. `.env` is git-ignored and is **not** tracked ✓.
- `.env.example` is **deleted (staged)**, but the README and the compose header still say `cp .env.example .env`.
- Business config is split between `application.yml` (`app.tax.gst-rate-percent`, `app.shipping.*`) and the `store_settings` table, so there are two sources of truth.
- Placeholder defaults are baked in: `BUSINESS_GSTIN:27AAAAA0000A1Z5`, address, phone. JDBC URL hard-codes `sslMode=DISABLED&allowPublicKeyRetrieval=true`.
- `app.jwt.refresh-minutes: 5` is **shorter** than `access-minutes: 15` (see §6).

**Tests**: 21 classes / 73 `@Test` in `backend/src/test`, all **unit tests with Mockito**. There's no `@SpringBootTest`, `@DataJpaTest` or Testcontainers, so Flyway migrations, JPQL queries, locking and security rules are never exercised against a real DB. The frontend has 1 test file. `mvn test` is green (73/73 as of the last session).

**Docker**: backend multi-stage build (runs `mvn verify` inside the image build), non-root uid 10001, healthcheck on the 8081 readiness probe, JVM container flags. Frontend: node 20 build into nginx 1.27. Compose: `mysql:8.0` (bound to 127.0.0.1:3307), `axllent/mailpit:latest` (unpinned), backend, frontend, named volumes. There are no Kubernetes manifests.

## 6. Code quality issues and risks

Ordered by impact. File references are relative to `backend/src/main/` unless noted.

| # | Severity | Issue | Where | Notes / fix direction |
|---|---|---|---|---|
| 1 | High | **Refresh token outlives nothing.** Refresh TTL 5 min < access TTL 15 min, so when the access token expires the refresh token has already expired. Users are logged out every ~15 min. | `resources/application.yml` (`app.jwt`) | Typical: access 15 min, refresh 7–30 days with rotation. |
| 2 | High | **Rate limiter keys on the nginx IP.** No `server.forward-headers-strategy`, so behind nginx `getRemoteAddr()` is the proxy's IP for every client. The `/contact` limit (always on) is **10 messages per minute for the whole site**. The auth limiter is off by default. | `security/RateLimitFilter.java`, `application.yml` | Set forward-headers strategy (trusted proxy only); move to Redis or the gateway later. |
| 3 | High | **No integration tests.** Migrations, JPQL, pessimistic locks, security URL rules and the webhook signature path are untested against a real DB. Porting to PostgreSQL without them is risky. | `backend/src/test` | Testcontainers before any DB or service move. |
| 4 | High | **God services and cyclic workflow coupling.** Payment → Order/Return/Tracking and Return → Order → Inventory all run in one transaction. These are the seams that become sagas. | `service/OrderService.java`, `ReturnService.java`, `PaymentService.java` | Introduce domain events and ports first (modular monolith), then split. |
| 5 | Med | **Cross-domain JPA associations** (`CartItem→ProductVariant`, `OrderItem→ProductVariant`, `Payment→CustomerOrder`, `Review→User`, …) make the domains inseparable at the ORM level. | `entity/*` | Replace with ID references plus snapshots per module. |
| 6 | Med | **Inventory is a column on the catalog table** (`product_variants.stock_qty`), reserved under `PESSIMISTIC_WRITE` inside the checkout transaction. The hot row is shared by catalog edits and checkout. | `OrderService.place`, `InventoryService.adjust`, `ProductVariantRepository` | Separate `inventory` table/module → inventory-service with reservations. |
| 7 | Med | **Order number collisions.** `"CAP" + date + random 5 digits`, with a unique key. On a collision the customer gets a 409 "Duplicate or invalid data" at checkout. | `OrderService.place` (and `RPL…` for replacements) | DB sequence or ULID. |
| 8 | Med | **In-process events only.** `@TransactionalEventListener` + `@Async` on the default executor, so a crash after commit loses the email. Schedulers (`OrderExpiryScheduler`, `ReturnProcessingScheduler`) run on **every** instance with no lock. | `service/*Listener`, `*Scheduler` | Outbox → Kafka; ShedLock or single-consumer jobs. |
| 9 | Med | **Two user DB lookups per request**, and the JWT subject is the email (changing an email silently breaks sessions). | `security/JwtAuthFilter.java`, `security/AuthFacade.java` | Put the user id in the JWT (`sub`), carry the principal; cache in Redis later. |
| 10 | Med | **Tokens in `localStorage`**: any XSS gives full account takeover, refresh token included. | `frontend/src/api.js` | httpOnly cookie for refresh (when the gateway lands). |
| 11 | Med | **Missing validation:** `PUT /cart/items/{id}` has no `@Valid` and `UpdateItemRequest(int quantity)` has no bounds, so any quantity can be set (add is capped at 10 000). | `controller/CartController.java`, `dto/CartDtos.java` | `@Valid` plus `@Min(0) @Max(…)`. |
| 12 | Low | **Mixed identifier styles.** Products, orders and returns use UUID `publicId`, but reviews and wishlist take the numeric `productId`. Cart items, addresses, variants, coupons and banners also use numeric ids. This leaks internal ids and makes the API inconsistent. | `ReviewController`, `WishlistController`, … | Standardise on public ids at service boundaries. |
| 13 | Low | **Duplicate unversioned routes** `/api/orders/*` and `/api/returns`, kept alongside `/api/v1/...`. | `OrderTrackingController`, `ReturnController` | Deprecate; route via the gateway. |
| 14 | Low | **Unbounded lists and in-memory reports.** `GET /admin/returns` returns all rows. Reports `findAll()` every variant and load all paid orders for a range. Batch fetch avoids classic N+1, but memory grows with data. | `ReturnService.allForAdmin`, `ReportService` | Paginate; SQL aggregation / read model. |
| 15 | Low | **Config duplication and placeholders**: GST/shipping in both YAML and DB; fake GSTIN default; `sslMode=DISABLED` hard-coded; `mailpit:latest` unpinned. | `application.yml`, `docker-compose.yml` | Single source; profile-specific JDBC params. |
| 16 | Low | **Repo hygiene**: CI workflow and `.env.example` deleted in the working tree; no Maven wrapper; README §5 still says the backend "could not compile"; `.DS_Store` files lying around (ignored). | root | Restore and refresh as part of Phase 0. |

N+1 check: `hibernate.default_batch_fetch_size: 50` is set and the hot listing query uses fetch joins, so **no severe N+1 was found**. Order and product listings will still issue one batched query per lazy collection. Verify with SQL logging in the integration tests.

Transactions: all mutating service methods found are `@Transactional`, so there's no "missing transaction" problem. The opposite problem is **transactions that are too wide** (checkout, payment verify and returns span several domains). That's what makes splitting hard.

## 7. Proposed service split

Rules: **database per service** (PostgreSQL schema or DB each). Services hold only IDs and snapshots of other domains' data. Sync calls go through the gateway or service clients for queries; state changes travel as Kafka events through an outbox.

| Target service | Owns (tables) | Moves from existing code | Publishes / consumes |
|---|---|---|---|
| **gateway** (Spring Cloud Gateway) | — | `SecurityConfig` CORS rules, `RateLimitFilter` (→ Redis rate limiter), JWT validation part of `JwtAuthFilter`, nginx `/api` routing, static `/uploads` route | — |
| **user-service** | users, addresses, password_reset_tokens, wishlist_items | `AuthController`, `AuthService`, `JwtService` (token issuing → switch to RS256 + JWKS so the gateway can verify), `AddressController/Service`, `PasswordResetService`, `AdminUserService`, customers part of `AdminCustomerController`/`AdminService`, `WishlistController/Service` (stores productId only) | pub `UserRegistered`, `PasswordResetRequested` |
| **product-service** | categories, products, product_images, product_variants (**without** stock), reviews, banners | `CatalogController`, `AdminCatalogController` (catalog parts), `CatalogService`, `ReviewController/Service`, `BannerService`, `UploadController` + `StorageService` impls, `ImageUploadValidator` | pub `ProductCreated/Updated`, `VariantPriceChanged`; con `OrderDelivered` (for "verified purchase" reviews; replaces `OrderItemRepository` use in `ReviewService`) |
| **inventory-service** | inventory (variantId, onHand, reserved), inventory_logs, reservations | `InventoryService`, stock endpoints + low-stock from `AdminCatalogController`, reservation logic from `OrderService.place/cancel` and `OrderExpiryScheduler` | con `OrderPlaced` → pub `StockReserved` / `StockReservationFailed`; con `OrderCancelled/Expired` → release; con `ReturnReceived` → restock; pub `LowStock` |
| **cart-service** | carts (Redis primary, optional PG) | `CartController`, `CartService`, `PricingService` (price from product-service / cached copy) | con `VariantPriceChanged`, `OrderConfirmed` (removes purchased items; replaces `removePurchasedItemsFromCart`) |
| **order-service** | orders, order_items, order_status_history, coupons, store_settings, shipments, tracking_events, return_requests | `OrderController`, `AdminOrderController`, `OrderService`, `CouponService`, `StoreController` (config/coupons), `StoreSettingsService`, `TrackingService`, `CourierClient`, `CourierWebhookController`, `OrderTrackingController`, `ReturnController`, `AdminReturnController`, `ReturnService` (non-refund parts), `ReturnTransitions`, `InvoiceService`, `ReportService`, dashboard part of `AdminService` | pub `OrderPlaced`, `OrderConfirmed`, `OrderCancelled`, `OrderShipped`, `OrderDelivered`, `ReturnApproved`, `RefundRequested`; con `PaymentCaptured/Failed`, `RefundCompleted`, `StockReserved/Failed` |
| **payment-service** | payments, refunds | `PaymentController` (incl. Razorpay webhook), `PaymentService`, `PaymentGateway`, `RazorpayPaymentGateway`, refund execution and retry from `ReturnService` + `ReturnProcessingScheduler` | pub `PaymentCaptured`, `PaymentFailed`, `RefundCompleted/Failed`; con `OrderPlaced` (amount), `RefundRequested` |
| **notification-service** | notification_logs (dedup), templates | `MailService`, `NotificationService`, `OrderNotificationListener`, `OrderNotificationEvent`, `OrderStatusMailEvent`, `templates/mail/*`, `ContactController/Service` | con almost every event above |

Fulfilment (shipments, tracking, returns) has no dedicated target service, so it stays as a **module inside order-service**. It's the natural candidate for a later `fulfilment-service` if you want one.

The admin SPA calls several services through the gateway. The dashboard becomes a small composition (gateway or BFF endpoint), or each service exposes its own admin stats.

## 8. Strangler migration order

Each step is small and leaves the system shippable. "Monolith" = the current backend.

1. **Freeze a baseline**: commit the current tree, restore `.env.example` and the CI workflow, add the Maven wrapper.
2. **Safety net**: Testcontainers integration tests for register → login → add to cart → checkout → payment webhook → confirmed → cancel/return. Run them on the current MySQL.
3. **Quick fixes** with tests: refresh TTL, forward headers for the rate limiter, `@Valid` on cart update, order-number generation.
4. **Java 21** toolchain and Docker base images (no code changes beyond what the compiler asks for).
5. **PostgreSQL**: port the Flyway scripts, switch the driver and compose service, and run the same integration tests on PG.
6. **Package by domain** inside the monolith (`com.capstore.{user,catalog,inventory,cart,order,payment,notification}`), a mechanical move only.
7. **Boundary rule test** (ArchUnit or Spring Modulith, *dependency needs approval*): no module touches another module's repositories or entities.
8. **Break cross-module entity references** one at a time (e.g. `Payment.order` → `orderId`), each with its own commit and migration.
9. **Extract `stock_qty` into an `inventory` table**, still inside the monolith, behind `InventoryService`.
10. **Replace direct cross-module calls with in-process domain events** (start with notifications, which are already events).
11. **Transactional outbox + Kafka** in compose; the monolith publishes events to Kafka (no consumers yet).
12. **OpenTelemetry** on the monolith + collector + Jaeger/Grafana in compose.
13. **Gateway in front of the monolith** (100 % of routes → monolith; JWT and CORS still in the monolith). Point nginx at the gateway.
14. **Extract notification-service** (consumes Kafka only, no sync callers). Turn off the monolith's mail listener.
15. **Extract product-service** (read-heavy). Gateway routes `/products`, `/categories`, `/banners`, `/reviews`, `/uploads`. Redis cache for listings.
16. **Extract inventory-service** with a reservation saga (OrderPlaced → StockReserved/Failed), replacing the in-transaction lock.
17. **Extract cart-service** on Redis.
18. **Extract payment-service**, with the Razorpay webhook routed through the gateway and refunds driven by events.
19. **Extract user-service**, switch to RS256/JWKS, and move JWT validation to the gateway.
20. **What remains is order-service**: rename and slim the monolith. Delete dead code, drop the legacy `/api/*` routes.
21. **Kubernetes** manifests/Helm, probes on 0.0.0.0, secrets, HPA; refresh the frontend env and auth (httpOnly refresh cookie).

## 9. Proposed phased roadmap (pending approval)

| Phase | Goal | Covers steps | Exit criteria |
|---|---|---|---|
| **0. Baseline & hygiene** | Clean, reproducible starting point | 1 | Green CI on `main`, `.env.example` back, README accurate |
| **1. Safety net & quick fixes** | Lock current behaviour in tests; fix the known bugs | 2–3 | Testcontainers suite covers checkout, payment and returns; issues #1, #2, #7, #11 closed |
| **2. Platform upgrade** | Java 21 + PostgreSQL | 4–5 | Same suite green on PG 16; compose uses Postgres |
| **3. Modular monolith** | Domain packages with enforced boundaries | 6–8 | Boundary test passes; no cross-module entity references |
| **4. Inventory + events in-process** | Inventory separated; domain events replace direct calls | 9–10 | `OrderService` no longer calls Inventory, Notification or Tracking directly |
| **5. Kafka + outbox + observability** | Reliable async backbone you can see | 11–12 | Events visible in Kafka; one trace spans HTTP → DB → Kafka |
| **6. Gateway (strangler facade)** + first extraction | Gateway in front; notification-service live | 13–14 | All traffic via the gateway; emails sent by the new service |
| **7. Product service + Redis cache** | First synchronous service extracted | 15 | Catalog served by product-service; cache-hit metrics |
| **8. Inventory & checkout saga** | Distributed reservation with compensation | 16 | Oversell and timeout tests pass; no cross-service DB transaction |
| **9. Cart + payment services** | Redis cart, isolated payments and refunds | 17–18 | Razorpay sandbox end-to-end via the gateway; idempotency tests |
| **10. User service + auth at the edge** | RS256/JWKS, gateway auth, order-service is what remains | 19–20 | Monolith retired / renamed to order-service |
| **11. Kubernetes & hardening** | Deploy the whole platform on k8s (kind/minikube) | 21 | `kubectl apply` or Helm brings it up; probes, HPA, secrets, dashboards |

### Decisions (approved 2026-11-24)
- Roadmap Phases 0–11 approved as above.
- **No MySQL data migration**: Phase 2 starts from a fresh PostgreSQL schema plus seed data.
- **Module boundaries: Spring Modulith**, to be re-proposed and approved at Phase 3. Every other dependency is approved at the phase that needs it.
- **Repo layout: Maven multi-module monorepo.**
- **Fulfilment and returns stay inside order-service.**
- Phase 0 finding: `backend/src/main/resources/application-prod.yml` was missing from source (only a stale copy in `target/classes`); restored in Phase 0.

### Original open questions (answered above)
1. **Existing MySQL data**: does any real data need migrating to PostgreSQL, or can Phase 2 start from a fresh schema plus seed data?
2. **Dependencies** I expect to propose (each needs your OK when we get there): Testcontainers, ArchUnit *or* Spring Modulith, Spring Cloud Gateway, Spring Kafka, Spring Data Redis, OpenTelemetry Java agent, ShedLock, Resilience4j.
3. **Repo layout for services**: one Maven multi-module monorepo (recommended for learning) or a separate repo per service?
4. **Fulfilment/returns**: keep them inside order-service (as proposed), or plan a separate fulfilment-service?
