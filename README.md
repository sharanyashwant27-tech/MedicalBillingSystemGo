# Medical Billing System (Go)

Enterprise Medical Shop Billing System built with **Go**, **Gin**, **GORM**, **SQLite** (default), **JWT Security**, and **Bootstrap 5** frontend.

## Quick Start

### Prerequisites
- **Local dev:** Go 1.22+ (tested with Go 1.26)
- **Docker:** Docker 24+ and Docker Compose v2+
- No MySQL required for local dev (uses embedded SQLite)

---

## Run with Docker (Recommended)

The Docker image bundles the Go binary, web assets, and SQLite — no separate database container needed.

### 1. Build the image

```bash
docker build -t medical-billing-system-go:1.0.0 .
```

### 2. Start with Docker Compose

```bash
docker compose up -d
```

Or build and start in one step:

```bash
docker compose up --build -d
```

### 3. Access the application

| Resource | URL |
|----------|-----|
| Application | http://localhost:8086 |
| Login page | http://localhost:8086/login |
| Dashboard | http://localhost:8086/dashboard |

### 4. Log in

Demo users are seeded on first startup:

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | Admin |
| `pharmacist` | `pharma123` | Pharmacist |
| `cashier` | `cashier123` | Cashier |

Override passwords via environment variables (see [Configuration](#configuration-environment-variables)).

### Docker commands

```bash
# View logs
docker compose logs -f app

# Check status
docker compose ps

# Stop containers
docker compose down

# Stop and remove volumes (fresh database)
docker compose down -v

# Rebuild after code changes
docker compose up --build -d app
```

### Docker architecture

| Item | Value |
|------|-------|
| Image | `medical-billing-system-go:1.0.0` |
| Container | `medical-billing-go` |
| Host port | **8086** |
| Database | SQLite at `/app/data/medical_billing.db` |

**Persistent volumes**

| Volume | Mount path | Purpose |
|--------|------------|---------|
| `app_data` | `/app/data` | SQLite database |
| `app_uploads` | `/app/uploads` | Prescription and file uploads |
| `app_backups` | `/app/backups` | Backup files |

### Run without Compose

```bash
docker run -d \
  --name medical-billing-go \
  -p 8086:8086 \
  -e APP_JWT_SECRET="your-long-random-secret-at-least-32-characters" \
  -v medibill_data:/app/data \
  medical-billing-system-go:1.0.0
```

---

## Local Development (without Docker)

```bash
go run ./cmd/server/
```

Open **http://localhost:8086/login**

---

## API Authentication

```bash
curl -X POST http://localhost:8086/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Use the returned JWT:

```bash
curl http://localhost:8086/api/dashboard \
  -H "Authorization: Bearer <token>"
```

---

## Configuration (environment variables)

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8086` | HTTP server port |
| `APP_JWT_SECRET` | dev default | JWT signing key (32+ chars in production) |
| `DB_DRIVER` | `sqlite` | `sqlite` or `mysql` |
| `DB_DSN` | `data/medical_billing.db` | SQLite file path or MySQL DSN |
| `APP_ADMIN_PASSWORD` | `admin123` | Seed admin password |
| `APP_PHARMACIST_PASSWORD` | `pharma123` | Seed pharmacist password |
| `APP_CASHIER_PASSWORD` | `cashier123` | Seed cashier password |
| `APP_LOW_STOCK_THRESHOLD` | `10` | Low stock alert threshold |
| `APP_NEAR_EXPIRY_DAYS` | `30` | Near expiry alert window (days) |

For Docker Compose, set values in a `.env` file (copy from `.env.example`):

```bash
cp .env.example .env
```

---

## Project Structure

```
MedicalBillingSystemGo/
├── cmd/server/main.go          # Application entry point
├── internal/
│   ├── config/                 # Configuration
│   ├── models/                 # GORM entities & DTOs
│   ├── database/               # DB connection & migration
│   ├── auth/                   # JWT & password hashing
│   ├── middleware/             # Auth & security middleware
│   ├── handlers/               # HTTP handlers (API + web)
│   ├── services/               # Business logic
│   ├── seed/                   # Database seed data
│   └── templates/              # Template engine
├── web/
│   ├── static/                 # CSS, JS, images
│   └── templates/              # HTML templates
├── Dockerfile                  # Multi-stage Go build
├── docker-compose.yml          # Single-service stack (port 8086)
├── go.mod
└── .env.example
```

## Features

- JWT + session authentication with role-based access
- Medicine, category, supplier, customer CRUD
- Purchase & sales billing with inventory updates
- Dashboard with low stock / near expiry alerts
- Online orders, branches, loyalty, audit logs
- Reports, returns, prescriptions, settings
- MediBill UI (Bootstrap 5, dark mode, notifications)

## Troubleshooting (Docker)

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `bind: address already in use` on 8086 | Port taken by local `go run` or old container | Stop the other process or run `docker compose down` |
| Container exits immediately | Missing writable `/app/data` | Ensure the `app_data` volume is mounted |
| UI changes not visible | Stale image | `docker compose up --build -d app`, then hard-refresh (Ctrl+F5) |
| Login fails with defaults | Custom `.env` passwords | Check `APP_ADMIN_PASSWORD` etc. in `.env` |

## Legacy Java Code

The original Spring Boot application remains under `src/` for reference. The active application is the Go server in `cmd/server/`.

## License

Proprietary - Medical Billing System
