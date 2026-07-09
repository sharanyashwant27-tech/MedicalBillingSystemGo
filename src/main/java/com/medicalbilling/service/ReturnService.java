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
        if (request.getReturnType() == ReturnType.SALES_RETURN) {
            if (request.getSaleId() == null) throw new BusinessException("Sale ID required for sales return");
            Sale sale = saleService.getById(request.getSaleId());
            refundAmount = medicine.getSellingPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            medicineService.adjustStock(medicine.getId(), request.getQuantity());
        } else {
            if (request.getPurchaseId() == null) throw new BusinessException("Purchase ID required for purchase return");
            Purchase purchase = purchaseService.getById(request.getPurchaseId());
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

        if (request.getSaleId() != null) medicineReturn.setSale(saleService.getById(request.getSaleId()));
        if (request.getPurchaseId() != null) medicineReturn.setPurchase(purchaseService.getById(request.getPurchaseId()));

        MedicineReturn saved = returnRepository.save(medicineReturn);
        auditService.log("CREATE", "Return", saved.getId(), username, "Processed return: " + saved.getReturnNumber());
        return saved;
    }
}
