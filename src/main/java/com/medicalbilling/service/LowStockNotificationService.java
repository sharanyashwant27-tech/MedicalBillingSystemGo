package com.medicalbilling.service;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.entity.Medicine;
import com.medicalbilling.entity.ShopSettings;
import com.medicalbilling.entity.User;
import com.medicalbilling.repository.MedicineRepository;
import com.medicalbilling.repository.NotificationLogRepository;
import com.medicalbilling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LowStockNotificationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final MedicineRepository medicineRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final SettingsService settingsService;
    private final SmsNotificationService smsNotificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Value("${app.low-stock.notify.enabled:true}")
    private boolean notifyEnabled;

    @Value("${app.low-stock.threshold:10}")
    private int lowStockThreshold;

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public boolean isLowStock(int currentStock) {
        return currentStock < lowStockThreshold;
    }

    @Transactional(readOnly = true)
    public List<DtoModels.AlertItem> getDetailedLowStockAlerts() {
        return medicineRepository.findLowStockMedicinesWithDetails(lowStockThreshold).stream()
                .map(this::toAlertItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countLowStockMedicines() {
        return medicineRepository.findLowStockMedicinesWithDetails(lowStockThreshold).size();
    }

    @Transactional
    public void notifyOnStockChange(Medicine medicine, int previousStock) {
        if (!notifyEnabled || medicine == null) {
            return;
        }
        int currentStock = medicine.getCurrentStock();
        boolean wasAboveThreshold = previousStock >= lowStockThreshold;
        boolean isNowLow = currentStock < lowStockThreshold;
        if (!wasAboveThreshold || !isNowLow) {
            return;
        }

        String referenceId = "LOW_STOCK:" + medicine.getId() + ":" + LocalDate.now();
        if (alreadyNotified(referenceId)) {
            return;
        }

        DtoModels.AlertItem alert = toAlertItem(medicine);
        String message = buildIndividualAlertMessage(alert);
        sendToRecipients(message, referenceId);
        ShopSettings settings = settingsService.getSettings();
        if (settings.getEmail() != null && !settings.getEmail().isBlank()) {
            emailService.sendLowStockAlertEmail(settings.getEmail(), message, referenceId);
        }
        log.info("Low stock alert sent for {} (stock: {}, threshold: {})",
                medicine.getMedicineName(), currentStock, lowStockThreshold);
    }

    @Transactional
    public int sendDailyLowStockDigest() {
        if (!notifyEnabled) {
            return 0;
        }

        List<DtoModels.AlertItem> alerts = getDetailedLowStockAlerts();
        if (alerts.isEmpty()) {
            return 0;
        }

        String referenceId = "LOW_STOCK_DIGEST:" + LocalDate.now();
        if (alreadyNotified(referenceId)) {
            return 0;
        }

        String smsMessage = buildDigestSmsMessage(alerts);
        String emailMessage = buildDigestEmailMessage(alerts);
        sendToRecipients(smsMessage, referenceId);
        sendDigestEmail(emailMessage, referenceId);
        log.info("Daily low stock digest sent for {} medicines", alerts.size());
        return alerts.size();
    }

    private boolean alreadyNotified(String referenceId) {
        return notificationLogRepository.existsByReferenceIdAndSentAtAfter(
                referenceId, LocalDate.now().atStartOfDay());
    }

    private void sendToRecipients(String message, String referenceId) {
        Set<String> phones = collectRecipientPhones();
        for (String phone : phones) {
            smsNotificationService.sendLowStockAlert(phone, message, referenceId);
        }
    }

    private void sendDigestEmail(String message, String referenceId) {
        ShopSettings settings = settingsService.getSettings();
        if (settings.getEmail() != null && !settings.getEmail().isBlank()) {
            emailService.sendLowStockAlertEmail(settings.getEmail(), message, referenceId);
        }
    }

    private Set<String> collectRecipientPhones() {
        Set<String> phones = new LinkedHashSet<>();
        ShopSettings settings = settingsService.getSettings();
        if (settings.getPhone() != null && !settings.getPhone().isBlank()) {
            phones.add(settings.getPhone().trim());
        }
        userRepository.findAll().stream()
                .filter(user -> user != null && user.isEnabled())
                .filter(user -> user.getPhone() != null && !user.getPhone().isBlank())
                .filter(user -> isInventoryNotifier(user))
                .map(user -> user.getPhone().trim())
                .forEach(phones::add);
        return phones;
    }

    private boolean isInventoryNotifier(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().name().contains("ADMIN")
                        || role.getName().name().contains("PHARMACIST"));
    }

    DtoModels.AlertItem toAlertItem(Medicine medicine) {
        int currentStock = medicine.getCurrentStock();
        int shortage = Math.max(lowStockThreshold - currentStock, 0);
        String stockStatus = currentStock <= 0 ? "OUT_OF_STOCK" : "LOW_STOCK";
        String severity = currentStock <= 0 ? "danger" : "warning";

        return DtoModels.AlertItem.builder()
                .type("LOW_STOCK")
                .severity(severity)
                .stockStatus(stockStatus)
                .medicineId(medicine.getId())
                .medicineCode(medicine.getMedicineCode())
                .medicineName(medicine.getMedicineName())
                .currentStock(currentStock)
                .minimumStock(lowStockThreshold)
                .shortage(shortage)
                .categoryName(medicine.getCategory() != null ? medicine.getCategory().getName() : null)
                .supplierName(medicine.getSupplier() != null ? medicine.getSupplier().getSupplierName() : null)
                .supplierPhone(medicine.getSupplier() != null ? medicine.getSupplier().getPhone() : null)
                .batchNumber(medicine.getBatchNumber())
                .expiryDate(medicine.getExpiryDate())
                .rackNumber(medicine.getRackNumber())
                .message(buildSummaryLine(medicine, currentStock, shortage, stockStatus))
                .build();
    }

    private String buildSummaryLine(Medicine medicine, int currentStock, int shortage, String stockStatus) {
        if ("OUT_OF_STOCK".equals(stockStatus)) {
            return medicine.getMedicineName() + " — OUT OF STOCK (below " + lowStockThreshold + " units)";
        }
        return medicine.getMedicineName() + " — Only " + currentStock + " left (below " + lowStockThreshold + " units)";
    }

    private String buildIndividualAlertMessage(DtoModels.AlertItem alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("MEDIBILL LOW STOCK ALERT\n");
        sb.append("Medicine: ").append(alert.getMedicineName());
        if (alert.getMedicineCode() != null) {
            sb.append(" (").append(alert.getMedicineCode()).append(")");
        }
        sb.append("\n");
        if ("OUT_OF_STOCK".equals(alert.getStockStatus())) {
            sb.append("Status: OUT OF STOCK (below ").append(lowStockThreshold).append(" units)\n");
        } else {
            sb.append("Stock: ").append(alert.getCurrentStock()).append(" units (alert below ")
                    .append(lowStockThreshold).append(")\n");
        }
        appendDetailLine(sb, "Category", alert.getCategoryName());
        appendDetailLine(sb, "Rack", alert.getRackNumber());
        appendDetailLine(sb, "Supplier", formatSupplier(alert.getSupplierName(), alert.getSupplierPhone()));
        appendDetailLine(sb, "Batch", alert.getBatchNumber());
        if (alert.getExpiryDate() != null) {
            appendDetailLine(sb, "Expiry", alert.getExpiryDate().format(DATE_FMT));
        }
        sb.append("Action: Reorder immediately");
        return sb.toString();
    }

    private String buildDigestSmsMessage(List<DtoModels.AlertItem> alerts) {
        StringBuilder sb = new StringBuilder();
        sb.append("MEDIBILL LOW STOCK REPORT (").append(alerts.size()).append(" items)\n");
        int limit = Math.min(alerts.size(), 8);
        for (int i = 0; i < limit; i++) {
            DtoModels.AlertItem alert = alerts.get(i);
            sb.append(i + 1).append(". ").append(alert.getMessage()).append("\n");
        }
        if (alerts.size() > limit) {
            sb.append("...and ").append(alerts.size() - limit).append(" more. Check Inventory.");
        }
        return sb.toString().trim();
    }

    private String buildDigestEmailMessage(List<DtoModels.AlertItem> alerts) {
        StringBuilder sb = new StringBuilder();
        sb.append("MediBill Low Stock Report\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))).append("\n");
        sb.append("Total low stock medicines: ").append(alerts.size()).append("\n\n");

        for (int i = 0; i < alerts.size(); i++) {
            DtoModels.AlertItem alert = alerts.get(i);
            sb.append(i + 1).append(". ").append(alert.getMedicineName());
            if (alert.getMedicineCode() != null) {
                sb.append(" [").append(alert.getMedicineCode()).append("]");
            }
            sb.append("\n");
            sb.append("   Status: ").append(alert.getStockStatus().replace('_', ' ')).append("\n");
            sb.append("   Stock: ").append(alert.getCurrentStock()).append(" units (threshold: ")
                    .append(lowStockThreshold).append(")\n");
            appendDetailLine(sb, "   Category", alert.getCategoryName());
            appendDetailLine(sb, "   Rack", alert.getRackNumber());
            appendDetailLine(sb, "   Supplier", formatSupplier(alert.getSupplierName(), alert.getSupplierPhone()));
            appendDetailLine(sb, "   Batch", alert.getBatchNumber());
            if (alert.getExpiryDate() != null) {
                appendDetailLine(sb, "   Expiry", alert.getExpiryDate().format(DATE_FMT));
            }
            sb.append("\n");
        }
        sb.append("Open Inventory > Low Stock in MediBill to review and reorder.");
        return sb.toString();
    }

    private void appendDetailLine(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(value).append("\n");
        }
    }

    private String formatSupplier(String name, String phone) {
        if (name == null || name.isBlank()) {
            return phone;
        }
        if (phone == null || phone.isBlank()) {
            return name;
        }
        return name + " (" + phone + ")";
    }
}
