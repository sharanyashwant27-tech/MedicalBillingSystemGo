package handlers

import (
	"fmt"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/medicalbilling/medical-billing-system/internal/models"
)

func (h *Handlers) GetBranches(c *gin.Context) {
	branches, err := h.Services.GetBranches()
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, branches)
}

func (h *Handlers) GetActiveBranches(c *gin.Context) {
	branches, err := h.Services.GetActiveBranches()
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, branches)
}

func (h *Handlers) CreateBranch(c *gin.Context) {
	var branch models.Branch
	if err := c.ShouldBindJSON(&branch); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	created, err := h.Services.CreateBranch(branch, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, created)
}

func (h *Handlers) UpdateBranch(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	var branch models.Branch
	if err := c.ShouldBindJSON(&branch); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	updated, err := h.Services.UpdateBranch(uint(id), branch, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, updated)
}

func (h *Handlers) GetLoyalty(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("customerId"), 10, 64)
	data, err := h.Services.GetLoyaltyBalance(uint(id))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, data)
}

func (h *Handlers) RedeemLoyalty(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("customerId"), 10, 64)
	var body struct {
		Points int `json:"points"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	if err := h.Services.RedeemLoyaltyPoints(uint(id), body.Points, h.username(c)); err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Points redeemed"})
}

func (h *Handlers) GetOnlineOrders(c *gin.Context) {
	orders, err := h.Services.GetOnlineOrders()
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, orders)
}

func (h *Handlers) GetOnlineOrdersByStatus(c *gin.Context) {
	status := models.OrderStatus(c.Param("status"))
	orders, err := h.Services.GetOnlineOrdersByStatus(status)
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, orders)
}

func (h *Handlers) GetOnlineOrder(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	order, err := h.Services.GetOnlineOrder(uint(id))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, order)
}

func (h *Handlers) CreateOnlineOrder(c *gin.Context) {
	var data map[string]interface{}
	if err := c.ShouldBindJSON(&data); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	order, err := h.Services.CreateOnlineOrder(data, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, order)
}

func (h *Handlers) UpdateOnlineOrder(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	var data map[string]interface{}
	if err := c.ShouldBindJSON(&data); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	order, err := h.Services.UpdateOnlineOrder(uint(id), data, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, order)
}

func (h *Handlers) DeleteOnlineOrder(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	if err := h.Services.DeleteOnlineOrder(uint(id), h.username(c)); err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Order deleted"})
}

func (h *Handlers) UpdateOnlineOrderStatus(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	var body struct {
		Status string `json:"status"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	order, err := h.Services.UpdateOnlineOrderStatus(uint(id), models.OrderStatus(body.Status), h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, order)
}

func (h *Handlers) GetReorderSuggestions(c *gin.Context) {
	suggestions, err := h.Services.GetReorderSuggestions()
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, suggestions)
}

func (h *Handlers) GetAccountingEntries(c *gin.Context) {
	start, _ := time.Parse("2006-01-02", c.Query("startDate"))
	end, _ := time.Parse("2006-01-02", c.Query("endDate"))
	entries, err := h.Services.GetAccountingEntries(start, end)
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, entries)
}

func (h *Handlers) ExportAccounting(c *gin.Context) {
	start, _ := time.Parse("2006-01-02", c.Query("startDate"))
	end, _ := time.Parse("2006-01-02", c.Query("endDate"))
	data, err := h.Services.ExportAccounting(start, end, h.username(c))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, data)
}

func (h *Handlers) SendSMS(c *gin.Context) {
	var body struct {
		Phone   string `json:"phone"`
		Message string `json:"message"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	log := h.Services.SendSMS(body.Phone, body.Message)
	c.JSON(http.StatusOK, log)
}

func (h *Handlers) WhatsAppShare(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	var body struct {
		Phone string `json:"phone"`
	}
	_ = c.ShouldBindJSON(&body)
	data, err := h.Services.WhatsAppShare(uint(id), body.Phone)
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, data)
}

func (h *Handlers) GetAuditLogs(c *gin.Context) {
	limit, _ := strconv.Atoi(c.DefaultQuery("limit", "50"))
	logs, err := h.Services.GetAuditLogs(limit)
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, logs)
}

func (h *Handlers) GetAuditLogsByUser(c *gin.Context) {
	var body struct {
		Username string `json:"username"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid request"})
		return
	}
	logs, err := h.Services.GetAuditLogsByUser(body.Username)
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, logs)
}

func (h *Handlers) GetAuditLogsByRange(c *gin.Context) {
	start, _ := time.Parse("2006-01-02", c.Query("start"))
	end, _ := time.Parse("2006-01-02", c.Query("end"))
	logs, err := h.Services.GetAuditLogsByRange(start, end)
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, logs)
}

func (h *Handlers) GetBackups(c *gin.Context) {
	c.JSON(http.StatusOK, h.Services.ListBackups())
}

func (h *Handlers) CreateBackup(c *gin.Context) {
	msg := h.Services.CreateBackup()
	c.JSON(http.StatusOK, gin.H{"message": msg})
}

func (h *Handlers) GetNotificationLogs(c *gin.Context) {
	logs, err := h.Services.GetNotifications()
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, logs)
}

func (h *Handlers) GetLowStockNotifications(c *gin.Context) {
	c.JSON(http.StatusOK, h.Services.GetLowStockAlerts())
}

func (h *Handlers) GetNearExpiryNotifications(c *gin.Context) {
	c.JSON(http.StatusOK, h.Services.GetNearExpiryAlerts())
}

func (h *Handlers) GetInventoryAlerts(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"lowStock":   h.Services.GetLowStockAlerts(),
		"nearExpiry": h.Services.GetNearExpiryAlerts(),
	})
}

func (h *Handlers) LowStockDigest(c *gin.Context) {
	c.JSON(http.StatusOK, h.Services.SendLowStockDigest())
}

func (h *Handlers) GetPrescriptions(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("customerId"), 10, 64)
	items, err := h.Services.GetPrescriptionsByCustomer(uint(id))
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, items)
}

func (h *Handlers) UploadPrescription(c *gin.Context) {
	customerID, _ := strconv.ParseUint(c.PostForm("customerId"), 10, 64)
	file, err := c.FormFile("file")
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "File required"})
		return
	}
	path := "uploads/" + file.Filename
	_ = c.SaveUploadedFile(file, path)
	userVal, _ := c.Get("user")
	user := userVal.(*models.User)
	p, err := h.Services.CreatePrescription(uint(customerID), file.Filename, path, c.PostForm("notes"), user.ID)
	if err != nil {
		h.handleError(c, err)
		return
	}
	c.JSON(http.StatusOK, p)
}

func (h *Handlers) EmailSale(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{"message": "Email sent (simulated)"})
}

func (h *Handlers) SalePDF(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 10, 64)
	sale, err := h.Services.GetSale(uint(id))
	if err != nil {
		h.handleError(c, err)
		return
	}
	data, err := h.Services.ExportSalePDF(sale)
	if err != nil {
		h.handleError(c, err)
		return
	}
	filename := fmt.Sprintf("invoice-%s.pdf", sale.BillNumber)
	c.Header("Content-Type", "application/pdf")
	c.Header("Content-Disposition", fmt.Sprintf("attachment; filename=%q", filename))
	c.Data(http.StatusOK, "application/pdf", data)
}

func (h *Handlers) ExportReport(c *gin.Context) {
	start, _ := time.Parse("2006-01-02", c.Query("startDate"))
	end, _ := time.Parse("2006-01-02", c.Query("endDate"))
	reportType := c.Query("type")
	format := c.DefaultQuery("format", "csv")

	data, contentType, err := h.Services.ExportReport(reportType, format, start, end)
	if err != nil {
		h.handleError(c, err)
		return
	}
	ext := "csv"
	if format == "xlsx" || format == "excel" {
		ext = "xlsx"
	}
	filename := fmt.Sprintf("report-%s.%s", reportType, ext)
	c.Header("Content-Disposition", fmt.Sprintf("attachment; filename=%q", filename))
	c.Data(http.StatusOK, contentType, data)
}
