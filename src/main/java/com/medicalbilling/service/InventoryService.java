package com.medicalbilling.service;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.entity.Medicine;
import com.medicalbilling.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final MedicineRepository medicineRepository;
    private final MedicineService medicineService;
    private final LowStockNotificationService lowStockNotificationService;
    private final NearExpiryNotificationService nearExpiryNotificationService;

    @Transactional(readOnly = true)
    public DtoModels.InventorySummary getInventorySummary(String filter) {
        List<Medicine> all = medicineRepository.findAll();
        List<Medicine> filtered = switch (filter != null ? filter.toUpperCase() : "ALL") {
            case "LOW_STOCK" -> all.stream()
                    .filter(m -> lowStockNotificationService.isLowStock(m.getCurrentStock()))
                    .collect(Collectors.toList());
            case "OUT_OF_STOCK" -> all.stream()
                    .filter(m -> m.getCurrentStock() == 0)
                    .collect(Collectors.toList());
            case "NEAR_EXPIRY" -> nearExpiryNotificationService.getNearExpiryMedicines();
            case "EXPIRED" -> all.stream()
                    .filter(m -> m.getExpiryDate() != null && m.getExpiryDate().isBefore(LocalDate.now()))
                    .sorted(Comparator.comparing(m -> Objects.requireNonNull(m.getExpiryDate())))
                    .collect(Collectors.toList());
            default -> all;
        };

        long lowStock = all.stream()
                .filter(m -> lowStockNotificationService.isLowStock(m.getCurrentStock()) && m.getCurrentStock() > 0)
                .count();
        long outOfStock = all.stream().filter(m -> m.getCurrentStock() == 0).count();
        long nearExpiry = nearExpiryNotificationService.countNearExpiryMedicines();
        long expired = medicineRepository.findByExpiryDateBefore(LocalDate.now()).size();
        BigDecimal valuation = medicineRepository.calculateInventoryValuation();
        if (valuation == null) valuation = BigDecimal.ZERO;

        return DtoModels.InventorySummary.builder()
                .totalMedicines(all.size())
                .lowStock(lowStock)
                .outOfStock(outOfStock)
                .nearExpiry(nearExpiry)
                .expired(expired)
                .valuation(valuation)
                .items(filtered.stream().map(medicineService::toResponse).collect(Collectors.toList()))
                .build();
    }
}
