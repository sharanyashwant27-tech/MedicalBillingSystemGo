package com.medicalbilling.service;

import com.medicalbilling.entity.Customer;
import com.medicalbilling.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LoyaltyServiceTest {

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @Transactional
    void redeemPointsReducesBalance() {
        Customer customer = customerRepository.save(Customer.builder()
                .customerName("Loyalty Test")
                .loyaltyPoints(100)
                .build());

        loyaltyService.redeemPoints(customer.getId(), 30, "admin");
        int balance = loyaltyService.getBalance(customer.getId());
        assertEquals(70, balance);
    }
}
