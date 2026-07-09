package com.medicalbilling.repository;

import com.medicalbilling.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    Optional<Purchase> findByInvoiceNumber(String invoiceNumber);
    List<Purchase> findByPurchaseDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT SUM(p.grandTotal) FROM Purchase p WHERE p.purchaseDate BETWEEN :start AND :end")
    java.math.BigDecimal sumPurchasesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
