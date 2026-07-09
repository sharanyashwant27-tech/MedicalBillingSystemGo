package com.medicalbilling.service;

import com.medicalbilling.entity.*;
import com.medicalbilling.exception.ResourceNotFoundException;
import com.medicalbilling.repository.OnlineOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OnlineOrderService {

    private final OnlineOrderRepository onlineOrderRepository;
    private final CustomerService customerService;
    private final MedicineService medicineService;
    private final BranchService branchService;
    private final AuditService auditService;
    private final SmsNotificationService smsNotificationService;

    @Transactional(readOnly = true)
    public List<OnlineOrder> getAll() {
        return onlineOrderRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<OnlineOrder> getByStatus(OrderStatus status) {
        return onlineOrderRepository.findByStatusWithDetails(status);
    }

    @Transactional(readOnly = true)
    public OnlineOrder getById(Long id) {
        Long orderId = Objects.requireNonNull(id);
        return onlineOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    @Transactional
    public OnlineOrder create(Map<String, Object> request, String username) {
        Long customerId = Long.valueOf(request.get("customerId").toString());
        Customer customer = customerService.findCustomer(customerId);

        OnlineOrder order = OnlineOrder.builder()
                .orderNumber("ORD-" + System.currentTimeMillis())
                .customer(customer)
                .deliveryAddress((String) request.get("deliveryAddress"))
                .contactPhone((String) request.get("contactPhone"))
                .notes((String) request.get("notes"))
                .status(OrderStatus.PENDING)
                .build();

        if (request.get("branchId") != null) {
            order.setBranch(branchService.getById(Long.valueOf(request.get("branchId").toString())));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        BigDecimal total = BigDecimal.ZERO;

        for (Map<String, Object> itemReq : items) {
            Long medicineId = Long.valueOf(itemReq.get("medicineId").toString());
            int quantity = Integer.parseInt(itemReq.get("quantity").toString());
            Medicine medicine = medicineService.findMedicine(medicineId);
            BigDecimal subtotal = medicine.getSellingPrice().multiply(BigDecimal.valueOf(quantity));

            OnlineOrderItem item = OnlineOrderItem.builder()
                    .medicine(medicine)
                    .quantity(quantity)
                    .unitPrice(medicine.getSellingPrice())
                    .subtotal(subtotal)
                    .build();
            order.addItem(item);
            total = total.add(subtotal);
        }

        order.setTotalAmount(total);
        OnlineOrder saved = onlineOrderRepository.save(order);
        auditService.log("CREATE", "OnlineOrder", saved.getId(), username, "Created order: " + saved.getOrderNumber());

        if (customer.getPhone() != null) {
            smsNotificationService.sendSms(customer.getPhone(),
                    "Your order " + saved.getOrderNumber() + " has been placed. Total: Rs." + saved.getTotalAmount());
        }
        return saved;
    }

    @Transactional
    public OnlineOrder updateStatus(Long id, OrderStatus status, String username) {
        OnlineOrder order = getById(id);
        order.setStatus(status);
        OnlineOrder saved = onlineOrderRepository.save(order);
        auditService.log("UPDATE", "OnlineOrder", saved.getId(), username, "Status changed to " + status);

        if (order.getContactPhone() != null) {
            smsNotificationService.sendSms(order.getContactPhone(),
                    "Order " + order.getOrderNumber() + " status updated to: " + status);
        }
        return saved;
    }
}
