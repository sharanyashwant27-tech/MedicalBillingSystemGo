package seed

import (
	"log"
	"time"

	"github.com/medicalbilling/medical-billing-system/internal/auth"
	"github.com/medicalbilling/medical-billing-system/internal/config"
	"github.com/medicalbilling/medical-billing-system/internal/models"
	"github.com/medicalbilling/medical-billing-system/internal/util"
	"gorm.io/gorm"
)

func Run(db *gorm.DB, cfg config.Config) {
	seedRoles(db)
	branch := seedBranch(db)
	seedUsers(db, cfg, branch)
	seedCategories(db)
	seedSuppliers(db)
	seedMedicines(db)
	seedCustomers(db)
	seedShopSettings(db)
	log.Println("Database initialization completed")
}

func seedRoles(db *gorm.DB) {
	for _, name := range []models.RoleType{models.RoleAdmin, models.RolePharmacist, models.RoleCashier} {
		var role models.Role
		if err := db.Where("name = ?", name).FirstOrCreate(&role, models.Role{Name: name}).Error; err != nil {
			log.Printf("seed role %s: %v", name, err)
		}
	}
}

func seedBranch(db *gorm.DB) models.Branch {
	var branch models.Branch
	db.Where("branch_code = ?", "MAIN").FirstOrCreate(&branch, models.Branch{
		BranchCode: "MAIN",
		BranchName: "Main Branch",
		Address:    "45 Main Street, Pune, Maharashtra",
		Phone:      "020-12345678",
		Email:      "main@healthcaremedical.com",
		City:       "Pune",
		State:      "Maharashtra",
		PinCode:    "411001",
		Active:     true,
		CreatedAt:  time.Now(),
	})
	return branch
}

func seedUsers(db *gorm.DB, cfg config.Config, branch models.Branch) {
	users := []struct {
		username string
		password string
		fullName string
		email    string
		phone    string
		role     models.RoleType
	}{
		{"admin", cfg.AdminPassword, "System Administrator", "admin@medicalshop.com", "9876543210", models.RoleAdmin},
		{"pharmacist", cfg.PharmacistPassword, "John Pharmacist", "pharmacist@medicalshop.com", "", models.RolePharmacist},
		{"cashier", cfg.CashierPassword, "Jane Cashier", "cashier@medicalshop.com", "", models.RoleCashier},
	}

	for _, u := range users {
		var role models.Role
		db.Where("name = ?", u.role).First(&role)
		hash, _ := auth.HashPassword(u.password)

		var user models.User
		result := db.Preload("Roles").Where("username = ?", u.username).First(&user)
		if result.Error == gorm.ErrRecordNotFound {
			user = models.User{
				Username:         u.username,
				Password:         hash,
				FullName:         u.fullName,
				Email:            u.email,
				Phone:            u.phone,
				BranchID:         &branch.ID,
				Roles:            []models.Role{role},
				Enabled:          true,
				AccountNonLocked: true,
				CreatedAt:        time.Now(),
			}
			db.Create(&user)
		} else {
			user.Password = hash
			user.FullName = u.fullName
			user.Email = u.email
			user.Phone = u.phone
			user.BranchID = &branch.ID
			user.Enabled = true
			user.AccountNonLocked = true
			db.Model(&user).Association("Roles").Replace([]models.Role{role})
			db.Save(&user)
		}
	}
}

func seedCategories(db *gorm.DB) {
	names := []string{"Tablets", "Syrups", "Injections", "Ointments", "Capsules", "Medical Devices"}
	for _, name := range names {
		var cat models.Category
		db.Where("name = ?", name).FirstOrCreate(&cat, models.Category{
			Name:        name,
			Description: name + " category",
			CreatedAt:   time.Now(),
		})
	}
}

func seedSuppliers(db *gorm.DB) {
	var count int64
	db.Model(&models.Supplier{}).Count(&count)
	if count > 0 {
		return
	}
	db.Create(&models.Supplier{
		SupplierName:  "MediSupply Corp",
		GSTNumber:     "27AABCU9603R1ZM",
		ContactPerson: "Rajesh Kumar",
		Phone:         "9123456780",
		Email:         "rajesh@medisupply.com",
		Address:       "123 Industrial Area, Mumbai",
		State:         "Maharashtra",
		PinCode:       "400001",
		CreatedAt:     time.Now(),
	})
}

func seedMedicines(db *gorm.DB) {
	var count int64
	db.Model(&models.Medicine{}).Count(&count)
	if count > 0 {
		return
	}

	var tablets models.Category
	db.Where("name = ?", "Tablets").First(&tablets)
	var supplier models.Supplier
	db.First(&supplier)

	exp1 := time.Now().AddDate(0, 12, 0)
	mfg1 := time.Now().AddDate(0, -2, 0)
	exp2 := time.Now().AddDate(0, 6, 0)
	mfg2 := time.Now().AddDate(0, -1, 0)
	mrp1 := 6.0
	mrp2 := 18.0

	medicines := []models.Medicine{
		{
			MedicineCode: "MED-001001", MedicineName: "Paracetamol 500mg", CategoryID: &tablets.ID,
			Brand: "Crocin", BatchNumber: "BATCH001", ExpiryDate: &exp1, ManufacturingDate: &mfg1,
			HSNCode: "3004", GSTPercent: 12, PurchasePrice: 2.5, SellingPrice: 5, MRP: &mrp1,
			RackNumber: "A1", MinimumStock: 50, CurrentStock: 200, Barcode: "8901234567890",
			SupplierID: &supplier.ID, Status: models.MedicineActive, CreatedAt: time.Now(),
		},
		{
			MedicineCode: "MED-001002", MedicineName: "Amoxicillin 250mg", CategoryID: &tablets.ID,
			Brand: "Mox", BatchNumber: "BATCH002", ExpiryDate: &exp2, ManufacturingDate: &mfg2,
			HSNCode: "3004", GSTPercent: 12, PurchasePrice: 8, SellingPrice: 15, MRP: &mrp2,
			RackNumber: "A2", MinimumStock: 30, CurrentStock: 5, Barcode: "8901234567891",
			SupplierID: &supplier.ID, Status: models.MedicineActive, CreatedAt: time.Now(),
		},
	}
	for _, m := range medicines {
		db.Create(&m)
	}
	_ = util.GenerateMedicineCode
}

func seedCustomers(db *gorm.DB) {
	var count int64
	db.Model(&models.Customer{}).Count(&count)
	if count > 0 {
		return
	}
	db.Create(&models.Customer{
		CustomerName: "Walk-in Customer",
		Phone:        "9999999999",
		CreatedAt:    time.Now(),
	})
}

func seedShopSettings(db *gorm.DB) {
	var count int64
	db.Model(&models.ShopSettings{}).Count(&count)
	if count > 0 {
		return
	}
	db.Create(&models.ShopSettings{
		ShopName:          "HealthCare Medical Store",
		GSTNumber:         "27AABCU9603R1ZM",
		Address:           "45 Main Street, Pune, Maharashtra - 411001",
		Phone:             "020-12345678",
		Email:             "info@healthcaremedical.com",
		InvoiceFooter:     "Thank you for your purchase! Get well soon.",
		DefaultGSTPercent: 12,
		CreatedAt:         time.Now(),
	})
}
