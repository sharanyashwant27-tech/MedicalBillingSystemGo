package services

import (
	"math"
	"strings"
	"time"

	"github.com/medicalbilling/medical-billing-system/internal/models"
	"github.com/medicalbilling/medical-billing-system/internal/util"
	"gorm.io/gorm"
)

func (s *Services) GetPurchases() ([]models.Purchase, error) {
	var items []models.Purchase
	err := s.DB.Preload("Supplier").Preload("Items.Medicine").Order("purchase_date desc").Find(&items).Error
	return items, err
}

func (s *Services) GetPurchase(id uint) (*models.Purchase, error) {
	var purchase models.Purchase
	if err := s.DB.Preload("Supplier").Preload("Items.Medicine").First(&purchase, id).Error; err != nil {
		return nil, notFound("Purchase not found")
	}
	return &purchase, nil
}

func (s *Services) CreatePurchase(req models.PurchaseRequest, username string) (*models.Purchase, error) {
	pdate, err := time.Parse("2006-01-02", req.PurchaseDate)
	if err != nil {
		return nil, badRequest("Invalid purchase date")
	}
	invoice, err := s.resolveInvoiceNumber(req.InvoiceNumber, 0)
	if err != nil {
		return nil, err
	}

	var purchase models.Purchase
	err = s.DB.Transaction(func(tx *gorm.DB) error {
		purchase = models.Purchase{
			InvoiceNumber: invoice,
			SupplierID:    req.SupplierID,
			PurchaseDate:  pdate,
			CreatedAt:     now(),
		}
		total, gst, grand, items, err := s.buildPurchaseItems(tx, req.Items)
		if err != nil {
			return err
		}
		purchase.Items = items
		purchase.TotalAmount = total
		purchase.GSTAmount = gst
		purchase.GrandTotal = grand
		if err := tx.Create(&purchase).Error; err != nil {
			return err
		}
		return nil
	})
	if err != nil {
		return nil, err
	}
	id := purchase.ID
	s.LogAudit("CREATE", "Purchase", &id, username, "Created purchase: "+purchase.InvoiceNumber, "")
	s.recordPurchaseAccounting(&purchase)
	return s.GetPurchase(purchase.ID)
}

func (s *Services) UpdatePurchase(id uint, req models.PurchaseRequest, username string) (*models.Purchase, error) {
	existing, err := s.GetPurchase(id)
	if err != nil {
		return nil, err
	}
	pdate, err := time.Parse("2006-01-02", req.PurchaseDate)
	if err != nil {
		return nil, badRequest("Invalid purchase date")
	}
	invoice := strings.TrimSpace(req.InvoiceNumber)
	if invoice == "" {
		invoice = existing.InvoiceNumber
	}
	invoice, err = s.resolveInvoiceNumber(invoice, id)
	if err != nil {
		return nil, err
	}

	err = s.DB.Transaction(func(tx *gorm.DB) error {
		if err := s.reversePurchaseStock(tx, existing.Items); err != nil {
			return err
		}
		if err := tx.Where("purchase_id = ?", id).Delete(&models.PurchaseItem{}).Error; err != nil {
			return err
		}

		total, gst, grand, items, err := s.buildPurchaseItems(tx, req.Items)
		if err != nil {
			return err
		}
		for i := range items {
			items[i].PurchaseID = id
		}
		if err := tx.Create(&items).Error; err != nil {
			return err
		}

		existing.Items = nil
		existing.InvoiceNumber = invoice
		existing.SupplierID = req.SupplierID
		existing.PurchaseDate = pdate
		existing.TotalAmount = total
		existing.GSTAmount = gst
		existing.GrandTotal = grand
		existing.UpdatedAt = now()
		return tx.Omit("Items").Save(existing).Error
	})
	if err != nil {
		return nil, err
	}
	s.LogAudit("UPDATE", "Purchase", &id, username, "Updated purchase: "+invoice, "")
	return s.GetPurchase(id)
}

func (s *Services) DeletePurchase(id uint, username string) error {
	purchase, err := s.GetPurchase(id)
	if err != nil {
		return err
	}
	err = s.DB.Transaction(func(tx *gorm.DB) error {
		if err := s.reversePurchaseStock(tx, purchase.Items); err != nil {
			return err
		}
		if err := tx.Where("purchase_id = ?", id).Delete(&models.PurchaseItem{}).Error; err != nil {
			return err
		}
		return tx.Delete(&models.Purchase{}, id).Error
	})
	if err != nil {
		return err
	}
	s.LogAudit("DELETE", "Purchase", &id, username, "Deleted purchase: "+purchase.InvoiceNumber, "")
	return nil
}

func (s *Services) resolveInvoiceNumber(invoice string, excludeID uint) (string, error) {
	invoice = strings.TrimSpace(invoice)
	if invoice == "" {
		invoice = util.GenerateInvoiceNumber()
	}
	var count int64
	q := s.DB.Model(&models.Purchase{}).Where("invoice_number = ?", invoice)
	if excludeID > 0 {
		q = q.Where("id <> ?", excludeID)
	}
	q.Count(&count)
	if count > 0 {
		return "", badRequest("Invoice number already exists")
	}
	return invoice, nil
}

func (s *Services) buildPurchaseItems(tx *gorm.DB, itemReqs []models.PurchaseItemRequest) (float64, float64, float64, []models.PurchaseItem, error) {
	var total, gst, grand float64
	items := make([]models.PurchaseItem, 0, len(itemReqs))

	for _, itemReq := range itemReqs {
		var med models.Medicine
		if err := tx.First(&med, itemReq.MedicineID).Error; err != nil {
			return 0, 0, 0, nil, notFound("Medicine not found")
		}
		lineGST := itemReq.GSTAmount
		subtotal := float64(itemReq.Quantity)*itemReq.PurchasePrice + lineGST
		exp, _ := util.ParseDate(derefStr(itemReq.ExpiryDate))

		items = append(items, models.PurchaseItem{
			MedicineID: itemReq.MedicineID, Quantity: itemReq.Quantity,
			PurchasePrice: itemReq.PurchasePrice, GSTAmount: lineGST, Subtotal: subtotal,
			ExpiryDate: exp, BatchNumber: itemReq.BatchNumber,
		})
		total += float64(itemReq.Quantity) * itemReq.PurchasePrice
		gst += lineGST
		grand += subtotal

		if err := s.adjustStockDB(tx, med.ID, itemReq.Quantity); err != nil {
			return 0, 0, 0, nil, err
		}
		med.PurchasePrice = itemReq.PurchasePrice
		if exp != nil {
			med.ExpiryDate = exp
		}
		if itemReq.BatchNumber != "" {
			med.BatchNumber = itemReq.BatchNumber
		}
		if err := tx.Save(&med).Error; err != nil {
			return 0, 0, 0, nil, err
		}
	}
	return total, gst, grand, items, nil
}

func (s *Services) reversePurchaseStock(tx *gorm.DB, items []models.PurchaseItem) error {
	for _, item := range items {
		if err := s.adjustStockDB(tx, item.MedicineID, -item.Quantity); err != nil {
			return err
		}
	}
	return nil
}

func (s *Services) adjustStockDB(db *gorm.DB, medicineID uint, change int) error {
	var med models.Medicine
	if err := db.First(&med, medicineID).Error; err != nil {
		return notFound("Medicine not found")
	}
	newStock := med.CurrentStock + change
	if newStock < 0 {
		return badRequest("Insufficient stock for medicine: " + med.MedicineName)
	}
	med.CurrentStock = newStock
	return db.Save(&med).Error
}

func (s *Services) GetSales() ([]models.Sale, error) {
	var items []models.Sale
	err := s.DB.Preload("Customer").Preload("Items.Medicine").Order("sale_date desc").Find(&items).Error
	return items, err
}

func (s *Services) GetSale(id uint) (*models.Sale, error) {
	var sale models.Sale
	if err := s.DB.Preload("Customer").Preload("Items.Medicine").Preload("CreatedBy").First(&sale, id).Error; err != nil {
		return nil, notFound("Sale not found")
	}
	return &sale, nil
}

func (s *Services) GetRecentSales(limit int) ([]models.Sale, error) {
	var items []models.Sale
	err := s.DB.Preload("Customer").Order("sale_date desc").Limit(limit).Find(&items).Error
	return items, err
}

func (s *Services) CreateSale(req models.SaleRequest, username string) (*models.Sale, error) {
	user, err := s.GetUserByUsername(username)
	if err != nil {
		return nil, err
	}

	sale := models.Sale{
		BillNumber:  util.GenerateBillNumber(),
		UserID:      user.ID,
		PaymentMode: req.PaymentMode,
		AmountPaid:  req.AmountPaid,
		SaleDate:    now(),
		CreatedAt:   now(),
	}
	if user.BranchID != nil {
		sale.BranchID = user.BranchID
	}
	if req.CustomerID != nil {
		sale.CustomerID = req.CustomerID
	}

	var subtotal, discount, gst float64
	for _, itemReq := range req.Items {
		med, err := s.findMedicine(itemReq.MedicineID)
		if err != nil {
			return nil, err
		}
		if med.CurrentStock < itemReq.Quantity {
			return nil, badRequest("Insufficient stock for: " + med.MedicineName)
		}
		discPct := med.DiscountPercent
		if itemReq.DiscountPercent != nil {
			discPct = *itemReq.DiscountPercent
		}
		lineTotal := med.SellingPrice * float64(itemReq.Quantity)
		lineDiscount := lineTotal * discPct / 100
		taxable := lineTotal - lineDiscount
		lineGST := taxable * med.GSTPercent / 100
		itemSubtotal := taxable + lineGST

		batch := med.BatchNumber
		if itemReq.BatchNumber != "" {
			batch = itemReq.BatchNumber
		}
		sale.Items = append(sale.Items, models.SaleItem{
			MedicineID: med.ID, BatchNumber: batch, Quantity: itemReq.Quantity,
			UnitPrice: med.SellingPrice, DiscountPercent: discPct,
			GSTAmount: lineGST, Subtotal: itemSubtotal,
		})
		subtotal += lineTotal
		discount += lineDiscount
		gst += lineGST

		if err := s.AdjustStock(med.ID, -itemReq.Quantity); err != nil {
			return nil, err
		}
	}

	sale.Subtotal = subtotal
	sale.DiscountAmount = discount
	sale.GSTAmount = gst
	sale.GrandTotal = subtotal - discount + gst
	sale.ReturnAmount = math.Max(req.AmountPaid-sale.GrandTotal, 0)

	if err := s.DB.Create(&sale).Error; err != nil {
		return nil, err
	}
	id := sale.ID
	s.LogAudit("CREATE", "Sale", &id, username, "Created sale: "+sale.BillNumber, "")
	s.earnLoyaltyPoints(&sale)
	s.recordSaleAccounting(&sale)
	s.DB.Preload("Customer").Preload("Items.Medicine").First(&sale, sale.ID)
	return &sale, nil
}

func (s *Services) GetReturns() ([]models.MedicineReturn, error) {
	var items []models.MedicineReturn
	err := s.DB.Preload("Medicine").Preload("ProcessedBy").Order("return_date desc").Find(&items).Error
	return items, err
}

func (s *Services) ProcessReturn(req models.ReturnRequest, username string) (*models.MedicineReturn, error) {
	user, err := s.GetUserByUsername(username)
	if err != nil {
		return nil, err
	}
	med, err := s.findMedicine(req.MedicineID)
	if err != nil {
		return nil, err
	}

	refund := med.SellingPrice * float64(req.Quantity)
	ret := models.MedicineReturn{
		ReturnNumber: util.GenerateReturnNumber(),
		ReturnType:   req.ReturnType,
		MedicineID:   req.MedicineID,
		SaleID:       req.SaleID,
		PurchaseID:   req.PurchaseID,
		Quantity:     req.Quantity,
		RefundAmount: refund,
		Reason:       req.Reason,
		UserID:       user.ID,
		ReturnDate:   now(),
	}

	change := req.Quantity
	if req.ReturnType == models.ReturnSales {
		change = req.Quantity
	} else {
		change = -req.Quantity
	}
	if err := s.AdjustStock(med.ID, change); err != nil {
		return nil, err
	}

	if err := s.DB.Create(&ret).Error; err != nil {
		return nil, err
	}
	id := ret.ID
	s.LogAudit("CREATE", "Return", &id, username, "Processed return: "+ret.ReturnNumber, "")
	s.DB.Preload("Medicine").Preload("ProcessedBy").First(&ret, ret.ID)
	return &ret, nil
}

func (s *Services) earnLoyaltyPoints(sale *models.Sale) {
	if sale.CustomerID == nil {
		return
	}
	points := int(sale.GrandTotal / 100)
	if points <= 0 {
		return
	}
	s.DB.Model(&models.Customer{}).Where("id = ?", *sale.CustomerID).
		UpdateColumn("loyalty_points", gorm.Expr("loyalty_points + ?", points))
	s.DB.Create(&models.LoyaltyTransaction{
		CustomerID: *sale.CustomerID, SaleID: &sale.ID, Points: points,
		TransactionType: "EARN", Description: "Points earned from sale " + sale.BillNumber,
		CreatedAt: now(),
	})
}

func (s *Services) recordSaleAccounting(sale *models.Sale) {
	entry := models.AccountingEntry{
		EntryNumber: util.GenerateEntryNumber(), EntryType: models.EntrySale,
		Description: "Sale " + sale.BillNumber, DebitAmount: sale.GrandTotal,
		CreditAmount: sale.GrandTotal, EntryDate: now(), ReferenceID: &sale.ID,
		ReferenceType: "SALE", CreatedAt: now(),
	}
	if sale.BranchID != nil {
		entry.BranchID = sale.BranchID
	}
	s.DB.Create(&entry)
}

func (s *Services) recordPurchaseAccounting(purchase *models.Purchase) {
	entry := models.AccountingEntry{
		EntryNumber: util.GenerateEntryNumber(), EntryType: models.EntryPurchase,
		Description: "Purchase " + purchase.InvoiceNumber, DebitAmount: purchase.GrandTotal,
		CreditAmount: purchase.GrandTotal, EntryDate: now(), ReferenceID: &purchase.ID,
		ReferenceType: "PURCHASE", CreatedAt: now(),
	}
	s.DB.Create(&entry)
}
