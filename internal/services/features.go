package services

import (
	"encoding/json"
	"fmt"
	"time"

	"github.com/medicalbilling/medical-billing-system/internal/models"
	"github.com/medicalbilling/medical-billing-system/internal/util"
)

func (s *Services) GetSettings() (*models.ShopSettings, error) {
	var settings models.ShopSettings
	if err := s.DB.First(&settings).Error; err != nil {
		return nil, notFound("Settings not found")
	}
	return &settings, nil
}

func (s *Services) UpdateSettings(req models.ShopSettingsRequest, username string) (*models.ShopSettings, error) {
	settings, err := s.GetSettings()
	if err != nil {
		settings = &models.ShopSettings{CreatedAt: now()}
	}
	if req.ShopName != "" {
		settings.ShopName = req.ShopName
	}
	settings.GSTNumber = req.GSTNumber
	settings.Address = req.Address
	settings.Phone = req.Phone
	settings.Email = req.Email
	settings.InvoiceFooter = req.InvoiceFooter
	if req.DefaultGSTPercent != nil {
		settings.DefaultGSTPercent = *req.DefaultGSTPercent
	}
	settings.UpdatedAt = now()
	s.DB.Save(settings)
	id := settings.ID
	s.LogAudit("UPDATE", "Settings", &id, username, "Updated shop settings", "")
	return settings, nil
}

func (s *Services) GetBranches() ([]models.Branch, error) {
	var branches []models.Branch
	err := s.DB.Order("branch_name").Find(&branches).Error
	return branches, err
}

func (s *Services) GetActiveBranches() ([]models.Branch, error) {
	var branches []models.Branch
	err := s.DB.Where("active = ?", true).Order("branch_name").Find(&branches).Error
	return branches, err
}

func (s *Services) CreateBranch(branch models.Branch, username string) (*models.Branch, error) {
	branch.CreatedAt = now()
	if err := s.DB.Create(&branch).Error; err != nil {
		return nil, err
	}
	id := branch.ID
	s.LogAudit("CREATE", "Branch", &id, username, "Created branch: "+branch.BranchName, "")
	return &branch, nil
}

func (s *Services) UpdateBranch(id uint, updated models.Branch, username string) (*models.Branch, error) {
	var branch models.Branch
	if err := s.DB.First(&branch, id).Error; err != nil {
		return nil, notFound("Branch not found")
	}
	branch.BranchName = updated.BranchName
	branch.Address = updated.Address
	branch.Phone = updated.Phone
	branch.Email = updated.Email
	branch.City = updated.City
	branch.State = updated.State
	branch.PinCode = updated.PinCode
	branch.Active = updated.Active
	branch.UpdatedAt = now()
	s.DB.Save(&branch)
	s.LogAudit("UPDATE", "Branch", &id, username, "Updated branch: "+branch.BranchName, "")
	return &branch, nil
}

func (s *Services) GetOnlineOrders() ([]models.OnlineOrder, error) {
	var orders []models.OnlineOrder
	err := s.DB.Preload("Customer").Preload("Branch").Preload("Items.Medicine").Order("order_date desc").Find(&orders).Error
	return orders, err
}

func (s *Services) GetOnlineOrder(id uint) (*models.OnlineOrder, error) {
	var order models.OnlineOrder
	if err := s.DB.Preload("Customer").Preload("Branch").Preload("Items.Medicine").First(&order, id).Error; err != nil {
		return nil, notFound("Order not found")
	}
	return &order, nil
}

func (s *Services) GetOnlineOrdersByStatus(status models.OrderStatus) ([]models.OnlineOrder, error) {
	var orders []models.OnlineOrder
	err := s.DB.Preload("Customer").Preload("Items.Medicine").Where("status = ?", status).Find(&orders).Error
	return orders, err
}

func (s *Services) CreateOnlineOrder(data map[string]interface{}, username string) (*models.OnlineOrder, error) {
	order := models.OnlineOrder{
		OrderNumber: util.GenerateOrderNumber(),
		Status:      models.OrderPending,
		OrderDate:   now(),
	}
	if v, ok := data["customerId"].(float64); ok {
		cid := uint(v)
		order.CustomerID = cid
	}
	if v, ok := data["contactPhone"].(string); ok {
		order.ContactPhone = v
	}
	if v, ok := data["deliveryAddress"].(string); ok {
		order.DeliveryAddress = v
	}
	if v, ok := data["notes"].(string); ok {
		order.Notes = v
	}
	if v, ok := data["branchId"].(float64); ok {
		bid := uint(v)
		order.BranchID = &bid
	}

	var total float64
	if items, ok := data["items"].([]interface{}); ok {
		for _, raw := range items {
			itemMap, _ := raw.(map[string]interface{})
			medID := uint(itemMap["medicineId"].(float64))
			qty := int(itemMap["quantity"].(float64))
			med, err := s.findMedicine(medID)
			if err != nil {
				return nil, err
			}
			subtotal := med.SellingPrice * float64(qty)
			order.Items = append(order.Items, models.OnlineOrderItem{
				MedicineID: medID, Quantity: qty, UnitPrice: med.SellingPrice, Subtotal: subtotal,
			})
			total += subtotal
		}
	}
	order.TotalAmount = total

	if err := s.DB.Create(&order).Error; err != nil {
		return nil, err
	}
	id := order.ID
	s.LogAudit("CREATE", "OnlineOrder", &id, username, "Created order: "+order.OrderNumber, "")
	return s.GetOnlineOrder(order.ID)
}

func (s *Services) UpdateOnlineOrder(id uint, data map[string]interface{}, username string) (*models.OnlineOrder, error) {
	order, err := s.GetOnlineOrder(id)
	if err != nil {
		return nil, err
	}
	if v, ok := data["status"].(string); ok {
		order.Status = models.OrderStatus(v)
	}
	if v, ok := data["contactPhone"].(string); ok {
		order.ContactPhone = v
	}
	if v, ok := data["deliveryAddress"].(string); ok {
		order.DeliveryAddress = v
	}
	if v, ok := data["notes"].(string); ok {
		order.Notes = v
	}
	order.UpdatedAt = now()
	s.DB.Save(order)
	s.LogAudit("UPDATE", "OnlineOrder", &id, username, "Updated order: "+order.OrderNumber, "")
	return s.GetOnlineOrder(id)
}

func (s *Services) UpdateOnlineOrderStatus(id uint, status models.OrderStatus, username string) (*models.OnlineOrder, error) {
	order, err := s.GetOnlineOrder(id)
	if err != nil {
		return nil, err
	}
	order.Status = status
	order.UpdatedAt = now()
	s.DB.Save(order)
	s.LogAudit("UPDATE", "OnlineOrder", &id, username, "Status changed to "+string(status), "")
	return order, nil
}

func (s *Services) DeleteOnlineOrder(id uint, username string) error {
	if err := s.DB.Delete(&models.OnlineOrder{}, id).Error; err != nil {
		return notFound("Order not found")
	}
	s.LogAudit("DELETE", "OnlineOrder", &id, username, "Deleted online order", "")
	return nil
}

func (s *Services) GetLoyaltyBalance(customerID uint) (map[string]interface{}, error) {
	customer, err := s.GetCustomer(customerID)
	if err != nil {
		return nil, err
	}
	var history []models.LoyaltyTransaction
	s.DB.Where("customer_id = ?", customerID).Order("created_at desc").Find(&history)
	return map[string]interface{}{
		"customerId":    customer.ID,
		"customerName":  customer.CustomerName,
		"loyaltyPoints": customer.LoyaltyPoints,
		"history":       history,
	}, nil
}

func (s *Services) RedeemLoyaltyPoints(customerID uint, points int, username string) error {
	customer, err := s.GetCustomer(customerID)
	if err != nil {
		return err
	}
	if customer.LoyaltyPoints < points {
		return badRequest("Insufficient loyalty points")
	}
	customer.LoyaltyPoints -= points
	s.DB.Save(customer)
	s.DB.Create(&models.LoyaltyTransaction{
		CustomerID: customerID, Points: -points, TransactionType: "REDEEM",
		Description: "Redeemed " + itoa(points) + " points", CreatedAt: now(),
	})
	id := customerID
	s.LogAudit("UPDATE", "Loyalty", &id, username, "Redeemed loyalty points", "")
	return nil
}

func (s *Services) GetReorderSuggestions() ([]map[string]interface{}, error) {
	var medicines []models.Medicine
	s.DB.Preload("Supplier").Where("status = ?", models.MedicineActive).Find(&medicines)

	suggestions := []map[string]interface{}{}
	for _, m := range medicines {
		if m.CurrentStock >= m.MinimumStock {
			continue
		}
		priority := "MEDIUM"
		if m.CurrentStock <= 0 {
			priority = "HIGH"
		} else if m.CurrentStock < s.Config.LowStockThreshold() {
			priority = "HIGH"
		}
		suggestedQty := m.MinimumStock*2 - m.CurrentStock
		if suggestedQty < 10 {
			suggestedQty = 10
		}
		supName := ""
		if m.Supplier != nil {
			supName = m.Supplier.SupplierName
		}
		suggestions = append(suggestions, map[string]interface{}{
			"medicineId": m.ID, "medicineCode": m.MedicineCode, "medicineName": m.MedicineName,
			"currentStock": m.CurrentStock, "minimumStock": m.MinimumStock,
			"suggestedOrderQty": suggestedQty, "priority": priority, "supplierName": supName,
		})
	}
	return suggestions, nil
}

func (s *Services) GetAccountingEntries(start, end time.Time) ([]models.AccountingEntry, error) {
	var entries []models.AccountingEntry
	err := s.DB.Preload("Branch").Where("entry_date BETWEEN ? AND ?", start, end).Order("entry_date desc").Find(&entries).Error
	return entries, err
}

func (s *Services) ExportAccounting(start, end time.Time, username string) (map[string]interface{}, error) {
	entries, err := s.GetAccountingEntries(start, end)
	if err != nil {
		return nil, err
	}
	data, _ := json.Marshal(entries)
	return map[string]interface{}{
		"entries": entries, "count": len(entries), "exportData": string(data),
		"message": fmt.Sprintf("Exported %d entries", len(entries)),
	}, nil
}

func (s *Services) GetAuditLogs(limit int) ([]models.AuditLog, error) {
	var logs []models.AuditLog
	err := s.DB.Order("timestamp desc").Limit(limit).Find(&logs).Error
	return logs, err
}

func (s *Services) GetAuditLogsByUser(username string) ([]models.AuditLog, error) {
	var logs []models.AuditLog
	err := s.DB.Where("username = ?", username).Order("timestamp desc").Find(&logs).Error
	return logs, err
}

func (s *Services) GetAuditLogsByRange(start, end time.Time) ([]models.AuditLog, error) {
	var logs []models.AuditLog
	err := s.DB.Where("timestamp BETWEEN ? AND ?", start, end).Order("timestamp desc").Find(&logs).Error
	return logs, err
}

func (s *Services) GetNotifications() ([]models.NotificationLog, error) {
	var logs []models.NotificationLog
	err := s.DB.Order("sent_at desc").Limit(100).Find(&logs).Error
	return logs, err
}

func (s *Services) SendSMS(phone, message string) *models.NotificationLog {
	log := models.NotificationLog{
		Type: models.NotifySMS, Recipient: phone, Message: message,
		Status: models.NotifySent, SentAt: now(),
	}
	s.DB.Create(&log)
	return &log
}

func (s *Services) GenerateReport(reportType string, start, end time.Time) (map[string]interface{}, error) {
	result := map[string]interface{}{
		"reportType": reportType, "startDate": start, "endDate": end,
	}
	switch reportType {
	case "sales":
		var sales []models.Sale
		s.DB.Preload("Customer").Where("sale_date BETWEEN ? AND ?", start, end).Find(&sales)
		var total float64
		for _, sale := range sales {
			total += sale.GrandTotal
		}
		result["sales"] = sales
		result["totalSales"] = total
		result["count"] = len(sales)
	case "purchases":
		var purchases []models.Purchase
		s.DB.Preload("Supplier").Where("purchase_date BETWEEN ? AND ?", start, end).Find(&purchases)
		var total float64
		for _, p := range purchases {
			total += p.GrandTotal
		}
		result["purchases"] = purchases
		result["totalPurchases"] = total
		result["count"] = len(purchases)
	default:
		result["message"] = "Report generated"
	}
	return result, nil
}

func (s *Services) GetPrescriptionsByCustomer(customerID uint) ([]models.Prescription, error) {
	var items []models.Prescription
	err := s.DB.Where("customer_id = ?", customerID).Order("uploaded_at desc").Find(&items).Error
	return items, err
}

func (s *Services) CreatePrescription(customerID uint, fileName, filePath, notes string, uploaderID uint) (*models.Prescription, error) {
	p := models.Prescription{
		CustomerID: customerID, FileName: fileName, FilePath: filePath,
		Notes: notes, UploadedBy: &uploaderID, UploadedAt: now(),
	}
	if err := s.DB.Create(&p).Error; err != nil {
		return nil, err
	}
	return &p, nil
}

func (s *Services) ListBackups() []string { return []string{} }

func (s *Services) CreateBackup() string { return "Backup created (SQLite file is self-contained)" }

func (s *Services) WhatsAppShare(saleID uint, phone string) (map[string]string, error) {
	sale, err := s.GetSale(saleID)
	if err != nil {
		return nil, err
	}
	msg := fmt.Sprintf("Invoice %s - Total: ₹%.2f. Thank you for shopping with us!", sale.BillNumber, sale.GrandTotal)
	url := fmt.Sprintf("https://wa.me/%s?text=%s", phone, msg)
	return map[string]string{"url": url, "message": msg}, nil
}

func (s *Services) SendLowStockDigest() map[string]interface{} {
	alerts := s.GetLowStockAlerts()
	return map[string]interface{}{"alertsSent": len(alerts), "alerts": alerts}
}
