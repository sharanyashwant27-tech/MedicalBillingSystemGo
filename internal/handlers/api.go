package handlers

import (
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/medicalbilling/medical-billing-system/internal/auth"
	"github.com/medicalbilling/medical-billing-system/internal/middleware"
	"github.com/medicalbilling/medical-billing-system/internal/models"
	"github.com/medicalbilling/medical-billing-system/internal/services"
)

type Handlers struct {
	Services *services.Services
	JWT      *auth.JWTManager
}

func NewHandlers(svc *services.Services, jwt *auth.JWTManager) *Handlers {
	return &Handlers{Services: svc, JWT: jwt}
}

func (h *Handlers) username(c *gin.Context) string {
	if u, ok := c.Get("username"); ok {
		return u.(string)
	}
	return ""
}

func (h *Handlers) handleError(c *gin.Context, err error) {
	if apiErr, ok := err.(*services.APIError); ok {
		c.JSON(apiErr.Code, gin.H{"message": apiErr.Message})
		return
	}
	c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
}

// Auth

func (h *Handlers) LoginAPI(c *gin.Context) {
	var req models.LoginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	resp, err := h.Services.Login(req)
	if err != nil {
		h.handleError(c, err)
		return
	}
	user, _ := h.Services.GetUserByUsername(req.Username)
	token, err := h.JWT.GenerateToken(user)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Failed to generate token"})
		return
	}
	resp.Token = token
	c.JSON(http.StatusOK, models.ApiResponse{Success: true, Message: "Login successful", Data: resp})
}

func (h *Handlers) LoginForm(c *gin.Context) {
	var req struct {
		Username string `form:"username" binding:"required"`
		Password string `form:"password" binding:"required"`
	}
	if err := c.ShouldBind(&req); err != nil {
		c.Redirect(http.StatusFound, "/login?error=true")
		return
	}
	loginReq := models.LoginRequest{Username: req.Username, Password: req.Password}
	if _, err := h.Services.Login(loginReq); err != nil {
		c.Redirect(http.StatusFound, "/login?error=true")
		return
	}
	user, _ := h.Services.GetUserByUsername(req.Username)
	middleware.SetSessionUser(c, user)
	c.Redirect(http.StatusFound, "/dashboard")
}

func (h *Handlers) Logout(c *gin.Context) {
	middleware.ClearSession(c)
	c.Redirect(http.StatusFound, "/logout-success")
}

// Categories

func (h *Handlers) GetCategories(c *gin.Context) {
	items, err := h.Services.GetCategories(c.Query("search"))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, items)
}

func (h *Handlers) CreateCategory(c *gin.Context) {
	var req models.CategoryRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	item, err := h.Services.CreateCategory(req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) UpdateCategory(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	var req models.CategoryRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	item, err := h.Services.UpdateCategory(uint(id), req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) DeleteCategory(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	if err := h.Services.DeleteCategory(uint(id), h.username(c)); err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Category deleted"})
}

// Suppliers

func (h *Handlers) GetSuppliers(c *gin.Context) {
	items, err := h.Services.GetSuppliers(c.Query("search"))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, items)
}

func (h *Handlers) GetSupplier(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	item, err := h.Services.GetSupplier(uint(id))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) CreateSupplier(c *gin.Context) {
	var req models.SupplierRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	item, err := h.Services.CreateSupplier(req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) UpdateSupplier(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	var req models.SupplierRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	item, err := h.Services.UpdateSupplier(uint(id), req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) DeleteSupplier(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	if err := h.Services.DeleteSupplier(uint(id), h.username(c)); err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Supplier deleted"})
}

// Customers

func (h *Handlers) GetCustomers(c *gin.Context) {
	items, err := h.Services.GetCustomers(c.Query("search"))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, items)
}

func (h *Handlers) GetCustomerHistory(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	data, err := h.Services.GetCustomerHistory(uint(id))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, data)
}

func (h *Handlers) CreateCustomer(c *gin.Context) {
	var req models.CustomerRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	item, err := h.Services.CreateCustomer(req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) UpdateCustomer(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	var req models.CustomerRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	item, err := h.Services.UpdateCustomer(uint(id), req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) DeleteCustomer(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	if err := h.Services.DeleteCustomer(uint(id), h.username(c)); err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Customer deleted"})
}

// Medicines

func (h *Handlers) GetMedicines(c *gin.Context) {
	items, err := h.Services.GetMedicines(c.Query("search"))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, items)
}

func (h *Handlers) SearchMedicines(c *gin.Context) {
	items, err := h.Services.SearchMedicines(c.Query("q"))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, items)
}

func (h *Handlers) GetMedicine(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	item, err := h.Services.GetMedicine(uint(id))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) GetMedicineByBarcode(c *gin.Context) {
	items, err := h.Services.GetMedicineByBarcode(c.Param("barcode"))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, items)
}

func (h *Handlers) CreateMedicine(c *gin.Context) {
	var req models.MedicineRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	item, err := h.Services.CreateMedicine(req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) UpdateMedicine(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	var req models.MedicineRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	item, err := h.Services.UpdateMedicine(uint(id), req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) DeleteMedicine(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	if err := h.Services.DeleteMedicine(uint(id), h.username(c)); err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Medicine deactivated"})
}

// Sales & Purchases

func (h *Handlers) GetPurchases(c *gin.Context) {
	items, err := h.Services.GetPurchases()
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, items)
}

func (h *Handlers) GetPurchase(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	item, err := h.Services.GetPurchase(uint(id))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) CreatePurchase(c *gin.Context) {
	var req models.PurchaseRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	item, err := h.Services.CreatePurchase(req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) UpdatePurchase(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	var req models.PurchaseRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	item, err := h.Services.UpdatePurchase(uint(id), req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) DeletePurchase(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	if err := h.Services.DeletePurchase(uint(id), h.username(c)); err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Purchase deleted"})
}

func (h *Handlers) GetSales(c *gin.Context) {
	items, err := h.Services.GetSales()
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, items)
}

func (h *Handlers) GetRecentSales(c *gin.Context) {
	items, err := h.Services.GetRecentSales(10)
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, items)
}

func (h *Handlers) GetSale(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	item, err := h.Services.GetSale(uint(id))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) CreateSale(c *gin.Context) {
	var req models.SaleRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	item, err := h.Services.CreateSale(req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handlers) GetReturns(c *gin.Context) {
	items, err := h.Services.GetReturns()
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, items)
}

func (h *Handlers) CreateReturn(c *gin.Context) {
	var req models.ReturnRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	item, err := h.Services.ProcessReturn(req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, item)
}

// Dashboard & Inventory

func (h *Handlers) GetDashboard(c *gin.Context) {
	data, err := h.Services.GetDashboard()
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, data)
}

func (h *Handlers) GetInventory(c *gin.Context) {
	data, err := h.Services.GetInventorySummary(c.Query("filter"))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, data)
}

// Settings

func (h *Handlers) GetSettings(c *gin.Context) {
	settings, err := h.Services.GetSettings()
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, settings)
}

func (h *Handlers) UpdateSettings(c *gin.Context) {
	var req models.ShopSettingsRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	settings, err := h.Services.UpdateSettings(req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, settings)
}

func (h *Handlers) BackupSettings(c *gin.Context) {
	msg := h.Services.CreateBackup()
	c.JSON(http.StatusOK, gin.H{"message": msg})
}

// Reports

func (h *Handlers) GetReports(c *gin.Context) {
	start, _ := time.Parse("2006-01-02", c.Query("startDate"))
	end, _ := time.Parse("2006-01-02", c.Query("endDate"))
	data, err := h.Services.GenerateReport(c.Query("type"), start, end)
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, data)
}

// Users

func (h *Handlers) GetUsers(c *gin.Context) {
	users, err := h.Services.GetAllUsers()
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, users)
}

func (h *Handlers) CreateUser(c *gin.Context) {
	var req models.UserRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	user, err := h.Services.CreateUser(req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, user)
}

func (h *Handlers) UpdateUser(c *gin.Context) {
	var req models.UserUpdateRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	user, err := h.Services.UpdateUser(req, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, user)
}

func (h *Handlers) DeleteUser(c *gin.Context) {
	var body struct {
		UserID uint `json:"userId"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	if err := h.Services.DeleteUser(body.UserID, h.username(c)); err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "User deleted"})
}

func (h *Handlers) ResetPassword(c *gin.Context) {
	var body struct {
		UserID   uint   `json:"userId"`
		Password string `json:"password"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	if err := h.Services.ResetPassword(body.UserID, body.Password, h.username(c)); err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Password reset"})
}

func (h *Handlers) LockUser(c *gin.Context) {
	var body struct {
		UserID uint `json:"userId"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	if err := h.Services.SetUserLocked(body.UserID, true, h.username(c)); err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "User locked"})
}

func (h *Handlers) UnlockUser(c *gin.Context) {
	var body struct {
		UserID uint `json:"userId"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	if err := h.Services.SetUserLocked(body.UserID, false, h.username(c)); err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "User unlocked"})
}
