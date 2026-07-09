package com.medicalbilling.service;

import com.medicalbilling.entity.Role;
import com.medicalbilling.entity.RoleType;
import com.medicalbilling.entity.User;
import com.medicalbilling.repository.RoleRepository;
import com.medicalbilling.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Test
    @Transactional
    void loginWithValidCredentials() {
        Role role = roleRepository.save(Role.builder().name(RoleType.ROLE_ADMIN).build());
        userRepository.save(User.builder()
                .username("testuser")
                .password(passwordEncoder.encode("password123"))
                .fullName("Test User")
                .roles(Set.of(role))
                .build());

        var request = new com.medicalbilling.dto.LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        var response = authService.login(request);
        assertNotNull(response.getToken());
        assertEquals("testuser", response.getUsername());
    }
}
