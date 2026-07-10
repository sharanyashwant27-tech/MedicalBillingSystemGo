package handlers

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/medicalbilling/medical-billing-system/internal/models"
	"github.com/medicalbilling/medical-billing-system/internal/templates"
)

func (h *Handlers) pageData(c *gin.Context, pageTitle, activePage string) gin.H {
	data := gin.H{
		"PageTitle":  pageTitle,
		"ActivePage": activePage,
		"Username":   h.username(c),
		"IsAdmin":    false,
	}
	if userVal, ok := c.Get("user"); ok {
		user := userVal.(*models.User)
		data["IsAdmin"] = user.HasRole(models.RoleAdmin)
	}
	return data
}

func (h *Handlers) LoginPage(c *gin.Context) {
	data := gin.H{"Error": ""}
	if c.Query("error") != "" {
		data["Error"] = "Invalid username or password"
	}
	templates.Render(c, http.StatusOK, "login.html", data)
}

func (h *Handlers) ForgotPasswordPage(c *gin.Context) {
	templates.Render(c, http.StatusOK, "forgot-password.html", gin.H{})
}

func (h *Handlers) LogoutSuccessPage(c *gin.Context) {
	templates.Render(c, http.StatusOK, "logout-success.html", gin.H{})
}

func (h *Handlers) DashboardPage(c *gin.Context) {
	dashboard, _ := h.Services.GetDashboard()
	data := h.pageData(c, "Dashboard", "dashboard")
	data["Dashboard"] = dashboard
	templates.Render(c, http.StatusOK, "dashboard.html", data)
}

func (h *Handlers) MedicinesPage(c *gin.Context) {
	medicines, _ := h.Services.GetMedicines(c.Query("search"))
	categories, _ := h.Services.GetCategories("")
	suppliers, _ := h.Services.GetSuppliers("")
	data := h.pageData(c, "Medicine Master", "medicines")
	data["Medicines"] = medicines
	data["Categories"] = categories
	data["Suppliers"] = suppliers
	templates.Render(c, http.StatusOK, "medicines.html", data)
}

func (h *Handlers) CategoriesPage(c *gin.Context) {
	categories, _ := h.Services.GetCategories("")
	data := h.pageData(c, "Category Master", "categories")
	data["Categories"] = categories
	templates.Render(c, http.StatusOK, "categories.html", data)
}

func (h *Handlers) SuppliersPage(c *gin.Context) {
	suppliers, _ := h.Services.GetSuppliers("")
	data := h.pageData(c, "Supplier Master", "suppliers")
	data["Suppliers"] = suppliers
	templates.Render(c, http.StatusOK, "suppliers.html", data)
}

func (h *Handlers) CustomersPage(c *gin.Context) {
	customers, _ := h.Services.GetCustomers("")
	data := h.pageData(c, "Customer Master", "customers")
	data["Customers"] = customers
	templates.Render(c, http.StatusOK, "customers.html", data)
}

func (h *Handlers) BillingPage(c *gin.Context) {
	customers, _ := h.Services.GetCustomers("")
	data := h.pageData(c, "Billing", "billing")
	data["Customers"] = customers
	templates.Render(c, http.StatusOK, "billing.html", data)
}

func (h *Handlers) PurchasesPage(c *gin.Context) {
	suppliers, _ := h.Services.GetSuppliers("")
	medicines, _ := h.Services.GetMedicines("")
	data := h.pageData(c, "Purchase", "purchases")
	data["Suppliers"] = suppliers
	data["Medicines"] = medicines
	templates.Render(c, http.StatusOK, "purchases.html", data)
}

func (h *Handlers) inventoryPage(c *gin.Context, filter, pageTitle string) {
	inventory, _ := h.Services.GetInventorySummary(filter)
	data := h.pageData(c, pageTitle, "inventory")
	data["Inventory"] = inventory
	data["Filter"] = filter
	templates.Render(c, http.StatusOK, "inventory.html", data)
}

func (h *Handlers) InventoryPage(c *gin.Context) {
	h.inventoryPage(c, c.Query("filter"), "Inventory")
}

func (h *Handlers) ExpiredMedicinesPage(c *gin.Context) {
	h.inventoryPage(c, "EXPIRED", "Expired Medicines")
}

func (h *Handlers) NearExpiryMedicinesPage(c *gin.Context) {
	h.inventoryPage(c, "NEAR_EXPIRY", "Near Expiry Medicines (30 Days)")
}

func (h *Handlers) LowStockMedicinesPage(c *gin.Context) {
	h.inventoryPage(c, "LOW_STOCK", "Low Stock Medicines (< 10 units)")
}

func (h *Handlers) ReportsPage(c *gin.Context) {
	data := h.pageData(c, "Reports", "reports")
	templates.Render(c, http.StatusOK, "reports.html", data)
}

func (h *Handlers) ReturnsPage(c *gin.Context) {
	medicines, _ := h.Services.GetMedicines("")
	data := h.pageData(c, "Returns", "returns")
	data["Medicines"] = medicines
	templates.Render(c, http.StatusOK, "returns.html", data)
}

func (h *Handlers) PrescriptionsPage(c *gin.Context) {
	customers, _ := h.Services.GetCustomers("")
	data := h.pageData(c, "Prescriptions", "prescriptions")
	data["Customers"] = customers
	templates.Render(c, http.StatusOK, "prescriptions.html", data)
}

func (h *Handlers) SettingsPage(c *gin.Context) {
	settings, _ := h.Services.GetSettings()
	data := h.pageData(c, "Settings", "settings")
	data["Settings"] = settings
	templates.Render(c, http.StatusOK, "settings.html", data)
}

func (h *Handlers) UsersPage(c *gin.Context) {
	users, _ := h.Services.GetAllUsers()
	data := h.pageData(c, "User Management", "users")
	data["Users"] = users
	templates.Render(c, http.StatusOK, "users.html", data)
}

func (h *Handlers) BranchesPage(c *gin.Context) {
	branches, _ := h.Services.GetBranches()
	data := h.pageData(c, "Branch Management", "branches")
	data["Branches"] = branches
	templates.Render(c, http.StatusOK, "branches.html", data)
}

func (h *Handlers) OnlineOrdersPage(c *gin.Context) {
	orders, _ := h.Services.GetOnlineOrders()
	customers, _ := h.Services.GetCustomers("")
	medicines, _ := h.Services.GetMedicines("")
	branches, _ := h.Services.GetActiveBranches()
	data := h.pageData(c, "Online Orders", "online-orders")
	data["Orders"] = orders
	data["Customers"] = customers
	data["Medicines"] = medicines
	data["Branches"] = branches
	templates.Render(c, http.StatusOK, "online-orders.html", data)
}

func (h *Handlers) ReorderSuggestionsPage(c *gin.Context) {
	suggestions, _ := h.Services.GetReorderSuggestions()
	data := h.pageData(c, "AI Reorder Suggestions", "reorder")
	data["Suggestions"] = suggestions
	templates.Render(c, http.StatusOK, "reorder-suggestions.html", data)
}

func (h *Handlers) AuditLogsPage(c *gin.Context) {
	logs, _ := h.Services.GetAuditLogs(100)
	data := h.pageData(c, "Audit Logs", "audit")
	data["Logs"] = logs
	templates.Render(c, http.StatusOK, "audit-logs.html", data)
}
