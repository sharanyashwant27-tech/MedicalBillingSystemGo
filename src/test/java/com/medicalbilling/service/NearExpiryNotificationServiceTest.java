package com.medicalbilling.service;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.entity.Category;
import com.medicalbilling.entity.Medicine;
import com.medicalbilling.entity.MedicineStatus;
import com.medicalbilling.repository.CategoryRepository;
import com.medicalbilling.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NearExpiryNotificationServiceTest {

    @Autowired
    private NearExpiryNotificationService nearExpiryNotificationService;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(Category.builder()
                .name("Tablets")
                .description("Tablet medicines")
                .build());

        medicineRepository.save(Medicine.builder()
                .medicineCode("EXP001")
                .medicineName("Expiring Soon")
                .category(category)
                .batchNumber("B100")
                .expiryDate(LocalDate.now().plusDays(10))
                .purchasePrice(new BigDecimal("10.00"))
                .sellingPrice(new BigDecimal("15.00"))
                .minimumStock(5)
                .currentStock(20)
                .status(MedicineStatus.ACTIVE)
                .build());

        medicineRepository.save(Medicine.builder()
                .medicineCode("EXP002")
                .medicineName("Safe Stock")
                .category(category)
                .batchNumber("B200")
                .expiryDate(LocalDate.now().plusDays(90))
                .purchasePrice(new BigDecimal("10.00"))
                .sellingPrice(new BigDecimal("15.00"))
                .minimumStock(5)
                .currentStock(20)
                .status(MedicineStatus.ACTIVE)
                .build());
    }

    @Test
    void includesMedicinesExpiringWithinThirtyDays() {
        assertEquals(1, nearExpiryNotificationService.countNearExpiryMedicines());

        DtoModels.AlertItem alert = nearExpiryNotificationService.getDetailedNearExpiryAlerts().getFirst();
        assertEquals("NEAR_EXPIRY", alert.getType());
        assertEquals("Expiring Soon", alert.getMedicineName());
        assertTrue(alert.getShortage() <= 10);
    }
}
