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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void categoriesPageRenders() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk());
    }
}
