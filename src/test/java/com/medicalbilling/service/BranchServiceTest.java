package com.medicalbilling.service;

import com.medicalbilling.entity.Branch;
import com.medicalbilling.repository.BranchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BranchServiceTest {

    @Autowired
    private BranchService branchService;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    @Transactional
    void createAndRetrieveBranch() {
        Branch branch = Branch.builder()
                .branchCode("TST01")
                .branchName("Test Branch")
                .city("Mumbai")
                .phone("9999999999")
                .build();

        Branch saved = branchService.create(branch, "admin");
        assertNotNull(saved.getId());
        assertEquals("TST01", saved.getBranchCode());

        Branch found = branchService.getById(saved.getId());
        assertEquals("Test Branch", found.getBranchName());
    }
}
