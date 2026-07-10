package models

import "time"

type ApiResponse struct {
	Success bool        `json:"success"`
	Message string      `json:"message,omitempty"`
	Data    interface{} `json:"data,omitempty"`
}

type LoginRequest struct {
	Username string `json:"username" binding:"required"`
	Password string `json:"password" binding:"required"`
}

type JwtResponse struct {
	Token    string   `json:"token"`
	Type     string   `json:"type"`
	ID       uint     `json:"id"`
	Username string   `json:"username"`
	FullName string   `json:"fullName"`
	Roles    []string `json:"roles"`
}

type CategoryRequest struct {
	Name        string `json:"name" binding:"required"`
	Description string `json:"description"`
}

type CategoryResponse struct {
	ID          uint   `json:"id"`
	Name        string `json:"name"`
	Description string `json:"description"`
}

type SupplierRequest struct {
	SupplierName  string `json:"supplierName" binding:"required"`
	GSTNumber     string `json:"gstNumber"`
	ContactPerson string `json:"contactPerson"`
	Phone         string `json:"phone"`
	Email         string `json:"email"`
	Address       string `json:"address"`
	State         string `json:"state"`
	PinCode       string `json:"pinCode"`
}

type SupplierResponse struct {
	ID                 uint    `json:"id"`
	SupplierName       string  `json:"supplierName"`
	GSTNumber          string  `json:"gstNumber"`
	ContactPerson      string  `json:"contactPerson"`
	Phone              string  `json:"phone"`
	Email              string  `json:"email"`
	Address            string  `json:"address"`
	State              string  `json:"state"`
	PinCode            string  `json:"pinCode"`
	OutstandingBalance float64 `json:"outstandingBalance"`
}

type CustomerRequest struct {
	CustomerName string `json:"customerName" binding:"required"`
	Phone        string `json:"phone"`
	Email        string `json:"email"`
	Address      string `json:"address"`
	Age          *int   `json:"age"`
	Gender       Gender `json:"gender"`
	DoctorName   string `json:"doctorName"`
	GSTNumber    string `json:"gstNumber"`
}

type CustomerResponse struct {
	ID            uint   `json:"id"`
	CustomerName  string `json:"customerName"`
	Phone         string `json:"phone"`
	Email         string `json:"email"`
	Address       string `json:"address"`
	Age           *int   `json:"age"`
	Gender        Gender `json:"gender"`
	DoctorName    string `json:"doctorName"`
	GSTNumber     string `json:"gstNumber"`
	LoyaltyPoints int    `json:"loyaltyPoints"`
}

type MedicineRequest struct {
	MedicineCode      string         `json:"medicineCode"`
	MedicineName      string         `json:"medicineName" binding:"required"`
	CategoryID        *uint          `json:"categoryId"`
	Brand             string         `json:"brand"`
	BatchNumber       string         `json:"batchNumber"`
	ExpiryDate        *string        `json:"expiryDate"`
	ManufacturingDate *string        `json:"manufacturingDate"`
	HSNCode           string         `json:"hsnCode"`
	GSTPercent        *float64       `json:"gstPercent"`
	PurchasePrice     float64        `json:"purchasePrice" binding:"required"`
	SellingPrice      float64        `json:"sellingPrice" binding:"required"`
	MRP               *float64       `json:"mrp"`
	DiscountPercent   *float64       `json:"discountPercent"`
	RackNumber        string         `json:"rackNumber"`
	MinimumStock      *int           `json:"minimumStock"`
	CurrentStock      *int           `json:"currentStock"`
	Barcode           string         `json:"barcode"`
	SupplierID        *uint          `json:"supplierId"`
	Status            MedicineStatus `json:"status"`
}

type MedicineResponse struct {
	ID                uint           `json:"id"`
	MedicineCode      string         `json:"medicineCode"`
	MedicineName      string         `json:"medicineName"`
	CategoryName      string         `json:"categoryName"`
	CategoryID        *uint          `json:"categoryId"`
	Brand             string         `json:"brand"`
	BatchNumber       string         `json:"batchNumber"`
	ExpiryDate        *time.Time     `json:"expiryDate"`
	ManufacturingDate *time.Time     `json:"manufacturingDate"`
	HSNCode           string         `json:"hsnCode"`
	GSTPercent        float64        `json:"gstPercent"`
	PurchasePrice     float64        `json:"purchasePrice"`
	SellingPrice      float64        `json:"sellingPrice"`
	MRP               *float64       `json:"mrp"`
	DiscountPercent   float64        `json:"discountPercent"`
	RackNumber        string         `json:"rackNumber"`
	MinimumStock      int            `json:"minimumStock"`
	CurrentStock      int            `json:"currentStock"`
	Barcode           string         `json:"barcode"`
	SupplierName      string         `json:"supplierName"`
	SupplierID        *uint          `json:"supplierId"`
	Status            MedicineStatus `json:"status"`
}

type PurchaseItemRequest struct {
	MedicineID    uint    `json:"medicineId" binding:"required"`
	Quantity      int     `json:"quantity" binding:"required,min=1"`
	PurchasePrice float64 `json:"purchasePrice" binding:"required"`
	GSTAmount     float64 `json:"gstAmount"`
	ExpiryDate    *string `json:"expiryDate"`
	BatchNumber   string  `json:"batchNumber"`
}

type PurchaseRequest struct {
	InvoiceNumber string                `json:"invoiceNumber"`
	SupplierID    uint                  `json:"supplierId" binding:"required"`
	PurchaseDate  string                `json:"purchaseDate" binding:"required"`
	Items         []PurchaseItemRequest `json:"items" binding:"required,min=1"`
}

type SaleItemRequest struct {
	MedicineID      uint     `json:"medicineId" binding:"required"`
	Quantity        int      `json:"quantity" binding:"required,min=1"`
	DiscountPercent *float64 `json:"discountPercent"`
	BatchNumber     string   `json:"batchNumber"`
}

type SaleRequest struct {
	CustomerID  *uint             `json:"customerId"`
	PaymentMode PaymentMode       `json:"paymentMode" binding:"required"`
	AmountPaid  float64           `json:"amountPaid" binding:"required"`
	Items       []SaleItemRequest `json:"items" binding:"required,min=1"`
}

type ReturnRequest struct {
	ReturnType ReturnType `json:"returnType" binding:"required"`
	MedicineID uint       `json:"medicineId" binding:"required"`
	SaleID     *uint      `json:"saleId"`
	PurchaseID *uint      `json:"purchaseId"`
	Quantity   int        `json:"quantity" binding:"required,min=1"`
	Reason     string     `json:"reason" binding:"required"`
}

type DashboardResponse struct {
	TodaySales         float64       `json:"todaySales"`
	TodayProfit        float64       `json:"todayProfit"`
	AvailableMedicines int64         `json:"availableMedicines"`
	LowStockMedicines  int64         `json:"lowStockMedicines"`
	NearExpiryMedicines int64        `json:"nearExpiryMedicines"`
	ExpiredMedicines   int64         `json:"expiredMedicines"`
	TotalCustomers     int64         `json:"totalCustomers"`
	TotalSuppliers     int64         `json:"totalSuppliers"`
	PendingPayments    int64         `json:"pendingPayments"`
	RecentBills        []SaleSummary `json:"recentBills"`
	Alerts             []AlertItem   `json:"alerts"`
}

type SaleSummary struct {
	ID           uint        `json:"id"`
	BillNumber   string      `json:"billNumber"`
	CustomerName string      `json:"customerName"`
	GrandTotal   float64     `json:"grandTotal"`
	SaleDate     time.Time   `json:"saleDate"`
	PaymentMode  PaymentMode `json:"paymentMode"`
}

type AlertItem struct {
	Type          string     `json:"type"`
	Message       string     `json:"message"`
	Severity      string     `json:"severity"`
	MedicineID    uint       `json:"medicineId"`
	MedicineCode  string     `json:"medicineCode"`
	MedicineName  string     `json:"medicineName"`
	CurrentStock  int        `json:"currentStock"`
	MinimumStock  int        `json:"minimumStock"`
	Shortage      int        `json:"shortage"`
	CategoryName  string     `json:"categoryName"`
	SupplierName  string     `json:"supplierName"`
	SupplierPhone string     `json:"supplierPhone"`
	BatchNumber   string     `json:"batchNumber"`
	ExpiryDate    *time.Time `json:"expiryDate"`
	RackNumber    string     `json:"rackNumber"`
	StockStatus   string     `json:"stockStatus"`
}

type InventorySummary struct {
	TotalMedicines int64              `json:"totalMedicines"`
	LowStock       int64              `json:"lowStock"`
	OutOfStock     int64              `json:"outOfStock"`
	NearExpiry     int64              `json:"nearExpiry"`
	Expired        int64              `json:"expired"`
	Valuation      float64            `json:"valuation"`
	Items          []MedicineResponse `json:"items"`
}

type ShopSettingsRequest struct {
	ShopName          string   `json:"shopName"`
	GSTNumber         string   `json:"gstNumber"`
	Address           string   `json:"address"`
	Phone             string   `json:"phone"`
	Email             string   `json:"email"`
	InvoiceFooter     string   `json:"invoiceFooter"`
	DefaultGSTPercent *float64 `json:"defaultGstPercent"`
}

type UserRequest struct {
	Username string     `json:"username" binding:"required"`
	Password string     `json:"password" binding:"required,min=6"`
	FullName string     `json:"fullName" binding:"required"`
	Email    string     `json:"email"`
	Phone    string     `json:"phone"`
	Roles    []RoleType `json:"roles" binding:"required"`
}

type UserResponse struct {
	ID               uint     `json:"id"`
	Username         string   `json:"username"`
	FullName         string   `json:"fullName"`
	Email            string   `json:"email"`
	Phone            string   `json:"phone"`
	Roles            []string `json:"roles"`
	Enabled          bool     `json:"enabled"`
	AccountNonLocked bool     `json:"accountNonLocked"`
}

type UserUpdateRequest struct {
	UserID   uint       `json:"userId" binding:"required"`
	Username string     `json:"username" binding:"required"`
	Password string     `json:"password"`
	FullName string     `json:"fullName" binding:"required"`
	Email    string     `json:"email"`
	Phone    string     `json:"phone"`
	Roles    []RoleType `json:"roles" binding:"required"`
}

type PageData struct {
	PageTitle  string
	ActivePage string
	Username   string
	Error      string
	Message    string
	IsAdmin    bool
}
