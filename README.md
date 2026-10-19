# CapStore: online store for a caps business (B2C + B2B)

A complete e-commerce website with a customer storefront and an admin panel.

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
├── docker-compose.yml  MySQL + backend + frontend in one command
├── .env.example        every setting you can change
└── .github/workflows/  CI (builds + tests on every push)
```

## 1. Run it (5 minutes)

**Option A: Docker app with your locally installed MySQL.** Needs Docker Desktop and a running local MySQL server.
```bash
cp .env.example .env        # set JWT_SECRET and your local MySQL credentials
docker compose up --build
```
The Compose stack does not start a MySQL container. Its backend connects to your computer's MySQL at `host.docker.internal:3306`; ensure MySQL accepts connections from Docker Desktop. Flyway creates the application tables in the existing `capstore` database.
- Store: http://localhost
- Admin panel: http://localhost/admin  (login: `ADMIN_EMAIL` / `ADMIN_PASSWORD` from `.env`, default `admin@capstore.local` / `Admin@12345`)
- API docs (Swagger): http://localhost:8080/swagger-ui.html
- Development email inbox (Mailpit): http://localhost:8025

To try authenticated API endpoints in Swagger, use `POST /api/v1/auth/login` to get an access token, click **Authorize**, and paste the access token. Swagger sends it as a Bearer token; log in again to get a new token after it expires.

**Option B: for developers.**
```bash
docker compose up -d mailpit            # optional: local email inbox at :8025
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev  # site on :5173 (proxies /api to :8080)
```
Create the `capstore` database in your locally installed MySQL first. The backend automatically loads your ignored `backend/application-local.yml` credentials file; update that file if your MySQL username, password, or port differ. Flyway creates the tables on first start; the backend also creates the admin user, three categories, two sample caps (with bulk tiers), a `WELCOME10` coupon and a home banner so you can click around immediately.

## 2. What is included

**Customer (user) module**
- Register / login / logout, forgot and reset password by email; while the app is open, refresh tokens are renewed before they expire
- Home page (banners, categories, new arrivals), search, filters (category, price), sorting, pagination
- Product page: variants (size/colour), live stock, bulk-price table, reviews (verified buyers only), wishlist
- Cart that works before login; it merges into the account cart when the customer logs in
- Checkout with address book, coupon codes, GST and shipping shown before paying
- Razorpay payment, order confirmation page and email
- Orders list, order tracking (progress bar + full history), cancel, return/refund/replacement requests, GST invoice PDF download
- **Business (B2B) accounts:** GST number at signup, admin approval, automatic bulk prices, quote requests with logo upload, accept/decline quotes

**Admin module** (`/admin`)
- Dashboard: today's and monthly sales, orders by status, low-stock list
- Orders: filter by status, update status with a note (customer gets an email), add courier and tracking number, download invoice, refund flag
- Products: add/edit, images, categories, variants, bulk price tiers, show/hide
- Returns: approve/reject requests, arrange reverse pickup, record quality checks, and confirm COD refunds
- Inventory: stock per variant, adjust with a reason, full stock history log
- Customers: retail and business lists, approve business accounts, disable accounts, add other admins
- Coupons (percent or flat, minimum order, cap, expiry, usage limit), home banners, quote responses
- Shipping & tax: manage the flat shipping charge, free-shipping threshold, and GST rate
- Reports with Excel export: daily sales, product-wise, category-wise, B2B vs B2C, GST report, inventory value

**Safety built in:** stock is reserved at checkout and released on cancel or after 30 minutes unpaid; payment signatures are verified on the server and a configured Razorpay webhook covers closed browsers; late payments on cancelled orders are flagged for refund; no card data ever touches your server; passwords are BCrypt-hashed; admin APIs are role-protected. The in-memory auth rate limiter is disabled by default for development; enable `app.security.rate-limit.enabled` outside local testing.

Customer-facing product and order detail URLs use opaque UUID references instead of sequential database IDs. Order endpoints also enforce authenticated ownership; opaque URLs are not a replacement for authorization.

## 3. Going live checklist

1. **Razorpay:** payment simulation is disabled by default. For local-only simulation, explicitly set `RAZORPAY_DEV_MODE=true`; never enable it in production. For real payments, leave it `false`, fill `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET`, and test with Razorpay test keys first. In the Razorpay dashboard add a webhook to `https://YOUR-DOMAIN/api/v1/payments/webhook` for events `payment.captured` and `order.paid`, and put that webhook's secret in `RAZORPAY_WEBHOOK_SECRET`. Rebuild/restart the backend after changing `.env`.
2. **Secrets:** set a long random `JWT_SECRET`, a strong `DB_PASSWORD`, and change the admin password immediately after first login.
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
- [ ] Business signup, admin approves, bulk price appears in the cart at the tier quantity
- [ ] Business requests a quote with a logo, admin responds, customer accepts
- [ ] Admin reports match the orders you placed; Excel export opens

## 5. Honest notes and limits

- **Testing status:** the React app was built successfully. The Java backend was written and syntax-checked, but I could not compile or run it in my environment (no access to Maven Central). Run `mvn test` and start it once; if anything fails to compile or start, send me the error and I will fix it quickly.
- Product images and logos are stored on the server disk (`uploads`). To use S3 or Cloudinary, only `StorageService.java` needs to change.
- Multiple product images can be uploaded in the admin editor; the first image is the cover and customers can browse the gallery on the product page. One warehouse; shipping within India with a flat-rate rule.
- Refunds are done manually in the Razorpay dashboard; the app tracks the status. Courier tracking is manual (courier + number + link); automatic courier APIs (Shiprocket, Delhivery) are a possible next step.
- Accepted quotes are followed up by you directly; they do not auto-create an order.
- SMS/WhatsApp notifications are not included (email only).
- Admins all have the same access (no separate Order Manager / Inventory Manager roles yet).
- The optional rate limiter is in memory (fine for one server). Use a gateway or Redis if you run several servers.
- Bulk tiers are matched per cart line (quantity of one size/colour), not across sizes of the same cap.

## 6. API overview

Base path `/api/v1`. Full interactive docs at `/swagger-ui.html`.

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/forgot-password`, `/auth/reset-password`, `GET /me` |
| Catalog | `GET /products`, `/products/{id}`, `/categories`, `/banners`, `/config`, `GET/POST /products/{id}/reviews` |
| Cart | `GET /cart`, `POST /cart/items`, `PUT/DELETE /cart/items/{id}`, `POST /coupons/validate` |
| Orders | `POST /orders`, `GET /orders`, `/orders/{id}`, `/orders/{id}/tracking`, `/orders/{id}/returns`, `/orders/{id}/invoice`, `POST /orders/{id}/cancel` |
| Returns | `POST /api/returns`, `GET /api/returns/{id}`, `GET /api/orders/{orderId}/returns` (owner-only; request body includes order ID, order item ID, type, reason and quantity) |
| Payments | `POST /payments/create-order`, `/payments/verify`, `/payments/webhook` |
| Tracking | `GET /api/v1/orders/{id}/tracking` (also `/api/orders/{id}/tracking`, owner-only); courier callbacks at `POST /webhooks/courier/{courierName}` with an HMAC-SHA256 `X-Signature` |
| Account | `GET/POST/PUT/DELETE /me/addresses`, `GET/POST/DELETE /wishlist`, `POST /uploads` |
| Business | `POST/GET /quotes`, `PUT /quotes/{id}/decision` |
| Admin | `/admin/dashboard`, `/admin/orders`, `/admin/products`, `/admin/variants`, `/admin/returns` (list, approve/reject, quality check, COD refund confirmation), `/admin/inventory/low-stock`, `/admin/customers`, `/admin/coupons`, `/admin/banners`, `/admin/store-settings`, `/admin/quotes`, `/admin/reports/{type}` (+ `/export`), `/admin/admins` |

## 7. Order lifecycle

`PENDING_PAYMENT → CONFIRMED → PROCESSING → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED`
Return flow: `REQUESTED → APPROVED → PICKUP_SCHEDULED → PICKED_UP → RECEIVED → QC_PASSED → REFUND_INITIATED → REFUNDED` or `REPLACEMENT_CREATED → CLOSED`. Admin can reject requests or record failed quality checks. A refund is recorded from the original item price after allocating the order discount; payment refunds are confirmed by the signed payment webhook. Admins configure return eligibility per product in the product editor. Every order change is logged and shown to the customer.
