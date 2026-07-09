package com.medicalbilling.config;

import com.medicalbilling.entity.*;
import com.medicalbilling.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineRepository medicineRepository;
    private final CustomerRepository customerRepository;
    private final ShopSettingsRepository shopSettingsRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Value("${app.seed.pharmacist-password:}")
    private String pharmacistPassword;

    @Value("${app.seed.cashier-password:}")
    private String cashierPassword;

    @Override
    public void run(String... args) {
        initializeRoles();
        Branch mainBranch = initializeBranches();
        initializeAdminUser(mainBranch);
        initializeCategories();
        initializeSuppliers();
        initializeMedicines();
        initializeCustomers();
        initializeShopSettings();
        log.info("Database initialization completed");
    }

    private void initializeRoles() {
        for (RoleType roleType : RoleType.values()) {
            if (roleRepository.findByName(roleType).isEmpty()) {
                Role role = Objects.requireNonNull(Role.builder().name(roleType).build());
                roleRepository.save(role);
            }
        }
    }

    private Branch initializeBranches() {
        return branchRepository.findByBranchCode("MAIN").orElseGet(() -> {
            Branch branch = Objects.requireNonNull(Branch.builder()
                    .branchCode("MAIN")
                    .branchName("Main Branch")
                    .address("45 Main Street, Pune, Maharashtra")
                    .phone("020-12345678")
                    .email("main@healthcaremedical.com")
                    .city("Pune")
                    .state("Maharashtra")
                    .pinCode("411001")
                    .build());
            return branchRepository.save(branch);
        });
    }

    private void initializeAdminUser(Branch branch) {
        String adminPwd = resolveSeedPassword(adminPassword, "admin123");
        String pharmaPwd = resolveSeedPassword(pharmacistPassword, "pharma123");
        String cashierPwd = resolveSeedPassword(cashierPassword, "cashier123");

        Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN).orElseThrow();
        Role pharmacistRole = roleRepository.findByName(RoleType.ROLE_PHARMACIST).orElseThrow();
        Role cashierRole = roleRepository.findByName(RoleType.ROLE_CASHIER).orElseThrow();

        createOrUpdateDemoUser("admin", adminPwd, "System Administrator", "admin@medicalshop.com",
                "9876543210", Set.of(adminRole), branch);
        createOrUpdateDemoUser("pharmacist", pharmaPwd, "John Pharmacist", "pharmacist@medicalshop.com",
                null, Set.of(pharmacistRole), branch);
        createOrUpdateDemoUser("cashier", cashierPwd, "Jane Cashier", "cashier@medicalshop.com",
                null, Set.of(cashierRole), branch);
    }

    private String resolveSeedPassword(String configured, String defaultPassword) {
        return isPresent(configured) ? configured : defaultPassword;
    }

    private void createOrUpdateDemoUser(String username, String password, String fullName, String email,
                                        String phone, Set<Role> roles, Branch branch) {
        userRepository.findByUsername(username).ifPresentOrElse(user -> {
            user.setPassword(passwordEncoder.encode(password));
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setRoles(roles);
            user.setBranch(branch);
            user.setEnabled(true);
            user.setAccountNonLocked(true);
            userRepository.save(user);
        }, () -> userRepository.save(Objects.requireNonNull(User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .roles(roles)
                .branch(branch)
                .build())));
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private void initializeCategories() {
        String[] categories = {"Tablets", "Syrups", "Injections", "Ointments", "Capsules", "Medical Devices"};
        for (String name : categories) {
            categoryRepository.findByNameIgnoreCase(name).orElseGet(() -> {
                Category category = Objects.requireNonNull(
                        Category.builder().name(name).description(name + " category").build());
                return categoryRepository.save(category);
            });
        }
    }

    private void initializeSuppliers() {
        if (supplierRepository.count() == 0) {
            Supplier supplier = Objects.requireNonNull(Supplier.builder()
                    .supplierName("MediSupply Corp")
                    .gstNumber("27AABCU9603R1ZM")
                    .contactPerson("Rajesh Kumar")
                    .phone("9123456780")
                    .email("rajesh@medisupply.com")
                    .address("123 Industrial Area, Mumbai")
                    .state("Maharashtra")
                    .pinCode("400001")
                    .build());
            supplierRepository.save(supplier);
        }
    }

    private void initializeMedicines() {
        if (medicineRepository.count() == 0) {
            Category tablets = categoryRepository.findByNameIgnoreCase("Tablets").orElseThrow();
            Supplier supplier = supplierRepository.findAll().get(0);

            Medicine paracetamol = Objects.requireNonNull(Medicine.builder()
                    .medicineCode("MED-001001")
                    .medicineName("Paracetamol 500mg")
                    .category(tablets)
                    .brand("Crocin")
                    .batchNumber("BATCH001")
                    .expiryDate(LocalDate.now().plusMonths(12))
                    .manufacturingDate(LocalDate.now().minusMonths(2))
                    .hsnCode("3004")
                    .gstPercent(new BigDecimal("12"))
                    .purchasePrice(new BigDecimal("2.50"))
                    .sellingPrice(new BigDecimal("5.00"))
                    .mrp(new BigDecimal("6.00"))
                    .discountPercent(BigDecimal.ZERO)
                    .rackNumber("A1")
                    .minimumStock(50)
                    .currentStock(200)
                    .barcode("8901234567890")
                    .supplier(supplier)
                    .status(MedicineStatus.ACTIVE)
                    .build());
            medicineRepository.save(paracetamol);

            Medicine amoxicillin = Objects.requireNonNull(Medicine.builder()
                    .medicineCode("MED-001002")
                    .medicineName("Amoxicillin 250mg")
                    .category(tablets)
                    .brand("Mox")
                    .batchNumber("BATCH002")
                    .expiryDate(LocalDate.now().plusMonths(6))
                    .manufacturingDate(LocalDate.now().minusMonths(1))
                    .hsnCode("3004")
                    .gstPercent(new BigDecimal("12"))
                    .purchasePrice(new BigDecimal("8.00"))
                    .sellingPrice(new BigDecimal("15.00"))
                    .mrp(new BigDecimal("18.00"))
                    .rackNumber("A2")
                    .minimumStock(30)
                    .currentStock(5)
                    .barcode("8901234567891")
                    .supplier(supplier)
                    .status(MedicineStatus.ACTIVE)
                    .build());
            medicineRepository.save(amoxicillin);
        }
    }

    private void initializeCustomers() {
        if (customerRepository.count() == 0) {
            Customer customer = Objects.requireNonNull(Customer.builder()
                    .customerName("Walk-in Customer")
                    .phone("9999999999")
                    .build());
            customerRepository.save(customer);
        }
    }

    private void initializeShopSettings() {
        if (shopSettingsRepository.count() == 0) {
            ShopSettings settings = Objects.requireNonNull(ShopSettings.builder()
                    .shopName("HealthCare Medical Store")
                    .gstNumber("27AABCU9603R1ZM")
                    .address("45 Main Street, Pune, Maharashtra - 411001")
                    .phone("020-12345678")
                    .email("info@healthcaremedical.com")
                    .invoiceFooter("Thank you for your purchase! Get well soon.")
                    .defaultGstPercent(new BigDecimal("12.00"))
                    .build());
            shopSettingsRepository.save(settings);
        }
    }
}
