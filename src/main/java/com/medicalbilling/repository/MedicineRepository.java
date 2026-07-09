package com.medicalbilling.repository;

import com.medicalbilling.entity.Medicine;
import com.medicalbilling.entity.MedicineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    Optional<Medicine> findByMedicineCode(String medicineCode);
    Optional<Medicine> findByBarcode(String barcode);
    List<Medicine> findByMedicineNameContainingIgnoreCase(String name);
    List<Medicine> findByBatchNumberContainingIgnoreCase(String batch);
    List<Medicine> findByCurrentStockLessThanEqual(Integer stock);
    List<Medicine> findByCurrentStock(Integer stock);
    List<Medicine> findByExpiryDateBefore(LocalDate date);
    List<Medicine> findByExpiryDateBetween(LocalDate start, LocalDate end);
    List<Medicine> findByStatus(MedicineStatus status);
    boolean existsByMedicineCode(String medicineCode);
    boolean existsByBarcode(String barcode);

    @Query("SELECT DISTINCT m FROM Medicine m LEFT JOIN FETCH m.category LEFT JOIN FETCH m.supplier")
    List<Medicine> findAllWithDetails();

    @Query("SELECT DISTINCT m FROM Medicine m LEFT JOIN FETCH m.category LEFT JOIN FETCH m.supplier WHERE m.medicineName LIKE %:query% OR m.barcode = :query OR m.batchNumber LIKE %:query% OR m.medicineCode LIKE %:query%")
    List<Medicine> searchMedicinesWithDetails(@Param("query") String query);

    @Query("SELECT m FROM Medicine m LEFT JOIN FETCH m.category LEFT JOIN FETCH m.supplier WHERE m.id = :id")
    Optional<Medicine> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT m FROM Medicine m WHERE m.medicineName LIKE %:query% OR m.barcode = :query OR m.batchNumber LIKE %:query% OR m.medicineCode LIKE %:query%")
    List<Medicine> searchMedicines(@Param("query") String query);

    @Query("SELECT COUNT(m) FROM Medicine m WHERE m.status = 'ACTIVE'")
    long countActiveMedicines();

    @Query("SELECT m FROM Medicine m LEFT JOIN FETCH m.category LEFT JOIN FETCH m.supplier " +
           "WHERE m.status = 'ACTIVE' AND m.currentStock < :threshold " +
           "ORDER BY m.currentStock ASC, m.medicineName ASC")
    List<Medicine> findLowStockMedicinesWithDetails(@Param("threshold") int threshold);

    @Query("SELECT SUM(m.currentStock * m.purchasePrice) FROM Medicine m WHERE m.status = 'ACTIVE'")
    java.math.BigDecimal calculateInventoryValuation();
}
