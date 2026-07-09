package com.medicalbilling.service;

import com.medicalbilling.entity.*;
import com.medicalbilling.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReorderSuggestionService {

    private final MedicineRepository medicineRepository;
    private final SaleRepository saleRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSuggestions() {
        List<Medicine> medicines = medicineRepository.findAllWithDetails();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        List<Sale> recentSales = saleRepository.findSalesBetweenWithDetails(thirtyDaysAgo, LocalDateTime.now());

        Map<Long, Integer> salesVelocity = new HashMap<>();
        for (Sale sale : recentSales) {
            for (SaleItem item : sale.getItems()) {
                salesVelocity.merge(item.getMedicine().getId(), item.getQuantity(), Integer::sum);
            }
        }

        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (Medicine medicine : medicines) {
            int monthlySales = salesVelocity.getOrDefault(medicine.getId(), 0);
            double dailyVelocity = monthlySales / 30.0;
            int daysOfStock = dailyVelocity > 0
                    ? (int) (medicine.getCurrentStock() / dailyVelocity)
                    : medicine.getCurrentStock() > 0 ? 999 : 0;

            int suggestedQty = 0;
            String priority;
            String reason;

            if (medicine.getCurrentStock() == 0) {
                suggestedQty = Math.max(medicine.getMinimumStock() * 2, 50);
                priority = "CRITICAL";
                reason = "Out of stock - immediate reorder required";
            } else if (medicine.getCurrentStock() <= medicine.getMinimumStock()) {
                suggestedQty = medicine.getMinimumStock() * 3 - medicine.getCurrentStock();
                priority = "HIGH";
                reason = "Below minimum stock level";
            } else if (daysOfStock <= 7) {
                suggestedQty = (int) Math.ceil(dailyVelocity * 30) - medicine.getCurrentStock();
                priority = "MEDIUM";
                reason = "Stock will last ~" + daysOfStock + " days based on sales velocity";
            } else if (medicine.getExpiryDate() != null && medicine.getExpiryDate().isBefore(LocalDate.now().plusDays(60))) {
                continue;
            } else {
                continue;
            }

            if (suggestedQty > 0) {
                Map<String, Object> suggestion = new LinkedHashMap<>();
                suggestion.put("medicineId", medicine.getId());
                suggestion.put("medicineName", medicine.getMedicineName());
                suggestion.put("medicineCode", medicine.getMedicineCode());
                suggestion.put("currentStock", medicine.getCurrentStock());
                suggestion.put("minimumStock", medicine.getMinimumStock());
                suggestion.put("monthlySales", monthlySales);
                suggestion.put("dailyVelocity", BigDecimal.valueOf(dailyVelocity).setScale(2, RoundingMode.HALF_UP));
                suggestion.put("daysOfStockRemaining", daysOfStock);
                suggestion.put("suggestedReorderQty", suggestedQty);
                suggestion.put("priority", priority);
                suggestion.put("reason", reason);
                suggestion.put("estimatedCost", medicine.getPurchasePrice().multiply(BigDecimal.valueOf(suggestedQty)));
                suggestion.put("supplierName", medicine.getSupplier() != null ? medicine.getSupplier().getSupplierName() : "N/A");
                suggestions.add(suggestion);
            }
        }

        suggestions.sort((a, b) -> {
            Map<String, Integer> priorityOrder = Map.of("CRITICAL", 0, "HIGH", 1, "MEDIUM", 2);
            return priorityOrder.getOrDefault(a.get("priority"), 3)
                    .compareTo(priorityOrder.getOrDefault(b.get("priority"), 3));
        });
        return suggestions;
    }
}
