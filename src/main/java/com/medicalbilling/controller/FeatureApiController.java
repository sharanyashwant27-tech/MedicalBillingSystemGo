package com.medicalbilling.controller;

import com.medicalbilling.entity.*;
import com.medicalbilling.repository.NotificationLogRepository;
import com.medicalbilling.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FeatureApiController {

    private final BranchService branchService;
    private final LoyaltyService loyaltyService;
    private final OnlineOrderService onlineOrderService;
    private final ReorderSuggestionService reorderSuggestionService;
    private final AccountingIntegrationService accountingIntegrationService;
    private final SmsNotificationService smsNotificationService;
    private final WhatsAppService whatsAppService;
    private final AuditLogService auditLogService;
    private final BackupService backupService;
    private final SaleService saleService;
    private final MedicineService medicineService;
    private final NotificationLogRepository notificationLogRepository;
    private final LowStockNotificationService lowStockNotificationService;
    private final NearExpiryNotificationService nearExpiryNotificationService;

    // Branches
    @GetMapping("/branches")
    public ResponseEntity<List<Branch>> getBranches() {
        return ResponseEntity.ok(branchService.getAll());
    }

    @GetMapping("/branches/active")
    public ResponseEntity<List<Branch>> getActiveBranches() {
        return ResponseEntity.ok(branchService.getActive());
    }

    @PostMapping("/branches")
    public ResponseEntity<Branch> createBranch(@RequestBody Branch branch,
                                                @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(branchService.create(branch, user.getUsername()));
    }

    @PutMapping("/branches/{id}")
    public ResponseEntity<Branch> updateBranch(@PathVariable Long id, @RequestBody Branch branch,
                                                @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(branchService.update(id, branch, user.getUsername()));
    }

    // Loyalty
    @GetMapping("/loyalty/{customerId}")
    public ResponseEntity<Map<String, Object>> getLoyaltyBalance(@PathVariable Long customerId) {
        return ResponseEntity.ok(Map.of(
                "customerId", customerId,
                "points", loyaltyService.getBalance(customerId),
                "history", loyaltyService.getHistory(customerId)
        ));
    }

    @PostMapping("/loyalty/{customerId}/redeem")
    public ResponseEntity<Map<String, String>> redeemPoints(@PathVariable Long customerId,
                                                             @RequestBody Map<String, Integer> body,
                                                             @AuthenticationPrincipal UserDetails user) {
        loyaltyService.redeemPoints(customerId, body.get("points"), user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Points redeemed successfully"));
    }

    // Online Orders
    @GetMapping("/online-orders")
    public ResponseEntity<List<OnlineOrder>> getOnlineOrders() {
        return ResponseEntity.ok(onlineOrderService.getAll());
    }

    @GetMapping("/online-orders/status/{status}")
    public ResponseEntity<List<OnlineOrder>> getOrdersByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(onlineOrderService.getByStatus(status));
    }

    @GetMapping("/online-orders/{id}")
    public ResponseEntity<OnlineOrder> getOnlineOrder(@PathVariable Long id) {
        return ResponseEntity.ok(onlineOrderService.getByIdWithDetails(id));
    }

    @PostMapping("/online-orders")
    public ResponseEntity<OnlineOrder> createOnlineOrder(@RequestBody Map<String, Object> request,
                                                          @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(onlineOrderService.create(request, user.getUsername()));
    }

    @PutMapping("/online-orders/{id}")
    public ResponseEntity<OnlineOrder> updateOnlineOrder(@PathVariable Long id,
                                                         @RequestBody Map<String, Object> request,
                                                         @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(onlineOrderService.update(id, request, user.getUsername()));
    }

    @DeleteMapping("/online-orders/{id}")
    public ResponseEntity<Map<String, String>> deleteOnlineOrder(@PathVariable Long id,
                                                                 @AuthenticationPrincipal UserDetails user) {
        onlineOrderService.delete(id, user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Order deleted successfully"));
    }

    @PutMapping("/online-orders/{id}/status")
    public ResponseEntity<OnlineOrder> updateOrderStatus(@PathVariable Long id,
                                                          @RequestBody Map<String, String> body,
                                                          @AuthenticationPrincipal UserDetails user) {
        OrderStatus status = OrderStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(onlineOrderService.updateStatus(id, status, user.getUsername()));
    }

    // AI Reorder Suggestions
    @GetMapping("/reorder-suggestions")
    public ResponseEntity<List<Map<String, Object>>> getReorderSuggestions() {
        return ResponseEntity.ok(reorderSuggestionService.getSuggestions());
    }

    // Accounting
    @GetMapping("/accounting/entries")
    public ResponseEntity<List<AccountingEntry>> getAccountingEntries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(accountingIntegrationService.getEntries(startDate, endDate));
    }

    @PostMapping("/accounting/export")
    public ResponseEntity<Map<String, Object>> exportAccounting(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(accountingIntegrationService.exportForAccounting(startDate, endDate, user.getUsername()));
    }

    // SMS
    @PostMapping("/notifications/sms")
    public ResponseEntity<NotificationLog> sendSms(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(smsNotificationService.sendSms(body.get("phone"), body.get("message")));
    }

    // WhatsApp
    @PostMapping("/sales/{id}/whatsapp")
    public ResponseEntity<Map<String, String>> shareViaWhatsApp(@PathVariable Long id,
                                                                 @RequestBody Map<String, String> body) {
        Sale sale = saleService.getById(id);
        return ResponseEntity.ok(whatsAppService.shareInvoice(sale, body.get("phone")));
    }

    // Barcode lookup
    @GetMapping("/medicines/barcode/{barcode}")
    public ResponseEntity<?> lookupBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(medicineService.search(barcode));
    }

    // Audit Logs
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(auditLogService.getRecent(limit));
    }

    @PostMapping("/audit-logs/by-user")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUser(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(auditLogService.getByUsername(body.get("username")));
    }

    @GetMapping("/audit-logs/range")
    public ResponseEntity<List<AuditLog>> getAuditLogsByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(auditLogService.getByDateRange(start, end));
    }

    // Backups
    @GetMapping("/backups")
    public ResponseEntity<List<String>> listBackups() throws Exception {
        return ResponseEntity.ok(backupService.listBackups());
    }

    @PostMapping("/backups")
    public ResponseEntity<Map<String, String>> createBackup() throws Exception {
        String path = backupService.createBackup();
        return ResponseEntity.ok(Map.of("message", "Backup created", "path", path));
    }

    // Notification logs
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationLog>> getNotifications() {
        return ResponseEntity.ok(notificationLogRepository.findTop50ByOrderBySentAtDesc());
    }

    @GetMapping("/notifications/low-stock")
    public ResponseEntity<List<com.medicalbilling.dto.DtoModels.AlertItem>> getLowStockAlerts() {
        return ResponseEntity.ok(lowStockNotificationService.getDetailedLowStockAlerts());
    }

    @GetMapping("/notifications/near-expiry")
    public ResponseEntity<List<com.medicalbilling.dto.DtoModels.AlertItem>> getNearExpiryAlerts() {
        return ResponseEntity.ok(nearExpiryNotificationService.getDetailedNearExpiryAlerts());
    }

    @GetMapping("/notifications/inventory-alerts")
    public ResponseEntity<Map<String, List<com.medicalbilling.dto.DtoModels.AlertItem>>> getInventoryAlerts() {
        return ResponseEntity.ok(Map.of(
                "lowStock", lowStockNotificationService.getDetailedLowStockAlerts(),
                "nearExpiry", nearExpiryNotificationService.getDetailedNearExpiryAlerts()
        ));
    }

    @PostMapping("/notifications/low-stock/digest")
    public ResponseEntity<Map<String, Object>> sendLowStockDigest() {
        int count = lowStockNotificationService.sendDailyLowStockDigest();
        return ResponseEntity.ok(Map.of(
                "message", count > 0 ? "Low stock digest sent" : "No low stock items or digest already sent today",
                "count", count
        ));
    }
}
