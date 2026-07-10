# Medical Billing System

Enterprise-level Medical Shop Billing System built with **Java 21**, **Spring Boot 3**, **MySQL**, **JWT Security**, and **Thymeleaf/Bootstrap 5** frontend.

## Features

### Core
- JWT Authentication with role-based access (Admin, Pharmacist, Cashier)
- Medicine, Category, Supplier, Customer master modules
- Purchase & Sales billing with automatic inventory updates
- Inventory management (low stock when quantity **< 10**, near expiry within **30 days**, expired medicines)
- Reports with Excel/CSV/PDF export
- Prescription upload, returns processing
- User management, shop settings

### Advanced (New)
- **Barcode/QR code scanning** — Camera-based scanner on billing screen (html5-qrcode)
- **Voice-enabled billing** — Web Speech API medicine search by voice
- **SMS notifications** — Bill alerts, order updates, detailed low stock alerts (configurable provider)
- **WhatsApp invoice sharing** — Share invoice via WhatsApp link or API
- **Multi-branch support** — Branch management, branch-scoped users and sales
- **AI reorder suggestions** — Sales velocity analysis with priority-based reorder recommendations
- **Customer loyalty points** — Earn on purchase, redeem points, transaction history
- **Online order management** — Full CRUD on `/online-orders` (create, view, edit, delete); medicine name column; SMS updates on status change
- **Accounting integration** — Auto journal entries for sales/purchases, export for accounting
- **Audit logs & activity tracking** — Full activity log UI with user, action, IP tracking
- **Automated backups** — Scheduled daily backups + manual trigger (mysqldump when available)

### UI & DevOps
- **MediBill branding** — Logo + name in the sidebar (links to dashboard on every page); same branding on login/logout pages
- **Low stock alerts (< 10 units)** — Navbar bell (unread highlight + read history list), inline message, dropdown panel; dashboard table; SMS/email on threshold crossing + scheduled digest (9 AM & 5 PM)
- **Near expiry alerts (30 days)** — Bell notifications, dashboard stat card + table, dedicated list page; medicines expiring within 30 days highlighted in inventory
- **Expired medicines** — Clickable dashboard card, summary table, and `/expired-medicines` list page
- **Low stock medicines page** — Dedicated `/low-stock-medicines` route; dashboard **Low Stock** stat card links to filtered inventory list
- **Inventory stat cards** — Responsive CSS grid on inventory pages; labels and values fit inside cards with ellipsis tooltips for long amounts
- **Uniform site typography** — Single `--font-size-*` scale in `main.css` for body text, headings, tables, forms, sidebar, modals, and stat cards across all pages
- **Dashboard master links** — Available Medicines, Customers, and Suppliers stat cards link to `/medicines`, `/customers`, and `/suppliers` CRUD pages; sidebar Suppliers link uses a plain `href` fallback for reliable navigation
- Dark mode, dashboard charts, notification alerts
- Sidebar logout with dedicated logout-success page
- CSRF-protected login and logout forms
- Environment-based secrets (JWT, DB, API keys) — never in URLs or source code
- URL query-string blocking for credentials and security keys (POST body and headers still work)
- Docker & Docker Compose deployment (port **8085** on host → 8080 in container)
- Unit and integration tests (10 test classes)

## Prerequisites

### Local development
- Java 21+
- Maven 3.9+
- MySQL 8.0+

### Docker deployment
- Docker 24+
- Docker Compose v2+

---

## Quick Start with Docker (Recommended)

Run the full stack (MySQL + application) on **http://localhost:8085**.

### 1. Configure secrets (required before first run)

Docker Compose reads a `.env` file from the project root. Create it from the template:

```bash
cp .env.example .env
```

Edit `.env` and set **strong, unique** values for every required variable:

| Variable | Purpose |
|----------|---------|
| `APP_JWT_SECRET` | JWT signing key (**minimum 32 characters**) |
| `MYSQL_ROOT_PASSWORD` | MySQL root password |
| `APP_ADMIN_PASSWORD` | Initial `admin` user password |
| `APP_PHARMACIST_PASSWORD` | Initial `pharmacist` user password |
| `APP_CASHIER_PASSWORD` | Initial `cashier` user password |

> Never commit `.env`, put secrets in URLs, or hardcode them in `docker-compose.yml`. The app blocks `password`, `token`, `jwt`, `api_key`, `secret`, and similar query parameters.

### 2. Build and start containers

```bash
docker compose up --build -d
```

Wait until both services are healthy (about 30–60 seconds on first start):

```bash
docker compose ps
```

Both `medical-billing-mysql` and `medical-billing-app` should show **healthy**.

### 3. Access the application

| Resource | URL |
|----------|-----|
| Application | http://localhost:8085 |
| Login page | http://localhost:8085/login |
| Dashboard | http://localhost:8085/dashboard |
| Near expiry list | http://localhost:8085/near-expiry-medicines |
| Low stock medicines list | http://localhost:8085/low-stock-medicines |
| Expired medicines list | http://localhost:8085/expired-medicines |
| Customers (CRUD) | http://localhost:8085/customers |
| Suppliers (CRUD) | http://localhost:8085/suppliers |
| Medicines (CRUD) | http://localhost:8085/medicines |
| Online Orders (CRUD) | http://localhost:8085/online-orders |

### 4. Log in

Demo users are created on startup. Passwords come from `.env` (defaults shown in `.env.example`):

| Username | Role | Default password (if unset in `.env`) |
|----------|------|----------------------------------------|
| `admin` | Admin | `admin123` |
| `pharmacist` | Pharmacist | `pharma123` |
| `cashier` | Cashier | `cashier123` |

On **first startup**, set `APP_ADMIN_PASSWORD`, `APP_PHARMACIST_PASSWORD`, and `APP_CASHIER_PASSWORD` in `.env` to your chosen values. On every restart, `DataInitializer` syncs these users to match `.env`.

Credentials are accepted only via the **login form (POST with CSRF token)** or **API request body** — never in the browser address bar or query string.

After login, open the **Dashboard** at http://localhost:8085/dashboard. The **MediBill** logo in the left sidebar links back to the dashboard from any page.

**Inventory notifications (bell icon):** The navbar **bell** shows **unread** alerts with a red badge and inline message for:
- **Low stock** — fewer than 10 units
- **Near expiry** — expiring within 30 days

Open the bell dropdown to view alerts in a **listed format**. After you close the panel, alerts are marked **read** (no bell highlight; items appear muted with a “Read” label). New or changed alerts become unread again.

**Dashboard pages:** Click **Available Medicines**, **Customers**, or **Suppliers** stat cards to open master CRUD pages. Click **Low Stock (&lt;10)**, **Near Expiry (30 days)**, or **Expired Medicines** for filtered inventory lists. Use **Online Orders** in the sidebar for full order CRUD (New Order modal, medicine name column, view/edit/delete). Configure shop **phone** and **email** under **Settings** for SMS/email low stock alerts.

### Docker commands

```bash
# View logs (follow)
docker compose logs -f app

# View last 100 log lines
docker compose logs --tail 100 app

# Stop containers
docker compose down

# Stop and remove volumes (fresh database)
docker compose down -v

# Rebuild and restart after code or template changes
docker compose build app
docker compose up -d app

# One-liner rebuild + restart
docker compose up --build -d app

# Check container health
docker compose ps
docker inspect --format='{{.State.Health.Status}}' medical-billing-app
```

> **Important:** Docker runs a **built JAR** inside the image. After changing Java code, Thymeleaf templates, or static assets (`src/main/resources`), you must **rebuild the app image** (`docker compose build app` or `docker compose up --build -d app`). A simple container restart does **not** pick up source changes.

### Docker architecture

| Service | Container name | Host port | Description |
|---------|----------------|-----------|-------------|
| `app` | medical-billing-app | **8085** → 8080 | Spring Boot app (`medical-billing-system:1.0.5`, profile `docker`) |
| `mysql` | medical-billing-mysql | (internal only) | MySQL 8.0 database |

**Persistent volumes**

| Volume | Mount path | Purpose |
|--------|------------|---------|
| `mysql_data` | `/var/lib/mysql` | Database files |
| `app_uploads` | `/app/uploads` | Prescription and file uploads |
| `app_backups` | `/app/backups` | Database backup files |

Environment variables (set in `.env` — never commit real values or pass them in URLs):

| Variable | Required | Description |
|----------|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | Docker only | Activates `application-docker.properties` |
| `APP_JWT_SECRET` | Yes | JWT signing key (min. 32 characters) |
| `MYSQL_ROOT_PASSWORD` | Yes (Docker) | MySQL root password |
| `SPRING_DATASOURCE_USERNAME` | No | DB username (default: `root`) |
| `APP_ADMIN_PASSWORD` | Yes (first seed) | Initial admin password |
| `APP_PHARMACIST_PASSWORD` | Yes (first seed) | Initial pharmacist password |
| `APP_CASHIER_PASSWORD` | Yes (first seed) | Initial cashier password |
| `APP_SMS_API_KEY` | No | SMS provider API key |
| `APP_WHATSAPP_API_TOKEN` | No | WhatsApp API token |
| `APP_LOW_STOCK_NOTIFY_ENABLED` | No | Enable low stock SMS/email alerts (default: `true`) |
| `APP_LOW_STOCK_THRESHOLD` | No | Alert when stock is below this quantity (default: `10`) |
| `APP_NEAR_EXPIRY_DAYS` | No | Flag medicines expiring within this many days (default: `30`) |

> Security keys (`APP_JWT_SECRET`, API keys, DB passwords) are loaded from environment variables only. The application **rejects** any attempt to pass them as URL query parameters.

### Troubleshooting (Docker)

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| App container exits on startup | Missing or short `APP_JWT_SECRET` | Set `APP_JWT_SECRET` in `.env` (32+ characters), then `docker compose up -d app` |
| `MYSQL_ROOT_PASSWORD` variable is not set | No `.env` file | Run `cp .env.example .env` and fill in all required values |
| `Access denied for user 'root'` after changing `.env` | MySQL volume still has the old password | Use the original password or reset: `docker compose down -v` then `docker compose up --build -d` |
| Whitelabel Error Page (500) after code changes | Stale Docker image | `docker compose up --build -d app`, then hard-refresh the browser (Ctrl+Shift+R) |
| Inventory stat values overflow or fonts look inconsistent | Stale Docker image or browser cache | `docker compose up --build -d app`, then hard-refresh (Ctrl+F5); UI uses uniform `--font-size-*` tokens in `main.css` |
| Low Stock dashboard card opens wrong page | Stale Docker image | `docker compose up --build -d app`; card links to `/low-stock-medicines` |
| New Order button does nothing (Online Orders) | Stale Docker image missing order modal | `docker compose up --build -d app`, then hard-refresh (Ctrl+F5); page includes `#orderModal` and `online-orders.js` |
| Logo or UI changes not visible | Browser or image cache | `docker compose up --build -d app`, then hard-refresh (Ctrl+F5) |
| Bell icon shows no alerts | No matching stock/expiry or not logged in | Set medicine stock below 10 or expiry within 30 days; bell loads `/api/notifications/inventory-alerts` |
| Bell stays highlighted after viewing | Panel not closed | Close the dropdown (click outside or bell again) to mark alerts as read |
| Near expiry / expired pages empty | No matching medicines | Set expiry dates in **Medicines**; use `/near-expiry-medicines` or `/expired-medicines` |
| SMS low stock alerts not received | SMS provider disabled | Set `app.sms.enabled=true` and `APP_SMS_API_KEY`; without SMS, alerts are logged/simulated in app logs |
| Login returns `400 Bad Request` | Missing CSRF token or blocked query params | Use the login form at `/login`; do not append `?username=` or `?password=` to the URL |
| `Connection refused` on port 8085 | App still starting or crashed | `docker compose logs -f app` and wait for `Started MedicalBillingApplication` |
| Login works locally but not in Docker | Wrong port or old container | Use **8085** for Docker, **8080** for `mvn spring-boot:run` |
| Database errors on first boot | MySQL not ready yet | App waits for MySQL healthcheck; retry after `docker compose ps` shows mysql **healthy** |
| Fresh start with empty DB | Old volume data | `docker compose down -v` then `docker compose up --build -d` |
| `400` on a URL with `?token=` or `?api_key=` | Security filter blocked query-string secret | Send tokens in `Authorization: Bearer` header; send credentials in POST body |

To inspect assets inside the running container:

```bash
# Verify MediBill logo is packaged in the image
docker exec medical-billing-app unzip -l /app/app.jar | grep medibill-logo

# Verify notification services are in the image
docker exec medical-billing-app unzip -l /app/app.jar | grep LowStockNotification
docker exec medical-billing-app unzip -l /app/app.jar | grep NearExpiryNotification

# Inspect inventory template in the image (low-stock route and stat grid)
docker exec medical-billing-app unzip -p /app/app.jar BOOT-INF/classes/templates/inventory.html | head -80
```

### Notification configuration (optional)

Add to `.env` or Docker environment (never in URLs or committed files):

```properties
# Low stock (default threshold: 10 units)
APP_LOW_STOCK_NOTIFY_ENABLED=true
APP_LOW_STOCK_THRESHOLD=10

# Near expiry (default: within 30 days)
APP_NEAR_EXPIRY_DAYS=30

# SMS (low stock digest at 9 AM & 5 PM, instant alert when stock crosses threshold)
app.sms.enabled=true
app.sms.api-url=https://your-sms-provider.com/api
# APP_SMS_API_KEY is read from environment

# WhatsApp
app.whatsapp.enabled=true
app.whatsapp.api-url=https://your-whatsapp-api.com
# APP_WHATSAPP_API_TOKEN is read from environment
```

Set **shop phone** and **email** in **Settings** so low stock alerts reach the pharmacy. Admin and pharmacist user phone numbers (if set) also receive SMS alerts.

---

## Local Development (without Docker)

### 1. Create MySQL database

```sql
CREATE DATABASE medical_billing_db;
```

Or run `database/schema.sql`.

### 2. Configure database and secrets

Copy `.env.example` to `.env` and set values:

```bash
cp .env.example .env
```

Required for local run:

```properties
APP_JWT_SECRET=your-long-random-secret-at-least-32-characters
SPRING_DATASOURCE_PASSWORD=your_mysql_password
```

Edit `src/main/resources/application.properties` datasource URL if needed, or override with environment variables.

### 3. Build and run

```bash
mvn clean install
mvn spring-boot:run
```

### 4. Access application

- URL: http://localhost:8080
- Login: http://localhost:8080/login

---

## Project Structure

```
MedicalBillingSystem/
├── .env.example               # Template for required secrets (copy to .env)
├── Dockerfile                 # Multi-stage build (JDK 21 → JRE 21, healthcheck)
├── docker-compose.yml         # App + MySQL stack (host port 8085, reads .env)
├── .dockerignore              # Excludes target/, docs, and dev files from build context
├── pom.xml
├── src/main/java/com/medicalbilling/
│   ├── config/                # Security, schedulers (backup, low stock digest), data initializer
│   ├── controller/            # REST API & Thymeleaf web controllers
│   ├── dto/                   # Data transfer objects
│   ├── entity/                # JPA entities
│   ├── exception/             # Global exception handling
│   ├── repository/            # Spring Data JPA repositories
│   ├── security/              # JWT filter, query-string secret blocking
│   ├── service/               # Business logic (incl. LowStockNotificationService, NearExpiryNotificationService)
│   └── util/                  # JWT, code generators
├── src/main/resources/
│   ├── application.properties
│   ├── application-docker.properties
│   ├── static/css/            # main.css (uniform --font-size-* typography)
│   ├── static/images/         # MediBill logo (medibill-logo.svg)
│   ├── static/js/             # JavaScript modules
│   └── templates/             # Thymeleaf HTML pages (fragments/layout.html sidebar brand)
├── database/                  # SQL schema and seed data
└── documentation/             # API docs and user manual
```

## API Authentication

**Docker (port 8085):**

```bash
curl -X POST http://localhost:8085/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"<username>\",\"password\":\"<password>\"}"
```

**Local (port 8080):**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"<username>\",\"password\":\"<password>\"}"
```

Use the returned JWT token in the **Authorization header** (not the URL):

```bash
curl http://localhost:8085/api/dashboard \
  -H "Authorization: Bearer <token>"
```

> Sending `username`, `password`, `token`, `jwt`, `api_key`, `secret`, or similar values as URL query parameters is **blocked** by the application. Use POST body or `Authorization` header only.

### Inventory notification API (authenticated)

```bash
# Combined low stock + near expiry alerts (used by navbar bell)
curl http://localhost:8085/api/notifications/inventory-alerts \
  -H "Authorization: Bearer <token>"

# List medicines below threshold (default: 10 units)
curl http://localhost:8085/api/notifications/low-stock \
  -H "Authorization: Bearer <token>"

# List medicines expiring within 30 days (configurable)
curl http://localhost:8085/api/notifications/near-expiry \
  -H "Authorization: Bearer <token>"

# Manually trigger daily low stock digest (SMS/email)
curl -X POST http://localhost:8085/api/notifications/low-stock/digest \
  -H "Authorization: Bearer <token>"
```

### Online orders API (authenticated)

```bash
# List all online orders
curl http://localhost:8085/api/online-orders \
  -H "Authorization: Bearer <token>"

# Get one order (with items and medicine names)
curl http://localhost:8085/api/online-orders/1 \
  -H "Authorization: Bearer <token>"

# Create order
curl -X POST http://localhost:8085/api/online-orders \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d "{\"customerId\":1,\"contactPhone\":\"9876543210\",\"deliveryAddress\":\"123 Main St\",\"items\":[{\"medicineId\":1,\"quantity\":2}]}"

# Update order
curl -X PUT http://localhost:8085/api/online-orders/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d "{\"customerId\":1,\"status\":\"CONFIRMED\",\"items\":[{\"medicineId\":1,\"quantity\":3}]}"

# Update status only
curl -X PUT http://localhost:8085/api/online-orders/1/status \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d "{\"status\":\"DELIVERED\"}"

# Delete order
curl -X DELETE http://localhost:8085/api/online-orders/1 \
  -H "Authorization: Bearer <token>"
```

## Deployment

### Docker production notes

- Set all secrets in `.env` (`APP_JWT_SECRET`, `MYSQL_ROOT_PASSWORD`, user passwords) — never hardcode them in `docker-compose.yml` or URLs
- Do not expose the MySQL service to the host in production (it is internal to the Docker network by default)
- Use named volumes for persistent data (`mysql_data`, `app_uploads`, `app_backups`)
- Put a reverse proxy (Nginx/Traefik) in front for HTTPS
- Rebuild the image as part of your deploy pipeline: `docker compose build app && docker compose up -d app`
- Image tag: `medical-billing-system:1.0.5` (see `docker-compose.yml`)
- Monitor health: `docker compose ps` — both services should report **healthy**

### JAR deployment

Set environment variables before starting (same keys as `.env.example`):

```bash
mvn clean package -DskipTests
export APP_JWT_SECRET="your-long-random-secret-at-least-32-characters"
export SPRING_DATASOURCE_PASSWORD="your_mysql_password"
java -jar target/medical-billing-system-1.0.0.jar
```

### Production checklist

- Set `APP_JWT_SECRET` (32+ characters) via environment — not in source code or URLs
- Configure MySQL credentials via `.env` / environment variables
- Set up SMTP for email notifications
- Enable HTTPS
- Configure `mysqldump` for production backups

## License

Proprietary - Medical Billing System
