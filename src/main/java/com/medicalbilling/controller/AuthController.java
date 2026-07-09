package com.medicalbilling.controller;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.dto.JwtResponse;
import com.medicalbilling.dto.LoginRequest;
import com.medicalbilling.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<DtoModels.ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse response = authService.login(request);
        return ResponseEntity.ok(DtoModels.ApiResponse.<JwtResponse>builder()
                .success(true)
                .message("Login successful")
                .data(response)
                .build());
    }
}
