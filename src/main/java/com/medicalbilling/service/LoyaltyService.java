package com.medicalbilling.service;

import com.medicalbilling.entity.*;
import com.medicalbilling.repository.LoyaltyTransactionRepository;
import com.medicalbilling.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private static final int POINTS_PER_100_RUPEES = 1;

    private final CustomerRepository customerRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final AuditService auditService;

    @Transactional
    public void earnPointsFromSale(Sale sale) {
        if (sale.getCustomer() == null) return;
        Long customerId = sale.getCustomer().getId();
        if (customerId == null) return;
        Customer customer = customerRepository.findById(Objects.requireNonNull(customerId)).orElse(null);
        if (customer == null) return;

        int points = sale.getGrandTotal().intValue() / 100 * POINTS_PER_100_RUPEES;
        if (points <= 0) return;

        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + points);
        customerRepository.save(customer);

        loyaltyTransactionRepository.save(Objects.requireNonNull(LoyaltyTransaction.builder()
                .customer(customer)
                .sale(sale)
                .points(points)
                .transactionType("EARN")
                .description("Earned from bill " + sale.getBillNumber())
                .build()));
    }

    @Transactional
    public void redeemPoints(Long customerId, int points, String username) {
        Customer customer = customerRepository.findById(Objects.requireNonNull(customerId))
                .orElseThrow(() -> new com.medicalbilling.exception.ResourceNotFoundException("Customer not found"));
        if (customer.getLoyaltyPoints() < points) {
            throw new com.medicalbilling.exception.BusinessException("Insufficient loyalty points");
        }
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - points);
        customerRepository.save(customer);

        loyaltyTransactionRepository.save(Objects.requireNonNull(LoyaltyTransaction.builder()
                .customer(customer)
                .points(-points)
                .transactionType("REDEEM")
                .description("Redeemed " + points + " points")
                .build()));
        auditService.log("REDEEM", "Loyalty", customerId, username, "Redeemed " + points + " loyalty points");
    }

    @Transactional(readOnly = true)
    public List<LoyaltyTransaction> getHistory(Long customerId) {
        return loyaltyTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public int getBalance(Long customerId) {
        return customerRepository.findById(Objects.requireNonNull(customerId))
                .map(customer -> customer.getLoyaltyPoints())
                .orElse(0);
    }
}
