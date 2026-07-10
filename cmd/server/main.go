package main

import (
	"log"
	"net/http"

	"github.com/gin-contrib/sessions"
	"github.com/gin-contrib/sessions/cookie"
	"github.com/gin-gonic/gin"
	"github.com/medicalbilling/medical-billing-system/internal/auth"
	"github.com/medicalbilling/medical-billing-system/internal/config"
	"github.com/medicalbilling/medical-billing-system/internal/database"
	"github.com/medicalbilling/medical-billing-system/internal/handlers"
	"github.com/medicalbilling/medical-billing-system/internal/middleware"
	"github.com/medicalbilling/medical-billing-system/internal/models"
	"github.com/medicalbilling/medical-billing-system/internal/seed"
	"github.com/medicalbilling/medical-billing-system/internal/services"
	"github.com/medicalbilling/medical-billing-system/internal/templates"
)

func main() {
	cfg := config.Load()

	if err := templates.Init("web/templates"); err != nil {
		log.Fatalf("templates: %v", err)
	}

	db, err := database.Connect(cfg)
	if err != nil {
		log.Fatalf("database: %v", err)
	}

	seed.Run(db, cfg)

	svc := services.NewServices(db, cfg.LowStockThreshold, cfg.NearExpiryDays, cfg.UploadDir, cfg.BackupDir)
	jwtMgr := auth.NewJWTManager(cfg.JWTSecret)
	h := handlers.NewHandlers(svc, jwtMgr)
	authMw := middleware.NewAuthMiddleware(jwtMgr, svc)

	r := gin.Default()
	r.Use(middleware.BlockQuerySecrets())

	store := cookie.NewStore([]byte(cfg.JWTSecret))
	store.Options(sessions.Options{Path: "/", HttpOnly: true, MaxAge: 86400})
	r.Use(sessions.Sessions("medibill_session", store))

	r.Static("/css", "web/static/css")
	r.Static("/js", "web/static/js")
	r.Static("/images", "web/static/images")
	r.Static("/uploads", cfg.UploadDir)

	// Public routes
	r.GET("/", func(c *gin.Context) { c.Redirect(http.StatusFound, "/login") })
	r.GET("/login", h.LoginPage)
	r.POST("/login", h.LoginForm)
	r.GET("/forgot-password", h.ForgotPasswordPage)
	r.GET("/logout-success", h.LogoutSuccessPage)
	r.POST("/api/auth/login", h.LoginAPI)

	// Protected web routes
	web := r.Group("/")
	web.Use(authMw.Authenticate())
	{
		web.GET("/dashboard", h.DashboardPage)
		web.GET("/medicines", h.MedicinesPage)
		web.GET("/categories", h.CategoriesPage)
		web.GET("/suppliers", h.SuppliersPage)
		web.GET("/customers", h.CustomersPage)
		web.GET("/billing", h.BillingPage)
		web.GET("/purchases", h.PurchasesPage)
		web.GET("/inventory", h.InventoryPage)
		web.GET("/expired-medicines", h.ExpiredMedicinesPage)
		web.GET("/near-expiry-medicines", h.NearExpiryMedicinesPage)
		web.GET("/low-stock-medicines", h.LowStockMedicinesPage)
		web.GET("/reports", h.ReportsPage)
		web.GET("/returns", h.ReturnsPage)
		web.GET("/prescriptions", h.PrescriptionsPage)
		web.GET("/settings", h.SettingsPage)
		web.GET("/branches", h.BranchesPage)
		web.GET("/online-orders", h.OnlineOrdersPage)
		web.GET("/reorder-suggestions", h.ReorderSuggestionsPage)
		web.GET("/audit-logs", h.AuditLogsPage)
		web.GET("/users", authMw.RequireRole(models.RoleAdmin), h.UsersPage)
		web.POST("/logout", h.Logout)
	}

	// Protected API routes
	api := r.Group("/api")
	api.Use(authMw.Authenticate())
	{
		api.GET("/categories", h.GetCategories)
		api.POST("/categories", h.CreateCategory)
		api.PUT("/categories/:id", h.UpdateCategory)
		api.DELETE("/categories/:id", h.DeleteCategory)

		api.GET("/suppliers", h.GetSuppliers)
		api.GET("/suppliers/:id", h.GetSupplier)
		api.POST("/suppliers", h.CreateSupplier)
		api.PUT("/suppliers/:id", h.UpdateSupplier)
		api.DELETE("/suppliers/:id", h.DeleteSupplier)

		api.GET("/customers", h.GetCustomers)
		api.GET("/customers/:id/history", h.GetCustomerHistory)
		api.POST("/customers", h.CreateCustomer)
		api.PUT("/customers/:id", h.UpdateCustomer)
		api.DELETE("/customers/:id", h.DeleteCustomer)

		api.GET("/medicines", h.GetMedicines)
		api.GET("/medicines/search", h.SearchMedicines)
		api.GET("/medicines/:id", h.GetMedicine)
		api.GET("/medicines/barcode/:barcode", h.GetMedicineByBarcode)
		api.POST("/medicines", h.CreateMedicine)
		api.PUT("/medicines/:id", h.UpdateMedicine)
		api.DELETE("/medicines/:id", h.DeleteMedicine)

		api.GET("/purchases", h.GetPurchases)
		api.GET("/purchases/:id", h.GetPurchase)
		api.POST("/purchases", h.CreatePurchase)
		api.PUT("/purchases/:id", h.UpdatePurchase)
		api.DELETE("/purchases/:id", h.DeletePurchase)

		api.GET("/sales", h.GetSales)
		api.GET("/sales/recent", h.GetRecentSales)
		api.GET("/sales/:id", h.GetSale)
		api.POST("/sales", h.CreateSale)
		api.GET("/sales/:id/pdf", h.SalePDF)
		api.POST("/sales/:id/email", h.EmailSale)
		api.POST("/sales/:id/whatsapp", h.WhatsAppShare)

		api.GET("/dashboard", h.GetDashboard)
		api.GET("/inventory", h.GetInventory)

		api.GET("/returns", h.GetReturns)
		api.POST("/returns", h.CreateReturn)

		api.GET("/prescriptions/customer/:customerId", h.GetPrescriptions)
		api.POST("/prescriptions", h.UploadPrescription)

		api.GET("/settings", h.GetSettings)
		api.PUT("/settings", h.UpdateSettings)
		api.POST("/settings/backup", h.BackupSettings)

		api.GET("/reports", h.GetReports)
		api.GET("/reports/export", h.ExportReport)

		api.GET("/branches", h.GetBranches)
		api.GET("/branches/active", h.GetActiveBranches)
		api.POST("/branches", h.CreateBranch)
		api.PUT("/branches/:id", h.UpdateBranch)

		api.GET("/loyalty/:customerId", h.GetLoyalty)
		api.POST("/loyalty/:customerId/redeem", h.RedeemLoyalty)

		api.GET("/online-orders", h.GetOnlineOrders)
		api.GET("/online-orders/status/:status", h.GetOnlineOrdersByStatus)
		api.GET("/online-orders/:id", h.GetOnlineOrder)
		api.POST("/online-orders", h.CreateOnlineOrder)
		api.PUT("/online-orders/:id", h.UpdateOnlineOrder)
		api.DELETE("/online-orders/:id", h.DeleteOnlineOrder)
		api.PUT("/online-orders/:id/status", h.UpdateOnlineOrderStatus)

		api.GET("/reorder-suggestions", h.GetReorderSuggestions)

		api.GET("/accounting/entries", h.GetAccountingEntries)
		api.POST("/accounting/export", h.ExportAccounting)

		api.POST("/notifications/sms", h.SendSMS)
		api.GET("/notifications", h.GetNotificationLogs)
		api.GET("/notifications/low-stock", h.GetLowStockNotifications)
		api.GET("/notifications/near-expiry", h.GetNearExpiryNotifications)
		api.GET("/notifications/inventory-alerts", h.GetInventoryAlerts)
		api.POST("/notifications/low-stock/digest", h.LowStockDigest)

		api.GET("/audit-logs", h.GetAuditLogs)
		api.POST("/audit-logs/by-user", h.GetAuditLogsByUser)
		api.GET("/audit-logs/range", h.GetAuditLogsByRange)

		api.GET("/backups", h.GetBackups)
		api.POST("/backups", h.CreateBackup)

		admin := api.Group("/users")
		admin.Use(authMw.RequireRole(models.RoleAdmin))
		{
			admin.GET("", h.GetUsers)
			admin.POST("", h.CreateUser)
			admin.PUT("/update", h.UpdateUser)
			admin.POST("/delete", h.DeleteUser)
			admin.POST("/reset-password", h.ResetPassword)
			admin.POST("/lock", h.LockUser)
			admin.POST("/unlock", h.UnlockUser)
		}
	}

	addr := ":" + cfg.Port
	log.Printf("MediBill Go server starting on http://localhost%s", addr)
	log.Printf("Login: http://localhost%s/login (admin/admin123)", addr)
	if err := r.Run(addr); err != nil {
		log.Fatal(err)
	}
}
