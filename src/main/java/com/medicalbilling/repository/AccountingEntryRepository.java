package com.medicalbilling.repository;

import com.medicalbilling.entity.AccountingEntry;
import com.medicalbilling.entity.AccountingEntryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AccountingEntryRepository extends JpaRepository<AccountingEntry, Long> {
    List<AccountingEntry> findByEntryDateBetween(LocalDate start, LocalDate end);
    List<AccountingEntry> findBySyncedFalse();
    List<AccountingEntry> findByEntryType(AccountingEntryType type);
    List<AccountingEntry> findByBranchId(Long branchId);
}
