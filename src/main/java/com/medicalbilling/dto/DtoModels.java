package com.medicalbilling.dto;

import com.medicalbilling.entity.Gender;
import com.medicalbilling.entity.MedicineStatus;
import com.medicalbilling.entity.PaymentMode;
import com.medicalbilling.entity.ReturnType;
import com.medicalbilling.entity.RoleType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DtoModels {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserRequest {
        @NotBlank private String username;
        @NotBlank @Size(min = 6) private String password;
        @NotBlank private String fullName;
        private String email;
        private String phone;
        @NotEmpty private List<RoleType> roles;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String username;
        private String fullName;
        private String email;
        private String phone;
        private List<String> roles;
        private boolean enabled;
        private boolean accountNonLocked;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserUpdateRequest {
        @NotNull private Long userId;
        @NotBlank private String username;
        private String password;
        @NotBlank private String fullName;
        private String email;
        private String phone;
        @NotEmpty private List<RoleType> roles;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoryRequest {
        @NotBlank private String name;
        private String description;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoryResponse {
        private Long id;
        private String name;
        private String description;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SupplierRequest {
        @NotBlank private String supplierName;
        private String gstNumber;
        private String contactPerson;
        private String phone;
        private String email;
        private String address;
        private String state;
        private String pinCode;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SupplierResponse {
        private Long id;
        private String supplierName;
        private String gstNumber;
        private String contactPerson;
        private String phone;
        private String email;
        private String address;
        private String state;
        private String pinCode;
        private BigDecimal outstandingBalance;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CustomerRequest {
        @NotBlank private String customerName;
        private String phone;
        private String email;
        private String address;
        private Integer age;
        private Gender gender;
        private String doctorName;
        private String gstNumber;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CustomerResponse {
        private Long id;
        private String customerName;
        private String phone;
        private String email;
        private String address;
        private Integer age;
        private Gender gender;
        private String doctorName;
        private String gstNumber;
        private Integer loyaltyPoints;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MedicineRequest {
        @NotBlank private String medicineCode;
        @NotBlank private String medicineName;
        private Long categoryId;
        private String brand;
        private String batchNumber;
        private LocalDate expiryDate;
        private LocalDate manufacturingDate;
        private String hsnCode;
        private BigDecimal gstPercent;
        @NotNull private BigDecimal purchasePrice;
        @NotNull private BigDecimal sellingPrice;
        private BigDecimal mrp;
        private BigDecimal discountPercent;
        private String rackNumber;
        private Integer minimumStock;
        private Integer currentStock;
        private String barcode;
        private Long supplierId;
        private MedicineStatus status;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MedicineResponse {
        private Long id;
        private String medicineCode;
        private String medicineName;
        private String categoryName;
        private Long categoryId;
        private String brand;
        private String batchNumber;
        private LocalDate expiryDate;
        private LocalDate manufacturingDate;
        private String hsnCode;
        private BigDecimal gstPercent;
        private BigDecimal purchasePrice;
        private BigDecimal sellingPrice;
        private BigDecimal mrp;
        private BigDecimal discountPercent;
        private String rackNumber;
        private Integer minimumStock;
        private Integer currentStock;
        private String barcode;
        private String supplierName;
        private Long supplierId;
        private MedicineStatus status;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PurchaseItemRequest {
        @NotNull private Long medicineId;
        @NotNull @Min(1) private Integer quantity;
        @NotNull private BigDecimal purchasePrice;
        private BigDecimal gstAmount;
        private LocalDate expiryDate;
        private String batchNumber;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PurchaseRequest {
        @NotBlank private String invoiceNumber;
        @NotNull private Long supplierId;
        @NotNull private LocalDate purchaseDate;
        @NotEmpty private List<PurchaseItemRequest> items;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SaleItemRequest {
        @NotNull private Long medicineId;
        @NotNull @Min(1) private Integer quantity;
        private BigDecimal discountPercent;
        private String batchNumber;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SaleRequest {
        private Long customerId;
        @NotNull private PaymentMode paymentMode;
        @NotNull private BigDecimal amountPaid;
        @NotEmpty private List<SaleItemRequest> items;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReturnRequest {
        @NotNull private ReturnType returnType;
        @NotNull private Long medicineId;
        private Long saleId;
        private Long purchaseId;
        @NotNull @Min(1) private Integer quantity;
        @NotBlank private String reason;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DashboardResponse {
        private BigDecimal todaySales;
        private BigDecimal todayProfit;
        private long availableMedicines;
        private long lowStockMedicines;
        private long expiredMedicines;
        private long totalCustomers;
        private long totalSuppliers;
        private long pendingPayments;
        private List<SaleSummary> recentBills;
        private List<AlertItem> alerts;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SaleSummary {
        private Long id;
        private String billNumber;
        private String customerName;
        private BigDecimal grandTotal;
        private LocalDateTime saleDate;
        private PaymentMode paymentMode;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AlertItem {
        private String type;
        private String message;
        private String severity;
        private Long medicineId;
        private String medicineCode;
        private String medicineName;
        private Integer currentStock;
        private Integer minimumStock;
        private Integer shortage;
        private String categoryName;
        private String supplierName;
        private String supplierPhone;
        private String batchNumber;
        private java.time.LocalDate expiryDate;
        private String rackNumber;
        private String stockStatus;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InventorySummary {
        private long totalMedicines;
        private long lowStock;
        private long outOfStock;
        private long nearExpiry;
        private long expired;
        private BigDecimal valuation;
        private List<MedicineResponse> items;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ShopSettingsRequest {
        private String shopName;
        private String gstNumber;
        private String address;
        private String phone;
        private String email;
        private String invoiceFooter;
        private BigDecimal defaultGstPercent;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReportRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        private String reportType;
    }
}
