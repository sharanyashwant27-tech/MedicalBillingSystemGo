# Medical Billing System

Enterprise-level Medical Shop Billing System built with **Java 21**, **Spring Boot 3**, **MySQL**, **JWT Security**, and **Thymeleaf/Bootstrap 5** frontend.

## Features

### Core
- JWT Authentication with role-based access (Admin, Pharmacist, Cashier)
- Medicine, Category, Supplier, Customer master modules
- Purchase & Sales billing with automatic inventory updates
- Inventory management (low stock, expiry alerts)
- Reports with Excel/CSV/PDF export
- Prescription upload, returns processing
- User management, shop settings

### Advanced (New)
- **Barcode/QR code scanning** — Camera-based scanner on billing screen (html5-qrcode)
- **Voice-enabled billing** — Web Speech API medicine search by voice
- **SMS notifications** — Bill alerts, order updates, low stock alerts (configurable provider)
- **WhatsApp invoice sharing** — Share invoice via WhatsApp link or API
- **Multi-branch support** — Branch management, branch-scoped users and sales
- **AI reorder suggestions** — Sales velocity analysis with priority-based reorder recommendations
- **Customer loyalty points** — Earn on purchase, redeem points, transaction history
- **Online order management** — Create, track, and update online orders with SMS updates
- **Accounting integration** — Auto journal entries for sales/purchases, export for accounting
- **Audit logs & activity tracking** — Full activity log UI with user, action, IP tracking
- **Automated backups** — Scheduled daily backups + manual trigger (mysqldump when available)

### UI & DevOps
- Dark mode, dashboard charts, notification alerts
- Sidebar logout with dedicated logout-success page
- Docker & Docker Compose deployment (port **8085** on host → 8080 in container)
- Unit and integration tests (8+ test classes)

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

### 1. Build and start containers

```bash
docker compose up --build -d
```

Wait until the app is healthy (about 30–60 seconds on first start):

```bash
docker compose ps
```

Both `medical-billing-mysql` and `medical-billing-app` should show **healthy**.

### 2. Access the application

| Resource | URL |
|----------|-----|
| Application | http://localhost:8085 |
| Login page | http://localhost:8085/login |
| Dashboard | http://localhost:8085/dashboard |

### 3. Default users

| Username    | Password    | Role       |
|-------------|-------------|------------|
| admin       | admin123    | Admin      |
| pharmacist  | pharma123   | Pharmacist |
| cashier     | cashier123  | Cashier    |

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
| `app` | medical-billing-app | **8085** → 8080 | Spring Boot application (`SPRING_PROFILES_ACTIVE=docker`) |
| `mysql` | medical-billing-mysql | (internal only) | MySQL 8.0 database |

**Persistent volumes**

| Volume | Mount path | Purpose |
|--------|------------|---------|
| `mysql_data` | `/var/lib/mysql` | Database files |
| `app_uploads` | `/app/uploads` | Prescription and file uploads |
| `app_backups` | `/app/backups` | Database backup files |

Environment variables (set in `docker-compose.yml`):

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `docker` | Activates `application-docker.properties` |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://mysql:3306/medical_billing_db...` | Database connection |
| `SPRING_DATASOURCE_USERNAME` | `root` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `root` | DB password |
| `APP_JWT_SECRET` | (see compose file) | JWT signing key |

### Troubleshooting (Docker)

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Whitelabel Error Page (500) after code changes | Stale Docker image | `docker compose up --build -d app`, then hard-refresh the browser (Ctrl+Shift+R) |
| `Connection refused` on port 8085 | App still starting or crashed | `docker compose logs -f app` and wait for `Started MedicalBillingApplication` |
| Login works locally but not in Docker | Wrong port or old container | Use **8085** for Docker, **8080** for `mvn spring-boot:run` |
| Database errors on first boot | MySQL not ready yet | App waits for MySQL healthcheck; retry after `docker compose ps` shows mysql **healthy** |
| Fresh start with empty DB | Old volume data | `docker compose down -v` then `docker compose up --build -d` |

To inspect the template inside the running container:

```bash
docker exec medical-billing-app unzip -p /app/app.jar BOOT-INF/classes/templates/categories.html
```

### Notification configuration (optional)

Add to `application.properties` or Docker environment:

```properties
app.sms.enabled=true
app.sms.api-url=https://your-sms-provider.com/api
app.sms.api-key=your-api-key
app.whatsapp.enabled=true
app.whatsapp.api-url=https://your-whatsapp-api.com
app.whatsapp.api-token=your-token
```

---

## Local Development (without Docker)

### 1. Create MySQL database

```sql
CREATE DATABASE medical_billing_db;
```

Or run `database/schema.sql`.

### 2. Configure database

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

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
├── Dockerfile                 # Multi-stage build (JDK 21 → JRE 21, healthcheck)
├── docker-compose.yml         # App + MySQL stack (host port 8085)
├── .dockerignore              # Excludes target/, docs, and dev files from build context
├── pom.xml
├── src/main/java/com/medicalbilling/
│   ├── config/                # Security, Web MVC, Data initializer
│   ├── controller/            # REST API & Thymeleaf web controllers
│   ├── dto/                   # Data transfer objects
│   ├── entity/                # JPA entities
│   ├── exception/             # Global exception handling
│   ├── repository/            # Spring Data JPA repositories
│   ├── security/              # JWT filter, UserDetailsService
│   ├── service/               # Business logic layer
│   └── util/                  # JWT, code generators
├── src/main/resources/
│   ├── application.properties
│   ├── application-docker.properties
│   ├── static/css/            # Stylesheets
│   ├── static/js/             # JavaScript modules
│   └── templates/             # Thymeleaf HTML pages
├── database/                  # SQL schema and seed data
└── documentation/             # API docs and user manual
```

## API Authentication

**Docker (port 8085):**

```bash
curl -X POST http://localhost:8085/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

**Local (port 8080):**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

Use the returned JWT token:

```bash
curl http://localhost:8085/api/dashboard \
  -H "Authorization: Bearer <token>"
```

## Deployment

### Docker production notes

- Change `APP_JWT_SECRET` and MySQL passwords in `docker-compose.yml` before deploying
- Do not expose the MySQL service to the host in production (it is internal to the Docker network by default)
- Use named volumes for persistent data (`mysql_data`, `app_uploads`, `app_backups`)
- Put a reverse proxy (Nginx/Traefik) in front for HTTPS
- Rebuild the image as part of your deploy pipeline: `docker compose build app && docker compose up -d app`
- Monitor health: `docker compose ps` — both services should report **healthy**

### JAR deployment

```bash
mvn clean package -DskipTests
java -jar target/medical-billing-system-1.0.0.jar
```

### Production checklist

- Change JWT secret in `application.properties` or Docker env vars
- Configure MySQL credentials
- Set up SMTP for email notifications
- Enable HTTPS
- Configure `mysqldump` for production backups

## License

Proprietary - Medical Billing System
