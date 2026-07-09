package com.medicalbilling.repository;

import com.medicalbilling.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findBySupplierNameContainingIgnoreCase(String name);
}
