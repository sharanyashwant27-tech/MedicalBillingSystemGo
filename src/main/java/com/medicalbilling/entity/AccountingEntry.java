package com.medicalbilling.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String entryNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountingEntryType entryType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal debitAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal creditAmount;

    @Column(nullable = false)
    private LocalDate entryDate;

    private Long referenceId;

    @Column(length = 50)
    private String referenceType;

    @Column(nullable = false)
    @Builder.Default
    private boolean synced = false;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
