# Medical Billing System

Enterprise Medical Shop Billing System built with **Go**, **Gin**, **GORM**, **SQLite**, **JWT Security**, and **Bootstrap 5**.

## Features

- JWT + session authentication with role-based access (Admin, Pharmacist, Cashier)
- Medicine, category, supplier, and customer management
- **Purchase management** — create, view, edit, and delete purchases with automatic stock updates
- Sales billing with automatic inventory updates
- Dashboard with low stock, near expiry, and expired medicine alerts
- Online orders, multi-branch support, loyalty points, audit logs
- Reports with PDF, Excel, and CSV export
- Prescription upload, returns processing, shop settings
- MediBill UI with dark mode, charts, and notification bell

## Quick Start

### Prerequisites
- **Local:** Go 1.22+ (tested with Go 1.26)
- **Docker:** Docker 24+ and Docker Compose v2+

### Run locally

```bash
go run ./cmd/server/
```

Open **http://localhost:8086/login**

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | Admin |
| `pharmacist` | `pharma123` | Pharmacist |
| `cashier` | `cashier123` | Cashier |

### Run with Docker (recommended)

```bash
docker compose up --build -d
```

App available at **http://localhost:8086**

To rebuild after code changes:

```bash
docker compose up --build -d
```

Or use Make:

```bash
make docker-up
```

## API Authentication

```bash
curl -X POST http://localhost:8086/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

```bash
curl http://localhost:8086/api/dashboard \
  -H "Authorization: Bearer <token>"
```

### Purchase API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/purchases` | List all purchases |
| `GET` | `/api/purchases/:id` | Get purchase details |
| `POST` | `/api/purchases` | Create purchase (auto invoice if omitted) |
| `PUT` | `/api/purchases/:id` | Update purchase |
| `DELETE` | `/api/purchases/:id` | Delete purchase (reverses stock) |

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8086` | HTTP server port |
| `APP_JWT_SECRET` | dev default | JWT signing key (32+ chars in production) |
| `DB_DRIVER` | `sqlite` | `sqlite` or `mysql` |
| `DB_DSN` | `data/medical_billing.db` | Database connection string |
| `APP_ADMIN_PASSWORD` | `admin123` | Seed admin password |
| `APP_PHARMACIST_PASSWORD` | `pharma123` | Seed pharmacist password |
| `APP_CASHIER_PASSWORD` | `cashier123` | Seed cashier password |
| `APP_LOW_STOCK_THRESHOLD` | `10` | Low stock alert threshold |
| `APP_NEAR_EXPIRY_DAYS` | `30` | Near expiry window (days) |

Copy `.env.example` to `.env` for Docker Compose:

```bash
cp .env.example .env
```

## Project Structure

```
MedicalBillingSystemGo/
├── cmd/server/main.go          # Entry point
├── internal/
│   ├── auth/                   # JWT and password hashing
│   ├── config/                 # Environment configuration
│   ├── database/               # GORM connection and migrations
│   ├── handlers/               # HTTP handlers (API + web pages)
│   ├── middleware/             # Auth and security middleware
│   ├── models/                 # Entities and DTOs
│   ├── seed/                   # Demo data initializer
│   ├── services/               # Business logic and exports
│   └── templates/              # HTML template renderer
├── web/
│   ├── static/                 # CSS, JavaScript, images
│   └── templates/              # HTML page templates
├── database/                   # SQL schema reference
├── documentation/              # API and user docs
├── Dockerfile
├── docker-compose.yml
└── go.mod
```

## Docker

```bash
# Build image
docker build -t medical-billing-system-go:1.1.0 .

# Start with Compose (builds and runs in background)
docker compose up --build -d

# View logs
docker compose logs -f app

# Stop
docker compose down

# Stop and remove volumes (resets database)
docker compose down -v
```

| Item | Value |
|------|-------|
| Image | `medical-billing-system-go:1.1.0` |
| Container | `medical-billing-go` |
| Port | **8086** |
| Database | SQLite volume at `/app/data` |
| Uploads | Volume at `/app/uploads` |
| Backups | Volume at `/app/backups` |

### Makefile shortcuts

| Command | Description |
|---------|-------------|
| `make run` | Run locally with Go |
| `make build` | Build binary to `bin/medibill` |
| `make docker` | Build Docker image |
| `make docker-up` | Build and start with Compose |
| `make docker-down` | Stop containers |

## License

Proprietary - Medical Billing System
