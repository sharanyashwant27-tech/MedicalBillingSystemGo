package database

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/medicalbilling/medical-billing-system/internal/config"
	"github.com/medicalbilling/medical-billing-system/internal/models"
	"github.com/glebarez/sqlite"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

func Connect(cfg config.Config) (*gorm.DB, error) {
	var dialector gorm.Dialector
	switch cfg.DBDriver {
	case "mysql":
		dialector = mysql.Open(cfg.DBDSN)
	default:
		if err := os.MkdirAll(filepath.Dir(cfg.DBDSN), 0755); err != nil {
			return nil, fmt.Errorf("create data dir: %w", err)
		}
		dialector = sqlite.Open(cfg.DBDSN)
	}

	db, err := gorm.Open(dialector, &gorm.Config{
		Logger: logger.Default.LogMode(logger.Warn),
	})
	if err != nil {
		return nil, fmt.Errorf("open database: %w", err)
	}

	if err := models.AutoMigrate(db); err != nil {
		return nil, fmt.Errorf("migrate: %w", err)
	}

	return db, nil
}
