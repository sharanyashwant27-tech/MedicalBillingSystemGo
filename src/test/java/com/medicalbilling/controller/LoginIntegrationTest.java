package com.medicalbilling.controller;

import com.medicalbilling.entity.Role;
import com.medicalbilling.entity.RoleType;
import com.medicalbilling.entity.User;
import com.medicalbilling.repository.RoleRepository;
import com.medicalbilling.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_ADMIN).build()));

        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Administrator")
                    .email("admin@medicalshop.com")
                    .roles(Set.of(adminRole))
                    .build());
        }
    }

    @Test
    void loginPageLoads() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void formLoginSucceedsWithCsrf() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "admin123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void formLoginFailsWithoutCsrf() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().isForbidden());
    }
}
