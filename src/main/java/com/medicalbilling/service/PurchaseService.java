package com.medicalbilling.service;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.entity.*;
import com.medicalbilling.exception.BusinessException;
import com.medicalbilling.exception.ResourceNotFoundException;
import com.medicalbilling.repository.PurchaseRepository;
import com.medicalbilling.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierService supplierService;
    private final com.medicalbilling.repository.SupplierRepository supplierRepository;
    private final MedicineService medicineService;
    private final AuditService auditService;
    private final AccountingIntegrationService accountingIntegrationService;

    public List<Purchase> getAll() {
        return purchaseRepository.findAll();
    }

    public Purchase getById(Long id) {
        return purchaseRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + id));
    }

    @Transactional
    public Purchase create(DtoModels.PurchaseRequest request, String username) {
        if (purchaseRepository.findByInvoiceNumber(request.getInvoiceNumber()).isPresent()) {
            throw new BusinessException("Invoice number already exists");
        }
        Supplier supplier = supplierService.findSupplier(request.getSupplierId());
        Purchase purchase = Purchase.builder()
                .invoiceNumber(request.getInvoiceNumber() != null ? request.getInvoiceNumber() : CodeGenerator.generatePurchaseInvoice())
                .supplier(supplier)
                .purchaseDate(request.getPurchaseDate())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal gstAmount = BigDecimal.ZERO;

        for (DtoModels.PurchaseItemRequest itemReq : request.getItems()) {
            Medicine medicine = medicineService.findMedicine(itemReq.getMedicineId());
            BigDecimal subtotal = itemReq.getPurchasePrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemGst = itemReq.getGstAmount() != null ? itemReq.getGstAmount() : BigDecimal.ZERO;

            PurchaseItem item = PurchaseItem.builder()
                    .medicine(medicine)
                    .quantity(itemReq.getQuantity())
                    .purchasePrice(itemReq.getPurchasePrice())
                    .gstAmount(itemGst)
                    .subtotal(subtotal)
                    .expiryDate(itemReq.getExpiryDate())
                    .batchNumber(itemReq.getBatchNumber())
                    .build();
            purchase.addItem(item);

            totalAmount = totalAmount.add(subtotal);
            gstAmount = gstAmount.add(itemGst);

            medicineService.adjustStock(medicine.getId(), itemReq.getQuantity());
            medicineService.updatePurchaseDetails(medicine.getId(), itemReq.getPurchasePrice(),
                    itemReq.getExpiryDate(), itemReq.getBatchNumber());
        }

        purchase.setTotalAmount(totalAmount);
        purchase.setGstAmount(gstAmount);
        purchase.setGrandTotal(totalAmount.add(gstAmount));

        supplier.setOutstandingBalance(supplier.getOutstandingBalance().add(purchase.getGrandTotal()));
        supplierRepository.save(supplier);

        Purchase saved = purchaseRepository.save(purchase);
        auditService.log("CREATE", "Purchase", saved.getId(), username, "Created purchase: " + saved.getInvoiceNumber());
        accountingIntegrationService.recordPurchase(saved);
        return saved;
    }
}
