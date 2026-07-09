package com.medicalbilling.controller;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class ApiController {

    private final CategoryService categoryService;
    private final SupplierService supplierService;
    private final CustomerService customerService;
    private final MedicineService medicineService;
    private final PurchaseService purchaseService;
    private final SaleService saleService;
    private final DashboardService dashboardService;
    private final InventoryService inventoryService;
    private final ReportService reportService;
    private final ReturnService returnService;
    private final PrescriptionService prescriptionService;
    private final SettingsService settingsService;
    private final BackupService backupService;
    private final ExportService exportService;
    private final EmailService emailService;

    // Categories
    @GetMapping("/api/categories")
    public ResponseEntity<List<DtoModels.CategoryResponse>> getCategories(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(categoryService.getAll(search));
    }

    @PostMapping("/api/categories")
    public ResponseEntity<DtoModels.CategoryResponse> createCategory(@Valid @RequestBody DtoModels.CategoryRequest request,
                                                                     @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(categoryService.create(request, user.getUsername()));
    }

    @PutMapping("/api/categories/{id}")
    public ResponseEntity<DtoModels.CategoryResponse> updateCategory(@PathVariable Long id,
                                                                     @Valid @RequestBody DtoModels.CategoryRequest request,
                                                                     @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(categoryService.update(id, request, user.getUsername()));
    }

    @DeleteMapping("/api/categories/{id}")
    public ResponseEntity<Map<String, String>> deleteCategory(@PathVariable Long id,
                                                               @AuthenticationPrincipal UserDetails user) {
        categoryService.delete(id, user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Category deleted"));
    }

    // Suppliers
    @GetMapping("/api/suppliers")
    public ResponseEntity<List<DtoModels.SupplierResponse>> getSuppliers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(supplierService.getAll(search));
    }

    @GetMapping("/api/suppliers/{id}")
    public ResponseEntity<DtoModels.SupplierResponse> getSupplier(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getById(id));
    }

    @PostMapping("/api/suppliers")
    public ResponseEntity<DtoModels.SupplierResponse> createSupplier(@Valid @RequestBody DtoModels.SupplierRequest request,
                                                                     @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(supplierService.create(request, user.getUsername()));
    }

    @PutMapping("/api/suppliers/{id}")
    public ResponseEntity<DtoModels.SupplierResponse> updateSupplier(@PathVariable Long id,
                                                                     @Valid @RequestBody DtoModels.SupplierRequest request,
                                                                     @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(supplierService.update(id, request, user.getUsername()));
    }

    @DeleteMapping("/api/suppliers/{id}")
    public ResponseEntity<Map<String, String>> deleteSupplier(@PathVariable Long id,
                                                                @AuthenticationPrincipal UserDetails user) {
        supplierService.delete(id, user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Supplier deleted"));
    }

    // Customers
    @GetMapping("/api/customers")
    public ResponseEntity<List<DtoModels.CustomerResponse>> getCustomers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(customerService.getAll(search));
    }

    @GetMapping("/api/customers/{id}/history")
    public ResponseEntity<Map<String, Object>> getCustomerHistory(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerHistory(id));
    }

    @PostMapping("/api/customers")
    public ResponseEntity<DtoModels.CustomerResponse> createCustomer(@Valid @RequestBody DtoModels.CustomerRequest request,
                                                                     @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(customerService.create(request, user.getUsername()));
    }

    @PutMapping("/api/customers/{id}")
    public ResponseEntity<DtoModels.CustomerResponse> updateCustomer(@PathVariable Long id,
                                                                     @Valid @RequestBody DtoModels.CustomerRequest request,
                                                                     @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(customerService.update(id, request, user.getUsername()));
    }

    @DeleteMapping("/api/customers/{id}")
    public ResponseEntity<Map<String, String>> deleteCustomer(@PathVariable Long id,
                                                                @AuthenticationPrincipal UserDetails user) {
        customerService.delete(id, user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Customer deleted"));
    }

    // Medicines
    @GetMapping("/api/medicines")
    public ResponseEntity<List<DtoModels.MedicineResponse>> getMedicines(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(medicineService.getAll(search));
    }

    @GetMapping("/api/medicines/search")
    public ResponseEntity<List<DtoModels.MedicineResponse>> searchMedicines(@RequestParam String q) {
        return ResponseEntity.ok(medicineService.search(q));
    }

    @GetMapping("/api/medicines/{id}")
    public ResponseEntity<DtoModels.MedicineResponse> getMedicine(@PathVariable Long id) {
        return ResponseEntity.ok(medicineService.getById(id));
    }

    @PostMapping("/api/medicines")
    public ResponseEntity<DtoModels.MedicineResponse> createMedicine(@Valid @RequestBody DtoModels.MedicineRequest request,
                                                                     @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(medicineService.create(request, user.getUsername()));
    }

    @PutMapping("/api/medicines/{id}")
    public ResponseEntity<DtoModels.MedicineResponse> updateMedicine(@PathVariable Long id,
                                                                     @Valid @RequestBody DtoModels.MedicineRequest request,
                                                                     @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(medicineService.update(id, request, user.getUsername()));
    }

    @DeleteMapping("/api/medicines/{id}")
    public ResponseEntity<Map<String, String>> deleteMedicine(@PathVariable Long id,
                                                                @AuthenticationPrincipal UserDetails user) {
        medicineService.delete(id, user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Medicine deactivated"));
    }

    // Purchases
    @GetMapping("/api/purchases")
    public ResponseEntity<?> getPurchases() {
        return ResponseEntity.ok(purchaseService.getAll());
    }

    @PostMapping("/api/purchases")
    public ResponseEntity<?> createPurchase(@Valid @RequestBody DtoModels.PurchaseRequest request,
                                            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(purchaseService.create(request, user.getUsername()));
    }

    // Sales
    @GetMapping("/api/sales")
    public ResponseEntity<?> getSales() {
        return ResponseEntity.ok(saleService.getAll());
    }

    @GetMapping("/api/sales/recent")
    public ResponseEntity<?> getRecentSales() {
        return ResponseEntity.ok(saleService.getRecentBills());
    }

    @GetMapping("/api/sales/{id}")
    public ResponseEntity<?> getSale(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getById(id));
    }

    @PostMapping("/api/sales")
    public ResponseEntity<?> createSale(@Valid @RequestBody DtoModels.SaleRequest request,
                                        @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(saleService.create(request, user.getUsername()));
    }

    @GetMapping("/api/sales/{id}/pdf")
    public ResponseEntity<byte[]> downloadSalePdf(@PathVariable Long id) throws Exception {
        var sale = saleService.getById(id);
        byte[] pdf = exportService.exportToPdf(sale);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + sale.getBillNumber() + ".pdf")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_PDF))
                .body(pdf);
    }

    @PostMapping("/api/sales/{id}/email")
    public ResponseEntity<Map<String, String>> emailSale(@PathVariable Long id, @RequestBody Map<String, String> body) {
        var sale = saleService.getById(id);
        emailService.sendBillEmail(sale, body.get("email"));
        return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
    }

    // Dashboard
    @GetMapping("/api/dashboard")
    public ResponseEntity<DtoModels.DashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboardData());
    }

    // Inventory
    @GetMapping("/api/inventory")
    public ResponseEntity<DtoModels.InventorySummary> getInventory(@RequestParam(required = false) String filter) {
        return ResponseEntity.ok(inventoryService.getInventorySummary(filter));
    }

    // Returns
    @GetMapping("/api/returns")
    public ResponseEntity<?> getReturns() {
        return ResponseEntity.ok(returnService.getAll());
    }

    @PostMapping("/api/returns")
    public ResponseEntity<?> createReturn(@Valid @RequestBody DtoModels.ReturnRequest request,
                                          @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(returnService.processReturn(request, user.getUsername()));
    }

    // Prescriptions
    @GetMapping("/api/prescriptions/customer/{customerId}")
    public ResponseEntity<?> getPrescriptions(@PathVariable Long customerId) {
        return ResponseEntity.ok(prescriptionService.getByCustomer(customerId));
    }

    @PostMapping("/api/prescriptions")
    public ResponseEntity<?> uploadPrescription(@RequestParam Long customerId,
                                                @RequestParam MultipartFile file,
                                                @RequestParam(required = false) String notes,
                                                @AuthenticationPrincipal UserDetails user) throws Exception {
        return ResponseEntity.ok(prescriptionService.upload(customerId, file, notes, user.getUsername()));
    }

    // Settings
    @GetMapping("/api/settings")
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping("/api/settings")
    public ResponseEntity<?> updateSettings(@RequestBody DtoModels.ShopSettingsRequest request,
                                            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(settingsService.updateSettings(request, user.getUsername()));
    }

    @PostMapping("/api/settings/logo")
    public ResponseEntity<?> uploadLogo(@RequestParam MultipartFile file,
                                        @AuthenticationPrincipal UserDetails user) throws Exception {
        return ResponseEntity.ok(settingsService.uploadLogo(file, user.getUsername()));
    }

    @PostMapping("/api/settings/backup")
    public ResponseEntity<Map<String, String>> backup(@AuthenticationPrincipal UserDetails user) throws Exception {
        String path = backupService.createBackup();
        return ResponseEntity.ok(Map.of("message", "Backup created", "path", path));
    }

    // Reports
    @GetMapping("/api/reports")
    public ResponseEntity<Map<String, Object>> getReport(
            @RequestParam String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.generateReport(type, startDate, endDate));
    }

    @GetMapping("/api/reports/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam String type,
            @RequestParam String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        Map<String, Object> report = reportService.generateReport(type, startDate, endDate);
        byte[] data;
        MediaType mediaType;
        String extension;
        if ("excel".equalsIgnoreCase(format)) {
            data = exportService.exportReportToExcel(report);
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            extension = "xlsx";
        } else if ("csv".equalsIgnoreCase(format)) {
            data = exportService.exportReportToCsv(report);
            mediaType = MediaType.TEXT_PLAIN;
            extension = "csv";
        } else {
            data = exportService.exportReportToCsv(report);
            mediaType = MediaType.TEXT_PLAIN;
            extension = "csv";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report." + extension)
                .contentType(Objects.requireNonNull(mediaType))
                .body(data);
    }
}
