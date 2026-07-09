package com.medicalbilling.service;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.entity.Customer;
import com.medicalbilling.entity.Sale;
import com.medicalbilling.exception.ResourceNotFoundException;
import com.medicalbilling.repository.CustomerRepository;
import com.medicalbilling.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final AuditService auditService;

    public List<DtoModels.CustomerResponse> getAll(String search) {
        List<Customer> customers = (search == null || search.isBlank())
                ? customerRepository.findAll()
                : customerRepository.findByCustomerNameContainingIgnoreCase(search);
        return customers.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public DtoModels.CustomerResponse getById(Long id) {
        return toResponse(findCustomer(id));
    }

    public Map<String, Object> getCustomerHistory(Long id) {
        Customer customer = findCustomer(id);
        List<Sale> sales = saleRepository.findAll().stream()
                .filter(s -> s.getCustomer() != null && s.getCustomer().getId().equals(id))
                .collect(Collectors.toList());
        Map<String, Object> history = new HashMap<>();
        history.put("customer", toResponse(customer));
        history.put("totalPurchases", sales.size());
        history.put("sales", sales.stream().map(s -> DtoModels.SaleSummary.builder()
                .id(s.getId())
                .billNumber(s.getBillNumber())
                .grandTotal(s.getGrandTotal())
                .saleDate(s.getSaleDate())
                .paymentMode(s.getPaymentMode())
                .build()).collect(Collectors.toList()));
        return history;
    }

    @Transactional
    public DtoModels.CustomerResponse create(DtoModels.CustomerRequest request, String username) {
        Customer saved = customerRepository.save(toEntity(request));
        auditService.log("CREATE", "Customer", saved.getId(), username, "Created customer: " + saved.getCustomerName());
        return toResponse(saved);
    }

    @Transactional
    public DtoModels.CustomerResponse update(Long id, DtoModels.CustomerRequest request, String username) {
        Customer customer = findCustomer(id);
        updateEntity(customer, request);
        Customer saved = customerRepository.save(customer);
        auditService.log("UPDATE", "Customer", saved.getId(), username, "Updated customer: " + saved.getCustomerName());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, String username) {
        Customer customer = findCustomer(id);
        customerRepository.delete(customer);
        auditService.log("DELETE", "Customer", id, username, "Deleted customer: " + customer.getCustomerName());
    }

    Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    private Customer toEntity(DtoModels.CustomerRequest request) {
        return Customer.builder()
                .customerName(request.getCustomerName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .age(request.getAge())
                .gender(request.getGender())
                .doctorName(request.getDoctorName())
                .gstNumber(request.getGstNumber())
                .build();
    }

    private void updateEntity(Customer customer, DtoModels.CustomerRequest request) {
        customer.setCustomerName(request.getCustomerName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setAge(request.getAge());
        customer.setGender(request.getGender());
        customer.setDoctorName(request.getDoctorName());
        customer.setGstNumber(request.getGstNumber());
    }

    private DtoModels.CustomerResponse toResponse(Customer customer) {
        return DtoModels.CustomerResponse.builder()
                .id(customer.getId())
                .customerName(customer.getCustomerName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .age(customer.getAge())
                .gender(customer.getGender())
                .doctorName(customer.getDoctorName())
                .gstNumber(customer.getGstNumber())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .build();
    }
}
