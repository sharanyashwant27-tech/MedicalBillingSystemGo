# Medical Billing System (Go)

Enterprise Medical Shop Billing System built with **Go**, **Gin**, **GORM**, **SQLite** (default), **JWT Security**, and **Bootstrap 5** frontend.

## Quick Start

### Prerequisites
- Go 1.22+ (tested with Go 1.26)
- No MySQL required for local dev (uses embedded SQLite)

### Run locally on port 8086

```bash
go run ./cmd/server/
```

Open **http://localhost:8086/login**

| Username   | Password   | Role        |
|------------|------------|-------------|
| `admin`    | `admin123` | Admin       |
| `pharmacist` | `pharma123` | Pharmacist |
| `cashier`  | `cashier123` | Cashier   |

### Docker

```bash
docker compose up --build -d
```

App runs at **http://localhost:8086**

### API Authentication

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
├── go.mod
├── Dockerfile
└── docker-compose.yml
```

## Features

- JWT + session authentication with role-based access
- Medicine, category, supplier, customer CRUD
- Purchase & sales billing with inventory updates
- Dashboard with low stock / near expiry alerts
- Online orders, branches, loyalty, audit logs
- Reports, returns, prescriptions, settings
- Reuses MediBill UI (Bootstrap 5, dark mode, notifications)

## Legacy Java Code

The original Spring Boot application remains under `src/` for reference. The active application is the Go server in `cmd/server/`.

## License

Proprietary - Medical Billing System
