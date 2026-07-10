package config

import (
	"os"
	"strconv"
)

type Config struct {
	Port              string
	JWTSecret         string
	DBDriver          string
	DBDSN             string
	AdminPassword     string
	PharmacistPassword string
	CashierPassword   string
	LowStockThreshold int
	NearExpiryDays    int
	UploadDir         string
	BackupDir         string
}

func Load() Config {
	threshold := 10
	if v := os.Getenv("APP_LOW_STOCK_THRESHOLD"); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			threshold = n
		}
	}
	nearExpiry := 30
	if v := os.Getenv("APP_NEAR_EXPIRY_DAYS"); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			nearExpiry = n
		}
	}

	jwtSecret := os.Getenv("APP_JWT_SECRET")
	if jwtSecret == "" {
		jwtSecret = "dev-jwt-secret-change-in-production-32chars"
	}

	dbDriver := os.Getenv("DB_DRIVER")
	if dbDriver == "" {
		dbDriver = "sqlite"
	}
	dbDSN := os.Getenv("DB_DSN")
	if dbDSN == "" {
		dbDSN = "data/medical_billing.db"
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = "8086"
	}

	adminPwd := envOr("APP_ADMIN_PASSWORD", "admin123")
	pharmaPwd := envOr("APP_PHARMACIST_PASSWORD", "pharma123")
	cashierPwd := envOr("APP_CASHIER_PASSWORD", "cashier123")

	return Config{
		Port:               port,
		JWTSecret:          jwtSecret,
		DBDriver:           dbDriver,
		DBDSN:              dbDSN,
		AdminPassword:      adminPwd,
		PharmacistPassword: pharmaPwd,
		CashierPassword:    cashierPwd,
		LowStockThreshold:  threshold,
		NearExpiryDays:     nearExpiry,
		UploadDir:          envOr("APP_UPLOAD_DIR", "uploads"),
		BackupDir:          envOr("APP_BACKUP_DIR", "backups"),
	}
}

func envOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
