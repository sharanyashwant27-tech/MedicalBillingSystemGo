package services

import (
	"math"
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

func (s *Services) CreatePurchase(req models.PurchaseRequest, username string) (*models.Purchase, error) {
	pdate, err := time.Parse("2006-01-02", req.PurchaseDate)
	if err != nil {
		return nil, badRequest("Invalid purchase date")
	}

	purchase := models.Purchase{
		InvoiceNumber: req.InvoiceNumber,
		SupplierID:    req.SupplierID,
		PurchaseDate:  pdate,
		CreatedAt:     now(),
	}

	var total, gst, grand float64
	for _, itemReq := range req.Items {
		med, err := s.findMedicine(itemReq.MedicineID)
		if err != nil {
			return nil, err
		}
		lineGST := itemReq.GSTAmount
		subtotal := float64(itemReq.Quantity)*itemReq.PurchasePrice + lineGST
		exp, _ := util.ParseDate(derefStr(itemReq.ExpiryDate))
		purchase.Items = append(purchase.Items, models.PurchaseItem{
			MedicineID: itemReq.MedicineID, Quantity: itemReq.Quantity,
			PurchasePrice: itemReq.PurchasePrice, GSTAmount: lineGST, Subtotal: subtotal,
			ExpiryDate: exp, BatchNumber: itemReq.BatchNumber,
		})
		total += float64(itemReq.Quantity) * itemReq.PurchasePrice
		gst += lineGST
		grand += subtotal

		if err := s.AdjustStock(med.ID, itemReq.Quantity); err != nil {
			return nil, err
		}
		med.PurchasePrice = itemReq.PurchasePrice
		if exp != nil {
			med.ExpiryDate = exp
		}
		if itemReq.BatchNumber != "" {
			med.BatchNumber = itemReq.BatchNumber
		}
		s.DB.Save(med)
	}

	purchase.TotalAmount = total
	purchase.GSTAmount = gst
	purchase.GrandTotal = grand

	if err := s.DB.Create(&purchase).Error; err != nil {
		return nil, err
	}
	id := purchase.ID
	s.LogAudit("CREATE", "Purchase", &id, username, "Created purchase: "+purchase.InvoiceNumber, "")
	s.recordPurchaseAccounting(&purchase)
	s.DB.Preload("Supplier").Preload("Items.Medicine").First(&purchase, purchase.ID)
	return &purchase, nil
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
