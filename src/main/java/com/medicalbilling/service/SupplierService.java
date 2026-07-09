package com.medicalbilling.service;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.entity.Supplier;
import com.medicalbilling.exception.ResourceNotFoundException;
import com.medicalbilling.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final AuditService auditService;

    public List<DtoModels.SupplierResponse> getAll(String search) {
        List<Supplier> suppliers = (search == null || search.isBlank())
                ? supplierRepository.findAll()
                : supplierRepository.findBySupplierNameContainingIgnoreCase(search);
        return suppliers.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public DtoModels.SupplierResponse getById(Long id) {
        return toResponse(findSupplier(id));
    }

    @Transactional
    public DtoModels.SupplierResponse create(DtoModels.SupplierRequest request, String username) {
        Supplier saved = supplierRepository.save(toEntity(request));
        auditService.log("CREATE", "Supplier", saved.getId(), username, "Created supplier: " + saved.getSupplierName());
        return toResponse(saved);
    }

    @Transactional
    public DtoModels.SupplierResponse update(Long id, DtoModels.SupplierRequest request, String username) {
        Supplier supplier = findSupplier(id);
        updateEntity(supplier, request);
        Supplier saved = supplierRepository.save(supplier);
        auditService.log("UPDATE", "Supplier", saved.getId(), username, "Updated supplier: " + saved.getSupplierName());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, String username) {
        Supplier supplier = findSupplier(id);
        supplierRepository.delete(supplier);
        auditService.log("DELETE", "Supplier", id, username, "Deleted supplier: " + supplier.getSupplierName());
    }

    Supplier findSupplier(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
    }

    private Supplier toEntity(DtoModels.SupplierRequest request) {
        return Supplier.builder()
                .supplierName(request.getSupplierName())
                .gstNumber(request.getGstNumber())
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .state(request.getState())
                .pinCode(request.getPinCode())
                .outstandingBalance(BigDecimal.ZERO)
                .build();
    }

    private void updateEntity(Supplier supplier, DtoModels.SupplierRequest request) {
        supplier.setSupplierName(request.getSupplierName());
        supplier.setGstNumber(request.getGstNumber());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setState(request.getState());
        supplier.setPinCode(request.getPinCode());
    }

    private DtoModels.SupplierResponse toResponse(Supplier supplier) {
        return DtoModels.SupplierResponse.builder()
                .id(supplier.getId())
                .supplierName(supplier.getSupplierName())
                .gstNumber(supplier.getGstNumber())
                .contactPerson(supplier.getContactPerson())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .state(supplier.getState())
                .pinCode(supplier.getPinCode())
                .outstandingBalance(supplier.getOutstandingBalance())
                .build();
    }
}
