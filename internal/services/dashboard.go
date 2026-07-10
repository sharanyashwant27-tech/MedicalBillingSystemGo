package services

import (
	"time"

	"github.com/medicalbilling/medical-billing-system/internal/models"
)

func (s *Services) GetDashboard() (*models.DashboardResponse, error) {
	threshold := s.Config.LowStockThreshold()
	nearDays := s.Config.NearExpiryDays()
	today := time.Now()
	start := time.Date(today.Year(), today.Month(), today.Day(), 0, 0, 0, 0, today.Location())
	end := start.Add(24*time.Hour - time.Nanosecond)

	var todaySales float64
	s.DB.Model(&models.Sale{}).Where("sale_date BETWEEN ? AND ?", start, end).
		Select("COALESCE(SUM(grand_total), 0)").Scan(&todaySales)

	var todaySaleList []models.Sale
	s.DB.Preload("Items.Medicine").Where("sale_date BETWEEN ? AND ?", start, end).Find(&todaySaleList)
	todayProfit := calculateProfit(todaySaleList)

	var available, lowStock, nearExpiry, expired int64
	s.DB.Model(&models.Medicine{}).Where("status = ?", models.MedicineActive).Count(&available)
	s.DB.Model(&models.Medicine{}).Where("status = ? AND current_stock < ?", models.MedicineActive, threshold).Count(&lowStock)

	nearDate := today.AddDate(0, 0, nearDays)
	s.DB.Model(&models.Medicine{}).Where("status = ? AND expiry_date IS NOT NULL AND expiry_date <= ? AND expiry_date >= ?",
		models.MedicineActive, nearDate, today).Count(&nearExpiry)
	s.DB.Model(&models.Medicine{}).Where("status = ? AND expiry_date IS NOT NULL AND expiry_date < ?",
		models.MedicineActive, today).Count(&expired)

	var totalCustomers, totalSuppliers, pendingPayments int64
	s.DB.Model(&models.Customer{}).Count(&totalCustomers)
	s.DB.Model(&models.Supplier{}).Count(&totalSuppliers)
	s.DB.Model(&models.Payment{}).Where("pending = ?", true).Count(&pendingPayments)

	recent, _ := s.GetRecentSales(10)
	recentBills := make([]models.SaleSummary, len(recent))
	for i, sale := range recent {
		custName := "Walk-in"
		if sale.Customer != nil {
			custName = sale.Customer.CustomerName
		}
		recentBills[i] = models.SaleSummary{
			ID: sale.ID, BillNumber: sale.BillNumber, CustomerName: custName,
			GrandTotal: sale.GrandTotal, SaleDate: sale.SaleDate, PaymentMode: sale.PaymentMode,
		}
	}

	alerts := s.GetLowStockAlerts()
	alerts = append(alerts, s.GetNearExpiryAlerts()...)
	alerts = append(alerts, s.GetExpiredAlerts()...)

	return &models.DashboardResponse{
		TodaySales: todaySales, TodayProfit: todayProfit,
		AvailableMedicines: available, LowStockMedicines: lowStock,
		NearExpiryMedicines: nearExpiry, ExpiredMedicines: expired,
		TotalCustomers: totalCustomers, TotalSuppliers: totalSuppliers,
		PendingPayments: pendingPayments, RecentBills: recentBills, Alerts: alerts,
	}, nil
}

func calculateProfit(sales []models.Sale) float64 {
	var profit float64
	for _, sale := range sales {
		for _, item := range sale.Items {
			if item.Medicine == nil {
				continue
			}
			revenue := item.UnitPrice * float64(item.Quantity)
			cost := item.Medicine.PurchasePrice * float64(item.Quantity)
			profit += revenue - cost
		}
	}
	return profit
}

func (s *Services) GetInventorySummary(filter string) (*models.InventorySummary, error) {
	threshold := s.Config.LowStockThreshold()
	nearDays := s.Config.NearExpiryDays()
	today := time.Now()
	nearDate := today.AddDate(0, 0, nearDays)

	medicines, err := s.GetMedicines("")
	if err != nil {
		return nil, err
	}

	var filtered []models.MedicineResponse
	var lowStock, outOfStock, nearExpiry, expired int64
	var valuation float64

	for _, m := range medicines {
		valuation += m.PurchasePrice * float64(m.CurrentStock)
		if m.CurrentStock <= 0 {
			outOfStock++
		}
		if m.CurrentStock < threshold {
			lowStock++
		}
		isNear := m.ExpiryDate != nil && !m.ExpiryDate.Before(today) && !m.ExpiryDate.After(nearDate)
		isExpired := m.ExpiryDate != nil && m.ExpiryDate.Before(today)
		if isNear {
			nearExpiry++
		}
		if isExpired {
			expired++
		}

		include := filter == "" || filter == "ALL"
		switch filter {
		case "LOW_STOCK":
			include = m.CurrentStock < threshold
		case "NEAR_EXPIRY":
			include = isNear
		case "EXPIRED":
			include = isExpired
		}
		if include {
			filtered = append(filtered, m)
		}
	}

	return &models.InventorySummary{
		TotalMedicines: int64(len(medicines)),
		LowStock:       lowStock,
		OutOfStock:     outOfStock,
		NearExpiry:     nearExpiry,
		Expired:        expired,
		Valuation:      valuation,
		Items:          filtered,
	}, nil
}

func (s *Services) GetLowStockAlerts() []models.AlertItem {
	threshold := s.Config.LowStockThreshold()
	var medicines []models.Medicine
	s.DB.Preload("Category").Preload("Supplier").
		Where("status = ? AND current_stock < ?", models.MedicineActive, threshold).
		Find(&medicines)

	alerts := make([]models.AlertItem, 0, len(medicines))
	for _, m := range medicines {
		status := "LOW_STOCK"
		severity := "warning"
		if m.CurrentStock <= 0 {
			status = "OUT_OF_STOCK"
			severity = "danger"
		}
		shortage := threshold - m.CurrentStock
		if shortage < 0 {
			shortage = 0
		}
		catName, supName, supPhone := "", "", ""
		if m.Category != nil {
			catName = m.Category.Name
		}
		if m.Supplier != nil {
			supName = m.Supplier.SupplierName
			supPhone = m.Supplier.Phone
		}
		alerts = append(alerts, models.AlertItem{
			Type: "LOW_STOCK", Message: m.MedicineName + " is low on stock (" + itoa(m.CurrentStock) + " units)",
			Severity: severity, MedicineID: m.ID, MedicineCode: m.MedicineCode, MedicineName: m.MedicineName,
			CurrentStock: m.CurrentStock, MinimumStock: m.MinimumStock, Shortage: shortage,
			CategoryName: catName, SupplierName: supName, SupplierPhone: supPhone,
			BatchNumber: m.BatchNumber, ExpiryDate: m.ExpiryDate, RackNumber: m.RackNumber, StockStatus: status,
		})
	}
	return alerts
}

func (s *Services) GetNearExpiryAlerts() []models.AlertItem {
	nearDays := s.Config.NearExpiryDays()
	today := time.Now()
	nearDate := today.AddDate(0, 0, nearDays)
	var medicines []models.Medicine
	s.DB.Preload("Category").Preload("Supplier").
		Where("status = ? AND expiry_date IS NOT NULL AND expiry_date <= ? AND expiry_date >= ?",
			models.MedicineActive, nearDate, today).Find(&medicines)

	alerts := make([]models.AlertItem, 0, len(medicines))
	for _, m := range medicines {
		daysLeft := int(m.ExpiryDate.Sub(today).Hours() / 24)
		catName, supName, supPhone := "", "", ""
		if m.Category != nil {
			catName = m.Category.Name
		}
		if m.Supplier != nil {
			supName = m.Supplier.SupplierName
			supPhone = m.Supplier.Phone
		}
		alerts = append(alerts, models.AlertItem{
			Type: "NEAR_EXPIRY", Message: m.MedicineName + " expires on " + m.ExpiryDate.Format("02-01-2006"),
			Severity: "info", MedicineID: m.ID, MedicineCode: m.MedicineCode, MedicineName: m.MedicineName,
			CurrentStock: m.CurrentStock, MinimumStock: m.MinimumStock, Shortage: daysLeft,
			CategoryName: catName, SupplierName: supName, SupplierPhone: supPhone,
			BatchNumber: m.BatchNumber, ExpiryDate: m.ExpiryDate, RackNumber: m.RackNumber, StockStatus: "NEAR_EXPIRY",
		})
	}
	return alerts
}

func (s *Services) GetExpiredAlerts() []models.AlertItem {
	today := time.Now()
	var medicines []models.Medicine
	s.DB.Preload("Category").Preload("Supplier").
		Where("status = ? AND expiry_date IS NOT NULL AND expiry_date < ?", models.MedicineActive, today).
		Find(&medicines)

	alerts := make([]models.AlertItem, 0, len(medicines))
	for _, m := range medicines {
		catName, supName, supPhone := "", "", ""
		if m.Category != nil {
			catName = m.Category.Name
		}
		if m.Supplier != nil {
			supName = m.Supplier.SupplierName
			supPhone = m.Supplier.Phone
		}
		alerts = append(alerts, models.AlertItem{
			Type: "EXPIRED", Message: m.MedicineName + " expired on " + m.ExpiryDate.Format("02-01-2006"),
			Severity: "danger", MedicineID: m.ID, MedicineCode: m.MedicineCode, MedicineName: m.MedicineName,
			CurrentStock: m.CurrentStock, MinimumStock: m.MinimumStock,
			CategoryName: catName, SupplierName: supName, SupplierPhone: supPhone,
			BatchNumber: m.BatchNumber, ExpiryDate: m.ExpiryDate, RackNumber: m.RackNumber, StockStatus: "EXPIRED",
		})
	}
	return alerts
}

func itoa(n int) string {
	if n == 0 {
		return "0"
	}
	neg := false
	if n < 0 {
		neg = true
		n = -n
	}
	var digits []byte
	for n > 0 {
		digits = append([]byte{byte('0' + n%10)}, digits...)
		n /= 10
	}
	if neg {
		digits = append([]byte{'-'}, digits...)
	}
	return string(digits)
}
