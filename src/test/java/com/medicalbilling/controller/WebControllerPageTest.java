package com.medicalbilling.controller;

import com.medicalbilling.entity.Category;
import com.medicalbilling.entity.Medicine;
import com.medicalbilling.entity.MedicineStatus;
import com.medicalbilling.repository.CategoryRepository;
import com.medicalbilling.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebControllerPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(Category.builder()
                .name("Tablets")
                .description("Tablet medicines")
                .build());

        medicineRepository.save(Medicine.builder()
                .medicineCode("MED001")
                .medicineName("Paracetamol")
                .category(category)
                .batchNumber("B001")
                .expiryDate(LocalDate.now().plusMonths(6))
                .purchasePrice(new BigDecimal("10.00"))
                .sellingPrice(new BigDecimal("15.00"))
                .minimumStock(5)
                .currentStock(2)
                .status(MedicineStatus.ACTIVE)
                .build());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void inventoryPageRenders() throws Exception {
        mockMvc.perform(get("/inventory"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void inventoryExpiredFilterRenders() throws Exception {
        mockMvc.perform(get("/inventory").param("filter", "EXPIRED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Expired Medicines List")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void expiredMedicinesPageRenders() throws Exception {
        mockMvc.perform(get("/expired-medicines"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Expired Medicines List")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void dashboardExpiredMedicinesLinkRenders() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/expired-medicines")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void nearExpiryMedicinesPageRenders() throws Exception {
        mockMvc.perform(get("/near-expiry-medicines"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Near Expiry Medicines")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void dashboardCustomersLinkRenders() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/customers")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void dashboardSuppliersLinkRenders() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/suppliers")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void suppliersPageRenders() throws Exception {
        mockMvc.perform(get("/suppliers"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Supplier Master")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void categoriesPageRenders() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk());
    }
}
