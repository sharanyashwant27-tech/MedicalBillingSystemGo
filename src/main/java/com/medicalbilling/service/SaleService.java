package com.medicalbilling.service;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.entity.*;
import com.medicalbilling.exception.BusinessException;
import com.medicalbilling.exception.ResourceNotFoundException;
import com.medicalbilling.repository.SaleRepository;
import com.medicalbilling.repository.UserRepository;
import com.medicalbilling.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final MedicineService medicineService;
    private final CustomerService customerService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final LoyaltyService loyaltyService;
    private final AccountingIntegrationService accountingIntegrationService;
    private final SmsNotificationService smsNotificationService;

    @Transactional(readOnly = true)
    public List<Sale> getAll() {
        return saleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Sale getById(Long id) {
        return saleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Sale> getRecentBills() {
        return saleRepository.findRecentWithCustomer(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "saleDate")));
    }

    @Transactional
    public Sale create(DtoModels.SaleRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Sale sale = Sale.builder()
                .billNumber(CodeGenerator.generateBillNumber())
                .createdBy(user)
                .paymentMode(request.getPaymentMode())
                .amountPaid(request.getAmountPaid())
                .build();

        if (user.getBranch() != null) {
            sale.setBranch(user.getBranch());
        }

        if (request.getCustomerId() != null) {
            sale.setCustomer(customerService.findCustomer(request.getCustomerId()));
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal gstAmount = BigDecimal.ZERO;

        for (DtoModels.SaleItemRequest itemReq : request.getItems()) {
            Medicine medicine = medicineService.findMedicine(itemReq.getMedicineId());
            if (medicine.getCurrentStock() < itemReq.getQuantity()) {
                throw new BusinessException("Insufficient stock for: " + medicine.getMedicineName());
            }

            BigDecimal discountPercent = itemReq.getDiscountPercent() != null
                    ? itemReq.getDiscountPercent() : medicine.getDiscountPercent();
            BigDecimal lineTotal = medicine.getSellingPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            BigDecimal lineDiscount = lineTotal.multiply(discountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal taxable = lineTotal.subtract(lineDiscount);
            BigDecimal lineGst = taxable.multiply(medicine.getGstPercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal itemSubtotal = taxable.add(lineGst);

            SaleItem item = SaleItem.builder()
                    .medicine(medicine)
                    .batchNumber(itemReq.getBatchNumber() != null ? itemReq.getBatchNumber() : medicine.getBatchNumber())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(medicine.getSellingPrice())
                    .discountPercent(discountPercent)
                    .gstAmount(lineGst)
                    .subtotal(itemSubtotal)
                    .build();
            sale.addItem(item);

            subtotal = subtotal.add(lineTotal);
            discountAmount = discountAmount.add(lineDiscount);
            gstAmount = gstAmount.add(lineGst);

            medicineService.adjustStock(medicine.getId(), -itemReq.getQuantity());
        }

        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(discountAmount);
        sale.setGstAmount(gstAmount);
        sale.setGrandTotal(subtotal.subtract(discountAmount).add(gstAmount));
        sale.setReturnAmount(request.getAmountPaid().subtract(sale.getGrandTotal()).max(BigDecimal.ZERO));

        Sale saved = saleRepository.save(sale);
        auditService.log("CREATE", "Sale", saved.getId(), username, "Created sale: " + saved.getBillNumber());
        loyaltyService.earnPointsFromSale(saved);
        accountingIntegrationService.recordSale(saved);
        if (saved.getCustomer() != null && saved.getCustomer().getPhone() != null) {
            smsNotificationService.sendBillNotification(
                    saved.getCustomer().getPhone(), saved.getBillNumber(), saved.getGrandTotal().toString());
        }
        return saved;
    }
}
