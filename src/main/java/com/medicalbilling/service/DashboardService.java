package com.medicalbilling.service;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.entity.Medicine;
import com.medicalbilling.entity.Sale;
import com.medicalbilling.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final MedicineRepository medicineRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final PaymentRepository paymentRepository;
    private final LowStockNotificationService lowStockNotificationService;

    @Transactional(readOnly = true)
    public DtoModels.DashboardResponse getDashboardData() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        BigDecimal todaySales = saleRepository.sumSalesBetween(startOfDay, endOfDay);
        if (todaySales == null) todaySales = BigDecimal.ZERO;

        List<Sale> todaySaleList = saleRepository.findSalesBetweenWithDetails(startOfDay, endOfDay);
        BigDecimal todayProfit = calculateTodayProfit(todaySaleList);

        long lowStock = lowStockNotificationService.countLowStockMedicines();
        long expired = medicineRepository.findByExpiryDateBefore(LocalDate.now()).size();

        List<DtoModels.AlertItem> alerts = buildAlerts();

        List<DtoModels.SaleSummary> recentBills = saleRepository.findRecentWithCustomer(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "saleDate"))).stream()
                .map(s -> DtoModels.SaleSummary.builder()
                        .id(s.getId())
                        .billNumber(s.getBillNumber())
                        .customerName(s.getCustomer() != null ? s.getCustomer().getCustomerName() : "Walk-in")
                        .grandTotal(s.getGrandTotal())
                        .saleDate(s.getSaleDate())
                        .paymentMode(s.getPaymentMode())
                        .build())
                .collect(Collectors.toList());

        return DtoModels.DashboardResponse.builder()
                .todaySales(todaySales)
                .todayProfit(todayProfit)
                .availableMedicines(medicineRepository.countActiveMedicines())
                .lowStockMedicines(lowStock)
                .expiredMedicines(expired)
                .totalCustomers(customerRepository.count())
                .totalSuppliers(supplierRepository.count())
                .pendingPayments(paymentRepository.findByPendingTrue().size())
                .recentBills(recentBills)
                .alerts(alerts)
                .build();
    }

    private BigDecimal calculateTodayProfit(List<Sale> sales) {
        BigDecimal profit = BigDecimal.ZERO;
        for (Sale sale : sales) {
            for (var item : sale.getItems()) {
                BigDecimal revenue = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                BigDecimal cost = item.getMedicine().getPurchasePrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                profit = profit.add(revenue.subtract(cost));
            }
        }
        return profit;
    }

    private List<DtoModels.AlertItem> buildAlerts() {
        List<DtoModels.AlertItem> alerts = new ArrayList<>(lowStockNotificationService.getDetailedLowStockAlerts());
        List<Medicine> nearExpiry = medicineRepository.findByExpiryDateBetween(
                LocalDate.now(), LocalDate.now().plusDays(30));
        for (Medicine m : nearExpiry) {
            alerts.add(toExpiryAlert(m, "NEAR_EXPIRY",
                    m.getMedicineName() + " expires on " + m.getExpiryDate(), "warning"));
        }

        List<Medicine> expired = medicineRepository.findByExpiryDateBefore(LocalDate.now());
        for (Medicine m : expired) {
            alerts.add(toExpiryAlert(m, "EXPIRED",
                    m.getMedicineName() + " expired on " + m.getExpiryDate(), "danger"));
        }
        return alerts;
    }

    private DtoModels.AlertItem toExpiryAlert(Medicine medicine, String type, String message, String severity) {
        return DtoModels.AlertItem.builder()
                .type(type)
                .message(message)
                .severity(severity)
                .stockStatus(type)
                .medicineId(medicine.getId())
                .medicineCode(medicine.getMedicineCode())
                .medicineName(medicine.getMedicineName())
                .currentStock(medicine.getCurrentStock())
                .minimumStock(medicine.getMinimumStock())
                .categoryName(medicine.getCategory() != null ? medicine.getCategory().getName() : null)
                .supplierName(medicine.getSupplier() != null ? medicine.getSupplier().getSupplierName() : null)
                .supplierPhone(medicine.getSupplier() != null ? medicine.getSupplier().getPhone() : null)
                .batchNumber(medicine.getBatchNumber())
                .expiryDate(medicine.getExpiryDate())
                .rackNumber(medicine.getRackNumber())
                .build();
    }
}
