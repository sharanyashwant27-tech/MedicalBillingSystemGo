package com.medicalbilling.repository;

import com.medicalbilling.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPendingTrue();
    List<Payment> findBySaleId(Long saleId);
}
