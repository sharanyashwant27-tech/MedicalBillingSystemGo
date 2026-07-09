package com.medicalbilling.service;

import com.medicalbilling.entity.*;
import com.medicalbilling.exception.BusinessException;
import com.medicalbilling.repository.ReturnRepository;
import com.medicalbilling.repository.UserRepository;
import com.medicalbilling.util.CodeGenerator;
import com.medicalbilling.dto.DtoModels;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ReturnRepository returnRepository;
    private final MedicineService medicineService;
    private final SaleService saleService;
    private final PurchaseService purchaseService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public List<MedicineReturn> getAll() {
        return returnRepository.findAll();
    }

    @Transactional
    public MedicineReturn processReturn(DtoModels.ReturnRequest request, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Medicine medicine = medicineService.findMedicine(request.getMedicineId());

        BigDecimal refundAmount;
        Sale sale = null;
        Purchase purchase = null;

        if (request.getReturnType() == ReturnType.SALES_RETURN) {
            if (request.getSaleId() == null) throw new BusinessException("Sale ID required for sales return");
            sale = saleService.getById(request.getSaleId());
            refundAmount = medicine.getSellingPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            medicineService.adjustStock(medicine.getId(), request.getQuantity());
        } else {
            if (request.getPurchaseId() == null) throw new BusinessException("Purchase ID required for purchase return");
            purchase = purchaseService.getById(request.getPurchaseId());
            refundAmount = medicine.getPurchasePrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            medicineService.adjustStock(medicine.getId(), -request.getQuantity());
        }

        MedicineReturn medicineReturn = MedicineReturn.builder()
                .returnNumber(CodeGenerator.generateReturnNumber())
                .returnType(request.getReturnType())
                .medicine(medicine)
                .quantity(request.getQuantity())
                .refundAmount(refundAmount)
                .reason(request.getReason())
                .processedBy(user)
                .build();

        if (sale != null) medicineReturn.setSale(sale);
        if (purchase != null) medicineReturn.setPurchase(purchase);

        MedicineReturn saved = returnRepository.save(Objects.requireNonNull(medicineReturn));
        auditService.log("CREATE", "Return", saved.getId(), username, "Processed return: " + saved.getReturnNumber());
        return saved;
    }
}
