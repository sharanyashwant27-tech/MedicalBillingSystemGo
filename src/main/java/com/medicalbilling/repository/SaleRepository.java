package com.medicalbilling.repository;

import com.medicalbilling.entity.Sale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    Optional<Sale> findByBillNumber(String billNumber);

    List<Sale> findTop10ByOrderBySaleDateDesc();

    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.customer")
    List<Sale> findRecentWithCustomer(Pageable pageable);

    @Query("SELECT DISTINCT s FROM Sale s LEFT JOIN FETCH s.items i LEFT JOIN FETCH i.medicine WHERE s.id = :id")
    Optional<Sale> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT SUM(s.grandTotal) FROM Sale s WHERE s.saleDate BETWEEN :start AND :end")
    java.math.BigDecimal sumSalesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT DISTINCT s FROM Sale s LEFT JOIN FETCH s.items i LEFT JOIN FETCH i.medicine WHERE s.saleDate BETWEEN :start AND :end ORDER BY s.saleDate DESC")
    List<Sale> findSalesBetweenWithDetails(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT s FROM Sale s WHERE s.saleDate BETWEEN :start AND :end ORDER BY s.saleDate DESC")
    List<Sale> findSalesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
