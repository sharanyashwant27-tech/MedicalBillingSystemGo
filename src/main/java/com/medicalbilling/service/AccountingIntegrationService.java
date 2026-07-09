package com.medicalbilling.service;

import com.medicalbilling.entity.*;
import com.medicalbilling.repository.AccountingEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountingIntegrationService {

    private final AccountingEntryRepository accountingEntryRepository;
    private final AuditService auditService;

    @Value("${app.accounting.enabled:true}")
    private boolean accountingEnabled;

    @Transactional
    public AccountingEntry recordSale(Sale sale) {
        if (!accountingEnabled) return null;
        AccountingEntry entry = Objects.requireNonNull(AccountingEntry.builder()
                .entryNumber("ACC-" + System.currentTimeMillis())
                .entryType(AccountingEntryType.SALE)
                .branch(sale.getBranch())
                .description("Sale " + sale.getBillNumber())
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(sale.getGrandTotal())
                .entryDate(LocalDate.now())
                .referenceId(sale.getId())
                .referenceType("SALE")
                .build());
        return accountingEntryRepository.save(entry);
    }

    @Transactional
    public AccountingEntry recordPurchase(Purchase purchase) {
        if (!accountingEnabled) return null;
        AccountingEntry entry = Objects.requireNonNull(AccountingEntry.builder()
                .entryNumber("ACC-" + System.currentTimeMillis())
                .entryType(AccountingEntryType.PURCHASE)
                .description("Purchase " + purchase.getInvoiceNumber())
                .debitAmount(purchase.getGrandTotal())
                .creditAmount(BigDecimal.ZERO)
                .entryDate(purchase.getPurchaseDate())
                .referenceId(purchase.getId())
                .referenceType("PURCHASE")
                .build());
        return accountingEntryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<AccountingEntry> getEntries(LocalDate start, LocalDate end) {
        return accountingEntryRepository.findByEntryDateBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<AccountingEntry> getUnsynced() {
        return accountingEntryRepository.findBySyncedFalse();
    }

    @Transactional
    public Map<String, Object> exportForAccounting(LocalDate start, LocalDate end, String username) {
        List<AccountingEntry> entries = getEntries(start, end);
        BigDecimal totalDebit = entries.stream()
                .map(entry -> entry.getDebitAmount())
                .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
        BigDecimal totalCredit = entries.stream()
                .map(entry -> entry.getCreditAmount())
                .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));

        entries.forEach(e -> e.setSynced(true));
        accountingEntryRepository.saveAll(entries);

        auditService.log("EXPORT", "Accounting", null, username, "Exported " + entries.size() + " entries");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entries", entries);
        result.put("totalDebit", totalDebit);
        result.put("totalCredit", totalCredit);
        result.put("balance", totalCredit.subtract(totalDebit));
        result.put("exportedAt", LocalDate.now());
        return result;
    }
}
