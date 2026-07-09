package com.medicalbilling.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shop_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    @Builder.Default
    private String shopName = "Medical Shop";

    @Column(length = 500)
    private String logoPath;

    @Column(length = 20)
    private String gstNumber;

    @Column(length = 500)
    private String address;

    @Column(length = 15)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 1000)
    private String invoiceFooter;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal defaultGstPercent = new BigDecimal("12.00");

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
