package com.medicalbilling.service;

import com.medicalbilling.entity.*;
import com.medicalbilling.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final MedicineRepository medicineRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;

    public Map<String, Object> generateReport(String reportType, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return switch (reportType.toUpperCase()) {
            case "DAILY_SALES", "MONTHLY_SALES", "YEARLY_SALES", "SALES" -> salesReport(start, end);
            case "PURCHASE" -> purchaseReport(startDate, endDate);
            case "PROFIT" -> profitReport(start, end);
            case "GST" -> gstReport(start, end);
            case "MEDICINE" -> medicineReport();
            case "CUSTOMER" -> customerReport();
            case "SUPPLIER" -> supplierReport();
            case "STOCK" -> stockReport();
            case "EXPIRY" -> expiryReport();
            default -> salesReport(start, end);
        };
    }

    private Map<String, Object> salesReport(LocalDateTime start, LocalDateTime end) {
        List<Sale> sales = saleRepository.findSalesBetween(start, end);
        BigDecimal total = saleRepository.sumSalesBetween(start, end);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportType", "Sales Report");
        report.put("startDate", start.toLocalDate());
        report.put("endDate", end.toLocalDate());
        report.put("totalSales", total != null ? total : BigDecimal.ZERO);
        report.put("totalBills", sales.size());
        report.put("sales", sales);
        return report;
    }

    private Map<String, Object> purchaseReport(LocalDate start, LocalDate end) {
        List<Purchase> purchases = purchaseRepository.findByPurchaseDateBetween(start, end);
        BigDecimal total = purchaseRepository.sumPurchasesBetween(start, end);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportType", "Purchase Report");
        report.put("startDate", start);
        report.put("endDate", end);
        report.put("totalPurchases", total != null ? total : BigDecimal.ZERO);
        report.put("totalInvoices", purchases.size());
        report.put("purchases", purchases);
        return report;
    }

    private Map<String, Object> profitReport(LocalDateTime start, LocalDateTime end) {
        List<Sale> sales = saleRepository.findSalesBetween(start, end);
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (Sale sale : sales) {
            for (SaleItem item : sale.getItems()) {
                revenue = revenue.add(item.getSubtotal());
                cost = cost.add(item.getMedicine().getPurchasePrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportType", "Profit Report");
        report.put("revenue", revenue);
        report.put("cost", cost);
        report.put("profit", revenue.subtract(cost));
        return report;
    }

    private Map<String, Object> gstReport(LocalDateTime start, LocalDateTime end) {
        List<Sale> sales = saleRepository.findSalesBetween(start, end);
        BigDecimal totalGst = BigDecimal.ZERO;
        for (Sale sale : sales) {
            if (sale != null && sale.getGstAmount() != null) {
                totalGst = totalGst.add(sale.getGstAmount());
            }
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportType", "GST Report");
        report.put("totalGstCollected", totalGst);
        report.put("sales", sales);
        return report;
    }

    private Map<String, Object> medicineReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportType", "Medicine Report");
        report.put("medicines", medicineRepository.findAll());
        report.put("totalCount", medicineRepository.count());
        return report;
    }

    private Map<String, Object> customerReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportType", "Customer Report");
        report.put("customers", customerRepository.findAll());
        report.put("totalCount", customerRepository.count());
        return report;
    }

    private Map<String, Object> supplierReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportType", "Supplier Report");
        report.put("suppliers", supplierRepository.findAll());
        report.put("totalCount", supplierRepository.count());
        return report;
    }

    private Map<String, Object> stockReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportType", "Stock Report");
        report.put("medicines", medicineRepository.findAll());
        report.put("valuation", medicineRepository.calculateInventoryValuation());
        return report;
    }

    private Map<String, Object> expiryReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportType", "Expiry Report");
        report.put("expired", medicineRepository.findByExpiryDateBefore(LocalDate.now()));
        report.put("nearExpiry", medicineRepository.findByExpiryDateBetween(
                LocalDate.now(), LocalDate.now().plusDays(90)));
        return report;
    }
}
