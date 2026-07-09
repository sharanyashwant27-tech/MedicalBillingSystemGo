# API Documentation

Base URL: `http://localhost:8080`

## Authentication

### POST /api/auth/login
Login and receive JWT token.

**Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbG...",
    "type": "Bearer",
    "username": "admin",
    "roles": ["ROLE_ADMIN"]
  }
}
```

## Dashboard

### GET /api/dashboard
Returns dashboard statistics (requires authentication).

## Medicines

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/medicines | List all medicines |
| GET | /api/medicines/search?q= | Search medicines |
| GET | /api/medicines/{id} | Get medicine by ID |
| POST | /api/medicines | Create medicine |
| PUT | /api/medicines/{id} | Update medicine |
| DELETE | /api/medicines/{id} | Deactivate medicine |

## Categories

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/categories | List categories |
| POST | /api/categories | Create category |
| PUT | /api/categories/{id} | Update category |
| DELETE | /api/categories/{id} | Delete category |

## Suppliers

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/suppliers | List suppliers |
| GET | /api/suppliers/{id} | Get supplier |
| POST | /api/suppliers | Create supplier |
| PUT | /api/suppliers/{id} | Update supplier |
| DELETE | /api/suppliers/{id} | Delete supplier |

## Customers

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/customers | List customers |
| GET | /api/customers/{id}/history | Customer purchase history |
| POST | /api/customers | Create customer |
| PUT | /api/customers/{id} | Update customer |
| DELETE | /api/customers/{id} | Delete customer |

## Sales

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/sales | List all sales |
| GET | /api/sales/recent | Recent bills |
| GET | /api/sales/{id} | Get sale details |
| POST | /api/sales | Create sale/bill |
| GET | /api/sales/{id}/pdf | Download invoice PDF |
| POST | /api/sales/{id}/email | Email invoice |

## Purchases

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/purchases | List purchases |
| POST | /api/purchases | Create purchase |

## Inventory

### GET /api/inventory?filter=
Filters: ALL, LOW_STOCK, OUT_OF_STOCK, NEAR_EXPIRY, EXPIRED

## Reports

### GET /api/reports?type=&startDate=&endDate=
Report types: SALES, PURCHASE, PROFIT, GST, MEDICINE, CUSTOMER, SUPPLIER, STOCK, EXPIRY

### GET /api/reports/export?type=&format=&startDate=&endDate=
Export formats: excel, csv, pdf

## Returns

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/returns | List returns |
| POST | /api/returns | Process return |

## Prescriptions

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/prescriptions/customer/{id} | List customer prescriptions |
| POST | /api/prescriptions | Upload prescription (multipart) |

## Settings

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/settings | Get shop settings |
| PUT | /api/settings | Update settings |
| POST | /api/settings/logo | Upload logo |
| POST | /api/settings/backup | Backup database |

## Users (Admin only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/users | List users |
| POST | /api/users | Create user |
| PUT | /api/users/{id} | Update user |
| DELETE | /api/users/{id} | Delete user |
| POST | /api/users/{id}/reset-password | Reset password |
| POST | /api/users/{id}/lock | Lock user |
| POST | /api/users/{id}/unlock | Unlock user |

## Branches

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/branches | List all branches |
| GET | /api/branches/active | List active branches |
| POST | /api/branches | Create branch |
| PUT | /api/branches/{id} | Update branch |

## Loyalty Points

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/loyalty/{customerId} | Get balance and history |
| POST | /api/loyalty/{customerId}/redeem | Redeem points |

## Online Orders

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/online-orders | List all orders |
| GET | /api/online-orders/status/{status} | Filter by status |
| POST | /api/online-orders | Create order |
| PUT | /api/online-orders/{id}/status | Update order status |

## AI Reorder Suggestions

### GET /api/reorder-suggestions
Returns AI-powered reorder recommendations based on sales velocity and stock levels.

## Accounting

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/accounting/entries | Get entries by date range |
| POST | /api/accounting/export | Export and mark entries synced |

## Notifications

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/notifications/sms | Send SMS |
| POST | /api/sales/{id}/whatsapp | Share invoice via WhatsApp |
| GET | /api/notifications | Notification log history |

## Barcode Lookup

### GET /api/medicines/barcode/{barcode}
Lookup medicine by barcode or QR code value.

## Audit Logs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/audit-logs | Recent audit logs |
| GET | /api/audit-logs/user/{username} | Logs by user |
| GET | /api/audit-logs/range | Logs by date range |

## Backups

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/backups | List backup files |
| POST | /api/backups | Create manual backup |

## Error Response Format

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/medicines"
}
```
