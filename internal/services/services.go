package services

import (
	"github.com/medicalbilling/medical-billing-system/internal/models"
	"gorm.io/gorm"
)

type Services struct {
	DB     *gorm.DB
	Config AppConfig
}

type AppConfig interface {
	LowStockThreshold() int
	NearExpiryDays() int
	UploadDir() string
	BackupDir() string
}

type appConfigAdapter struct {
	threshold    int
	nearExpiry   int
	uploadDir    string
	backupDir    string
}

func (a appConfigAdapter) LowStockThreshold() int { return a.threshold }
func (a appConfigAdapter) NearExpiryDays() int    { return a.nearExpiry }
func (a appConfigAdapter) UploadDir() string      { return a.uploadDir }
func (a appConfigAdapter) BackupDir() string      { return a.backupDir }

func NewServices(db *gorm.DB, threshold, nearExpiry int, uploadDir, backupDir string) *Services {
	return &Services{
		DB: db,
		Config: appConfigAdapter{
			threshold:  threshold,
			nearExpiry: nearExpiry,
			uploadDir:  uploadDir,
			backupDir:  backupDir,
		},
	}
}

func (s *Services) LogAudit(action, entityType string, entityID *uint, username, details, ip string) {
	s.DB.Create(&models.AuditLog{
		Action:     action,
		EntityType: entityType,
		EntityID:   entityID,
		Username:   username,
		Details:    details,
		IPAddress:  ip,
		Timestamp:  now(),
	})
}

func notFound(msg string) error {
	return &APIError{Code: 404, Message: msg}
}

func badRequest(msg string) error {
	return &APIError{Code: 400, Message: msg}
}

type APIError struct {
	Code    int
	Message string
}

func (e *APIError) Error() string { return e.Message }
