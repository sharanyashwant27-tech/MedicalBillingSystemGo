package models

import (
	"time"

	"gorm.io/gorm"
)

type RoleType string

const (
	RoleAdmin      RoleType = "ROLE_ADMIN"
	RolePharmacist RoleType = "ROLE_PHARMACIST"
	RoleCashier    RoleType = "ROLE_CASHIER"
)

type Gender string

const (
	GenderMale   Gender = "MALE"
	GenderFemale Gender = "FEMALE"
	GenderOther  Gender = "OTHER"
)

type PaymentMode string

const (
	PaymentCash   PaymentMode = "CASH"
	PaymentCard   PaymentMode = "CARD"
	PaymentUPI    PaymentMode = "UPI"
	PaymentCredit PaymentMode = "CREDIT"
)

type MedicineStatus string

const (
	MedicineActive       MedicineStatus = "ACTIVE"
	MedicineInactive     MedicineStatus = "INACTIVE"
	MedicineDiscontinued MedicineStatus = "DISCONTINUED"
)

type ReturnType string

const (
	ReturnSales    ReturnType = "SALES_RETURN"
	ReturnPurchase ReturnType = "PURCHASE_RETURN"
)

type OrderStatus string

const (
	OrderPending    OrderStatus = "PENDING"
	OrderConfirmed  OrderStatus = "CONFIRMED"
	OrderProcessing OrderStatus = "PROCESSING"
	OrderReady      OrderStatus = "READY"
	OrderDelivered  OrderStatus = "DELIVERED"
	OrderCancelled  OrderStatus = "CANCELLED"
)

type AccountingEntryType string

const (
	EntrySale      AccountingEntryType = "SALE"
	EntryPurchase  AccountingEntryType = "PURCHASE"
	EntryExpense   AccountingEntryType = "EXPENSE"
	EntryPayment   AccountingEntryType = "PAYMENT"
	EntryRefund    AccountingEntryType = "REFUND"
)

type NotificationType string

const (
	NotifySMS      NotificationType = "SMS"
	NotifyWhatsApp NotificationType = "WHATSAPP"
	NotifyEmail    NotificationType = "EMAIL"
)

type NotificationStatus string

const (
	NotifySent    NotificationStatus = "SENT"
	NotifyFailed  NotificationStatus = "FAILED"
	NotifyPending NotificationStatus = "PENDING"
)

type Role struct {
	ID   uint     `gorm:"primaryKey" json:"id"`
	Name RoleType `gorm:"uniqueIndex;size:30;not null" json:"name"`
}

type Branch struct {
	ID         uint      `gorm:"primaryKey" json:"id"`
	BranchCode string    `gorm:"uniqueIndex;size:20;not null" json:"branchCode"`
	BranchName string    `gorm:"size:150;not null" json:"branchName"`
	Address    string    `gorm:"size:500" json:"address"`
	Phone      string    `gorm:"size:15" json:"phone"`
	Email      string    `gorm:"size:100" json:"email"`
	City       string    `gorm:"size:50" json:"city"`
	State      string    `gorm:"size:50" json:"state"`
	PinCode    string    `gorm:"size:10" json:"pinCode"`
	Active     bool      `gorm:"not null;default:true" json:"active"`
	CreatedAt  time.Time `json:"createdAt"`
	UpdatedAt  time.Time `json:"updatedAt"`
}

type User struct {
	ID               uint      `gorm:"primaryKey" json:"id"`
	Username         string    `gorm:"uniqueIndex;size:50;not null" json:"username"`
	Password         string    `gorm:"not null" json:"-"`
	FullName         string    `gorm:"size:100;not null" json:"fullName"`
	Email            string    `gorm:"size:100" json:"email"`
	Phone            string    `gorm:"size:15" json:"phone"`
	BranchID         *uint     `json:"branchId"`
	Branch           *Branch   `json:"branch,omitempty"`
	Roles            []Role    `gorm:"many2many:user_roles" json:"roles"`
	Enabled          bool      `gorm:"not null;default:true" json:"enabled"`
	AccountNonLocked bool      `gorm:"not null;default:true" json:"accountNonLocked"`
	CreatedAt        time.Time `json:"createdAt"`
	UpdatedAt        time.Time `json:"updatedAt"`
}

func (u User) HasRole(role RoleType) bool {
	for _, r := range u.Roles {
		if r.Name == role {
			return true
		}
	}
	return false
}

func (u User) RoleNames() []string {
	names := make([]string, len(u.Roles))
	for i, r := range u.Roles {
		names[i] = string(r.Name)
	}
	return names
}

type Category struct {
	ID          uint      `gorm:"primaryKey" json:"id"`
	Name        string    `gorm:"uniqueIndex;size:100;not null" json:"name"`
	Description string    `gorm:"size:500" json:"description"`
	CreatedAt   time.Time `json:"createdAt"`
	UpdatedAt   time.Time `json:"updatedAt"`
}

type Supplier struct {
	ID                 uint      `gorm:"primaryKey" json:"id"`
	SupplierName       string    `gorm:"size:150;not null" json:"supplierName"`
	GSTNumber          string    `gorm:"size:20" json:"gstNumber"`
	ContactPerson      string    `gorm:"size:100" json:"contactPerson"`
	Phone              string    `gorm:"size:15" json:"phone"`
	Email              string    `gorm:"size:100" json:"email"`
	Address            string    `gorm:"size:500" json:"address"`
	State              string    `gorm:"size:50" json:"state"`
	PinCode            string    `gorm:"size:10" json:"pinCode"`
	OutstandingBalance float64   `gorm:"not null;default:0" json:"outstandingBalance"`
	CreatedAt          time.Time `json:"createdAt"`
	UpdatedAt          time.Time `json:"updatedAt"`
}

type Customer struct {
	ID            uint      `gorm:"primaryKey" json:"id"`
	CustomerName  string    `gorm:"size:150;not null" json:"customerName"`
	Phone         string    `gorm:"size:15" json:"phone"`
	Email         string    `gorm:"size:100" json:"email"`
	Address       string    `gorm:"size:500" json:"address"`
	Age           *int      `json:"age"`
	Gender        Gender    `gorm:"size:10" json:"gender"`
	DoctorName    string    `gorm:"size:100" json:"doctorName"`
	GSTNumber     string    `gorm:"size:20" json:"gstNumber"`
	LoyaltyPoints int       `gorm:"not null;default:0" json:"loyaltyPoints"`
	BranchID      *uint     `json:"branchId"`
	Branch        *Branch   `json:"branch,omitempty"`
	CreatedAt     time.Time `json:"createdAt"`
	UpdatedAt     time.Time `json:"updatedAt"`
}

type Medicine struct {
	ID               uint           `gorm:"primaryKey" json:"id"`
	MedicineCode     string         `gorm:"uniqueIndex;size:50;not null" json:"medicineCode"`
	MedicineName     string         `gorm:"size:200;not null" json:"medicineName"`
	CategoryID       *uint          `json:"categoryId"`
	Category         *Category      `json:"category,omitempty"`
	Brand            string         `gorm:"size:100" json:"brand"`
	BatchNumber      string         `gorm:"size:50" json:"batchNumber"`
	ExpiryDate       *time.Time     `json:"expiryDate"`
	ManufacturingDate *time.Time    `json:"manufacturingDate"`
	HSNCode          string         `gorm:"size:20" json:"hsnCode"`
	GSTPercent       float64        `gorm:"default:0" json:"gstPercent"`
	PurchasePrice    float64        `gorm:"not null" json:"purchasePrice"`
	SellingPrice     float64        `gorm:"not null" json:"sellingPrice"`
	MRP              *float64       `json:"mrp"`
	DiscountPercent  float64        `gorm:"default:0" json:"discountPercent"`
	RackNumber       string         `gorm:"size:20" json:"rackNumber"`
	MinimumStock     int            `gorm:"not null;default:10" json:"minimumStock"`
	CurrentStock     int            `gorm:"not null;default:0" json:"currentStock"`
	Barcode          string         `gorm:"size:100" json:"barcode"`
	SupplierID       *uint          `json:"supplierId"`
	Supplier         *Supplier      `json:"supplier,omitempty"`
	Status           MedicineStatus `gorm:"size:20;not null;default:ACTIVE" json:"status"`
	CreatedAt        time.Time      `json:"createdAt"`
	UpdatedAt        time.Time      `json:"updatedAt"`
}

type Purchase struct {
	ID            uint           `gorm:"primaryKey" json:"id"`
	InvoiceNumber string         `gorm:"uniqueIndex;size:50;not null" json:"invoiceNumber"`
	SupplierID    uint           `json:"supplierId"`
	Supplier      *Supplier      `json:"supplier,omitempty"`
	PurchaseDate  time.Time      `json:"purchaseDate"`
	TotalAmount   float64        `gorm:"not null;default:0" json:"totalAmount"`
	GSTAmount     float64        `gorm:"not null;default:0" json:"gstAmount"`
	GrandTotal    float64        `gorm:"not null;default:0" json:"grandTotal"`
	Items         []PurchaseItem `json:"items"`
	CreatedAt     time.Time      `json:"createdAt"`
	UpdatedAt     time.Time      `json:"updatedAt"`
}

type PurchaseItem struct {
	ID            uint      `gorm:"primaryKey" json:"id"`
	PurchaseID    uint      `json:"purchaseId"`
	MedicineID    uint      `json:"medicineId"`
	Medicine      *Medicine `json:"medicine,omitempty"`
	Quantity      int       `gorm:"not null" json:"quantity"`
	PurchasePrice float64   `gorm:"not null" json:"purchasePrice"`
	GSTAmount     float64   `gorm:"not null;default:0" json:"gstAmount"`
	Subtotal      float64   `gorm:"not null" json:"subtotal"`
	ExpiryDate    *time.Time `json:"expiryDate"`
	BatchNumber   string    `gorm:"size:50" json:"batchNumber"`
}

type Sale struct {
	ID             uint        `gorm:"primaryKey" json:"id"`
	BillNumber     string      `gorm:"uniqueIndex;size:50;not null" json:"billNumber"`
	CustomerID     *uint       `json:"customerId"`
	Customer       *Customer   `json:"customer,omitempty"`
	UserID         uint        `json:"userId"`
	CreatedBy      *User       `gorm:"foreignKey:UserID" json:"createdBy,omitempty"`
	Subtotal       float64     `gorm:"not null;default:0" json:"subtotal"`
	DiscountAmount float64     `gorm:"not null;default:0" json:"discountAmount"`
	GSTAmount      float64     `gorm:"not null;default:0" json:"gstAmount"`
	GrandTotal     float64     `gorm:"not null;default:0" json:"grandTotal"`
	AmountPaid     float64     `gorm:"not null;default:0" json:"amountPaid"`
	ReturnAmount   float64     `gorm:"not null;default:0" json:"returnAmount"`
	PaymentMode    PaymentMode `gorm:"size:10;not null" json:"paymentMode"`
	BranchID       *uint       `json:"branchId"`
	Branch         *Branch     `json:"branch,omitempty"`
	Items          []SaleItem  `json:"items"`
	SaleDate       time.Time   `json:"saleDate"`
	CreatedAt      time.Time   `json:"createdAt"`
}

type SaleItem struct {
	ID              uint      `gorm:"primaryKey" json:"id"`
	SaleID          uint      `json:"saleId"`
	MedicineID      uint      `json:"medicineId"`
	Medicine        *Medicine `json:"medicine,omitempty"`
	BatchNumber     string    `gorm:"size:50" json:"batchNumber"`
	Quantity        int       `gorm:"not null" json:"quantity"`
	UnitPrice       float64   `gorm:"not null" json:"unitPrice"`
	DiscountPercent float64   `gorm:"not null;default:0" json:"discountPercent"`
	GSTAmount       float64   `gorm:"not null;default:0" json:"gstAmount"`
	Subtotal        float64   `gorm:"not null" json:"subtotal"`
}

type Payment struct {
	ID          uint        `gorm:"primaryKey" json:"id"`
	SaleID      *uint       `json:"saleId"`
	SupplierID  *uint       `json:"supplierId"`
	Amount      float64     `gorm:"not null" json:"amount"`
	PaymentMode PaymentMode `gorm:"size:10;not null" json:"paymentMode"`
	Pending     bool        `gorm:"not null;default:false" json:"pending"`
	Notes       string      `gorm:"size:500" json:"notes"`
	PaymentDate time.Time   `json:"paymentDate"`
}

type MedicineReturn struct {
	ID           uint       `gorm:"primaryKey" json:"id"`
	ReturnNumber string     `gorm:"uniqueIndex;size:50;not null" json:"returnNumber"`
	ReturnType   ReturnType `gorm:"size:20;not null" json:"returnType"`
	MedicineID   uint       `json:"medicineId"`
	Medicine     *Medicine  `json:"medicine,omitempty"`
	SaleID       *uint      `json:"saleId"`
	Sale         *Sale      `json:"sale,omitempty"`
	PurchaseID   *uint      `json:"purchaseId"`
	Purchase     *Purchase  `json:"purchase,omitempty"`
	Quantity     int        `gorm:"not null" json:"quantity"`
	RefundAmount float64    `gorm:"not null" json:"refundAmount"`
	Reason       string     `gorm:"size:500;not null" json:"reason"`
	UserID       uint       `json:"userId"`
	ProcessedBy  *User      `gorm:"foreignKey:UserID" json:"processedBy,omitempty"`
	ReturnDate   time.Time  `json:"returnDate"`
}

func (MedicineReturn) TableName() string { return "returns" }

type Prescription struct {
	ID         uint      `gorm:"primaryKey" json:"id"`
	CustomerID uint      `json:"customerId"`
	Customer   *Customer `json:"customer,omitempty"`
	FileName   string    `gorm:"size:500;not null" json:"fileName"`
	FilePath   string    `gorm:"size:500;not null" json:"filePath"`
	Notes      string    `gorm:"size:500" json:"notes"`
	UploadedBy *uint     `json:"uploadedBy"`
	Uploader   *User     `gorm:"foreignKey:UploadedBy" json:"uploader,omitempty"`
	UploadedAt time.Time `json:"uploadedAt"`
}

type OnlineOrder struct {
	ID              uint             `gorm:"primaryKey" json:"id"`
	OrderNumber     string           `gorm:"uniqueIndex;size:50;not null" json:"orderNumber"`
	CustomerID      uint             `json:"customerId"`
	Customer        *Customer        `json:"customer,omitempty"`
	BranchID        *uint            `json:"branchId"`
	Branch          *Branch          `json:"branch,omitempty"`
	Status          OrderStatus      `gorm:"size:20;not null;default:PENDING" json:"status"`
	TotalAmount     float64          `gorm:"not null;default:0" json:"totalAmount"`
	DeliveryAddress string           `gorm:"size:500" json:"deliveryAddress"`
	ContactPhone    string           `gorm:"size:15" json:"contactPhone"`
	Notes           string           `gorm:"size:500" json:"notes"`
	Items           []OnlineOrderItem `json:"items"`
	OrderDate       time.Time        `json:"orderDate"`
	UpdatedAt       time.Time        `json:"updatedAt"`
}

type OnlineOrderItem struct {
	ID            uint      `gorm:"primaryKey" json:"id"`
	OnlineOrderID uint      `json:"onlineOrderId"`
	MedicineID    uint      `json:"medicineId"`
	Medicine      *Medicine `json:"medicine,omitempty"`
	Quantity      int       `gorm:"not null" json:"quantity"`
	UnitPrice     float64   `gorm:"not null" json:"unitPrice"`
	Subtotal      float64   `gorm:"not null" json:"subtotal"`
}

type LoyaltyTransaction struct {
	ID              uint      `gorm:"primaryKey" json:"id"`
	CustomerID      uint      `json:"customerId"`
	Customer        *Customer `json:"customer,omitempty"`
	SaleID          *uint     `json:"saleId"`
	Points          int       `gorm:"not null" json:"points"`
	TransactionType string    `gorm:"size:30;not null" json:"transactionType"`
	Description     string    `gorm:"size:500" json:"description"`
	CreatedAt       time.Time `json:"createdAt"`
}

type ShopSettings struct {
	ID                uint      `gorm:"primaryKey" json:"id"`
	ShopName          string    `gorm:"size:200;not null;default:Medical Shop" json:"shopName"`
	LogoPath          string    `gorm:"size:500" json:"logoPath"`
	GSTNumber         string    `gorm:"size:20" json:"gstNumber"`
	Address           string    `gorm:"size:500" json:"address"`
	Phone             string    `gorm:"size:15" json:"phone"`
	Email             string    `gorm:"size:100" json:"email"`
	InvoiceFooter     string    `gorm:"size:1000" json:"invoiceFooter"`
	DefaultGSTPercent float64   `gorm:"default:12" json:"defaultGstPercent"`
	CreatedAt         time.Time `json:"createdAt"`
	UpdatedAt         time.Time `json:"updatedAt"`
}

type AccountingEntry struct {
	ID            uint                `gorm:"primaryKey" json:"id"`
	EntryNumber   string              `gorm:"uniqueIndex;size:50;not null" json:"entryNumber"`
	EntryType     AccountingEntryType `gorm:"size:20;not null" json:"entryType"`
	BranchID      *uint               `json:"branchId"`
	Branch        *Branch             `json:"branch,omitempty"`
	Description   string              `gorm:"size:500" json:"description"`
	DebitAmount   float64             `gorm:"not null;default:0" json:"debitAmount"`
	CreditAmount  float64             `gorm:"not null;default:0" json:"creditAmount"`
	EntryDate     time.Time           `json:"entryDate"`
	ReferenceID   *uint               `json:"referenceId"`
	ReferenceType string              `gorm:"size:50" json:"referenceType"`
	Synced        bool                `gorm:"not null;default:false" json:"synced"`
	CreatedAt     time.Time           `json:"createdAt"`
}

type AuditLog struct {
	ID         uint      `gorm:"primaryKey" json:"id"`
	Action     string    `gorm:"size:100;not null" json:"action"`
	EntityType string    `gorm:"size:100;not null" json:"entityType"`
	EntityID   *uint     `json:"entityId"`
	Username   string    `gorm:"size:100" json:"username"`
	Details    string    `gorm:"size:1000" json:"details"`
	IPAddress  string    `gorm:"size:50" json:"ipAddress"`
	Timestamp  time.Time `json:"timestamp"`
}

type NotificationLog struct {
	ID           uint               `gorm:"primaryKey" json:"id"`
	Type         NotificationType   `gorm:"size:20;not null" json:"type"`
	Recipient    string             `gorm:"size:100;not null" json:"recipient"`
	Message      string             `gorm:"size:1000" json:"message"`
	Status       NotificationStatus `gorm:"size:20;not null" json:"status"`
	ReferenceID  string             `gorm:"size:100" json:"referenceId"`
	ErrorMessage string             `gorm:"size:500" json:"errorMessage"`
	SentAt       time.Time          `json:"sentAt"`
}

func AutoMigrate(db *gorm.DB) error {
	return db.AutoMigrate(
		&Role{}, &Branch{}, &User{}, &Category{}, &Supplier{}, &Customer{},
		&Medicine{}, &Purchase{}, &PurchaseItem{}, &Sale{}, &SaleItem{},
		&Payment{}, &MedicineReturn{}, &Prescription{}, &OnlineOrder{},
		&OnlineOrderItem{}, &LoyaltyTransaction{}, &ShopSettings{},
		&AccountingEntry{}, &AuditLog{}, &NotificationLog{},
	)
}
