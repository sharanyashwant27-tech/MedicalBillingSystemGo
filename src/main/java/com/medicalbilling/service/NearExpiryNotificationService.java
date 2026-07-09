package com.medicalbilling.service;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.entity.Medicine;
import com.medicalbilling.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NearExpiryNotificationService {

    private final MedicineRepository medicineRepository;

    @Value("${app.near-expiry.days:30}")
    private int nearExpiryDays;

    public int getNearExpiryDays() {
        return nearExpiryDays;
    }

    @Transactional(readOnly = true)
    public List<Medicine> getNearExpiryMedicines() {
        LocalDate today = LocalDate.now();
        return medicineRepository.findNearExpiryMedicinesWithDetails(today, today.plusDays(nearExpiryDays));
    }

    @Transactional(readOnly = true)
    public List<DtoModels.AlertItem> getDetailedNearExpiryAlerts() {
        return getNearExpiryMedicines().stream()
                .map(this::toAlertItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countNearExpiryMedicines() {
        return getNearExpiryMedicines().size();
    }

    private DtoModels.AlertItem toAlertItem(Medicine medicine) {
        LocalDate expiryDate = medicine.getExpiryDate();
        long daysLeft = expiryDate != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), expiryDate)
                : nearExpiryDays;

        return DtoModels.AlertItem.builder()
                .type("NEAR_EXPIRY")
                .severity(daysLeft <= 7 ? "danger" : "warning")
                .stockStatus("NEAR_EXPIRY")
                .medicineId(medicine.getId())
                .medicineCode(medicine.getMedicineCode())
                .medicineName(medicine.getMedicineName())
                .currentStock(medicine.getCurrentStock())
                .minimumStock(medicine.getMinimumStock())
                .shortage((int) Math.max(daysLeft, 0))
                .categoryName(medicine.getCategory() != null ? medicine.getCategory().getName() : null)
                .supplierName(medicine.getSupplier() != null ? medicine.getSupplier().getSupplierName() : null)
                .supplierPhone(medicine.getSupplier() != null ? medicine.getSupplier().getPhone() : null)
                .batchNumber(medicine.getBatchNumber())
                .expiryDate(expiryDate)
                .rackNumber(medicine.getRackNumber())
                .message(buildSummaryLine(medicine, daysLeft, expiryDate))
                .build();
    }

    private String buildSummaryLine(Medicine medicine, long daysLeft, LocalDate expiryDate) {
        if (daysLeft <= 0) {
            return medicine.getMedicineName() + " — expires today (" + expiryDate + ")";
        }
        return medicine.getMedicineName() + " — expires in " + daysLeft + " day(s) on " + expiryDate;
    }
}
