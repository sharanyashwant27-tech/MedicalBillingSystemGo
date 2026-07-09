package com.medicalbilling.service;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.entity.*;
import com.medicalbilling.exception.BusinessException;
import com.medicalbilling.exception.ResourceNotFoundException;
import com.medicalbilling.repository.MedicineRepository;
import com.medicalbilling.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final CategoryService categoryService;
    private final SupplierService supplierService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<DtoModels.MedicineResponse> getAll(String search) {
        List<Medicine> medicines = (search == null || search.isBlank())
                ? medicineRepository.findAllWithDetails()
                : medicineRepository.searchMedicinesWithDetails(search);
        return medicines.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DtoModels.MedicineResponse getById(Long id) {
        return toResponse(findMedicine(id));
    }

    @Transactional(readOnly = true)
    public List<DtoModels.MedicineResponse> search(String query) {
        return medicineRepository.searchMedicinesWithDetails(query).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public DtoModels.MedicineResponse create(DtoModels.MedicineRequest request, String username) {
        if (medicineRepository.existsByMedicineCode(request.getMedicineCode())) {
            throw new BusinessException("Medicine code already exists");
        }
        if (request.getBarcode() != null && medicineRepository.existsByBarcode(request.getBarcode())) {
            throw new BusinessException("Barcode already exists");
        }
        Medicine medicine = toEntity(request);
        if (medicine.getMedicineCode() == null || medicine.getMedicineCode().isBlank()) {
            medicine.setMedicineCode(CodeGenerator.generateMedicineCode());
        }
        Medicine saved = medicineRepository.save(medicine);
        auditService.log("CREATE", "Medicine", saved.getId(), username, "Created medicine: " + saved.getMedicineName());
        return toResponse(saved);
    }

    @Transactional
    public DtoModels.MedicineResponse update(Long id, DtoModels.MedicineRequest request, String username) {
        Medicine medicine = findMedicine(id);
        updateEntity(medicine, request);
        Medicine saved = medicineRepository.save(medicine);
        auditService.log("UPDATE", "Medicine", saved.getId(), username, "Updated medicine: " + saved.getMedicineName());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, String username) {
        Medicine medicine = findMedicine(id);
        medicine.setStatus(MedicineStatus.INACTIVE);
        medicineRepository.save(medicine);
        auditService.log("DELETE", "Medicine", id, username, "Deactivated medicine: " + medicine.getMedicineName());
    }

    @Transactional
    public void updatePurchaseDetails(Long medicineId, BigDecimal purchasePrice,
                                      java.time.LocalDate expiryDate, String batchNumber) {
        Medicine medicine = findMedicine(medicineId);
        medicine.setPurchasePrice(purchasePrice);
        if (expiryDate != null) medicine.setExpiryDate(expiryDate);
        if (batchNumber != null) medicine.setBatchNumber(batchNumber);
        medicineRepository.save(medicine);
    }

    @Transactional
    public void adjustStock(Long medicineId, int quantityChange) {
        Medicine medicine = findMedicine(medicineId);
        int newStock = medicine.getCurrentStock() + quantityChange;
        if (newStock < 0) {
            throw new BusinessException("Insufficient stock for medicine: " + medicine.getMedicineName());
        }
        medicine.setCurrentStock(newStock);
        medicineRepository.save(medicine);
    }

    @Transactional(readOnly = true)
    Medicine findMedicine(Long id) {
        return medicineRepository.findByIdWithDetails(id)
                .orElseGet(() -> medicineRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with id: " + id)));
    }

    private Medicine toEntity(DtoModels.MedicineRequest request) {
        Medicine.MedicineBuilder builder = Medicine.builder()
                .medicineCode(request.getMedicineCode())
                .medicineName(request.getMedicineName())
                .brand(request.getBrand())
                .batchNumber(request.getBatchNumber())
                .expiryDate(request.getExpiryDate())
                .manufacturingDate(request.getManufacturingDate())
                .hsnCode(request.getHsnCode())
                .gstPercent(request.getGstPercent() != null ? request.getGstPercent() : BigDecimal.ZERO)
                .purchasePrice(request.getPurchasePrice())
                .sellingPrice(request.getSellingPrice())
                .mrp(request.getMrp())
                .discountPercent(request.getDiscountPercent() != null ? request.getDiscountPercent() : BigDecimal.ZERO)
                .rackNumber(request.getRackNumber())
                .minimumStock(request.getMinimumStock() != null ? request.getMinimumStock() : 10)
                .currentStock(request.getCurrentStock() != null ? request.getCurrentStock() : 0)
                .barcode(request.getBarcode())
                .status(request.getStatus() != null ? request.getStatus() : MedicineStatus.ACTIVE);

        if (request.getCategoryId() != null) {
            Category category = categoryService.findCategory(request.getCategoryId());
            builder.category(category);
        }
        if (request.getSupplierId() != null) {
            Supplier supplier = supplierService.findSupplier(request.getSupplierId());
            builder.supplier(supplier);
        }
        return builder.build();
    }

    private void updateEntity(Medicine medicine, DtoModels.MedicineRequest request) {
        medicine.setMedicineName(request.getMedicineName());
        medicine.setBrand(request.getBrand());
        medicine.setBatchNumber(request.getBatchNumber());
        medicine.setExpiryDate(request.getExpiryDate());
        medicine.setManufacturingDate(request.getManufacturingDate());
        medicine.setHsnCode(request.getHsnCode());
        if (request.getGstPercent() != null) medicine.setGstPercent(request.getGstPercent());
        medicine.setPurchasePrice(request.getPurchasePrice());
        medicine.setSellingPrice(request.getSellingPrice());
        medicine.setMrp(request.getMrp());
        if (request.getDiscountPercent() != null) medicine.setDiscountPercent(request.getDiscountPercent());
        medicine.setRackNumber(request.getRackNumber());
        if (request.getMinimumStock() != null) medicine.setMinimumStock(request.getMinimumStock());
        if (request.getCurrentStock() != null) medicine.setCurrentStock(request.getCurrentStock());
        medicine.setBarcode(request.getBarcode());
        if (request.getStatus() != null) medicine.setStatus(request.getStatus());
        if (request.getCategoryId() != null) {
            medicine.setCategory(categoryService.findCategory(request.getCategoryId()));
        }
        if (request.getSupplierId() != null) {
            medicine.setSupplier(supplierService.findSupplier(request.getSupplierId()));
        }
    }

    DtoModels.MedicineResponse toResponse(Medicine medicine) {
        return DtoModels.MedicineResponse.builder()
                .id(medicine.getId())
                .medicineCode(medicine.getMedicineCode())
                .medicineName(medicine.getMedicineName())
                .categoryId(medicine.getCategory() != null ? medicine.getCategory().getId() : null)
                .categoryName(medicine.getCategory() != null ? medicine.getCategory().getName() : null)
                .brand(medicine.getBrand())
                .batchNumber(medicine.getBatchNumber())
                .expiryDate(medicine.getExpiryDate())
                .manufacturingDate(medicine.getManufacturingDate())
                .hsnCode(medicine.getHsnCode())
                .gstPercent(medicine.getGstPercent())
                .purchasePrice(medicine.getPurchasePrice())
                .sellingPrice(medicine.getSellingPrice())
                .mrp(medicine.getMrp())
                .discountPercent(medicine.getDiscountPercent())
                .rackNumber(medicine.getRackNumber())
                .minimumStock(medicine.getMinimumStock())
                .currentStock(medicine.getCurrentStock())
                .barcode(medicine.getBarcode())
                .supplierId(medicine.getSupplier() != null ? medicine.getSupplier().getId() : null)
                .supplierName(medicine.getSupplier() != null ? medicine.getSupplier().getSupplierName() : null)
                .status(medicine.getStatus())
                .build();
    }
}
