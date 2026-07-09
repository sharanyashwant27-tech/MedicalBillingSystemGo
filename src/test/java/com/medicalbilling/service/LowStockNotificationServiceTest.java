package com.medicalbilling.service;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.entity.Category;
import com.medicalbilling.entity.Medicine;
import com.medicalbilling.entity.MedicineStatus;
import com.medicalbilling.entity.Supplier;
import com.medicalbilling.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LowStockNotificationServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    private LowStockNotificationService lowStockNotificationService;

    @BeforeEach
    void setUp() {
        lowStockNotificationService = new LowStockNotificationService(
                medicineRepository,
                null,
                null,
                null,
                null,
                null
        );
        org.springframework.test.util.ReflectionTestUtils.setField(
                lowStockNotificationService, "lowStockThreshold", 10);
    }

    @Test
    void buildsDetailedLowStockAlert() {
        Medicine medicine = Medicine.builder()
                .id(1L)
                .medicineCode("MED-001")
                .medicineName("Paracetamol 500mg")
                .category(Category.builder().name("Pain Relief").build())
                .supplier(Supplier.builder().supplierName("MediSupply").phone("9876543210").build())
                .batchNumber("B2024")
                .expiryDate(LocalDate.of(2026, 8, 15))
                .rackNumber("A-12")
                .minimumStock(10)
                .currentStock(3)
                .purchasePrice(new BigDecimal("10"))
                .sellingPrice(new BigDecimal("15"))
                .status(MedicineStatus.ACTIVE)
                .build();

        when(medicineRepository.findLowStockMedicinesWithDetails(10)).thenReturn(List.of(medicine));

        List<DtoModels.AlertItem> alerts = lowStockNotificationService.getDetailedLowStockAlerts();

        assertThat(alerts).hasSize(1);
        DtoModels.AlertItem alert = alerts.get(0);
        assertThat(alert.getMedicineName()).isEqualTo("Paracetamol 500mg");
        assertThat(alert.getCurrentStock()).isEqualTo(3);
        assertThat(alert.getMinimumStock()).isEqualTo(10);
        assertThat(alert.getShortage()).isEqualTo(7);
        assertThat(alert.getStockStatus()).isEqualTo("LOW_STOCK");
        assertThat(alert.getSupplierName()).isEqualTo("MediSupply");
        assertThat(alert.getMinimumStock()).isEqualTo(10);
        assertThat(alert.getMessage()).contains("below 10");
    }
}
