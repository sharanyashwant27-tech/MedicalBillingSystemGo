package com.medicalbilling.repository;

import com.medicalbilling.entity.MedicineReturn;
import com.medicalbilling.entity.ReturnType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReturnRepository extends JpaRepository<MedicineReturn, Long> {
    Optional<MedicineReturn> findByReturnNumber(String returnNumber);
    List<MedicineReturn> findByReturnType(ReturnType returnType);
}
