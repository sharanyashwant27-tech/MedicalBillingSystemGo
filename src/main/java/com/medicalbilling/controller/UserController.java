package com.medicalbilling.controller;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<DtoModels.UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<DtoModels.UserResponse> create(@Valid @RequestBody DtoModels.UserRequest request,
                                                           @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(userService.createUser(request, user.getUsername()));
    }

    @PutMapping("/update")
    public ResponseEntity<DtoModels.UserResponse> update(@Valid @RequestBody DtoModels.UserUpdateRequest request,
                                                         @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(userService.updateUser(request.getUserId(), toUserRequest(request), user.getUsername()));
    }
    @PostMapping("/delete")
    public ResponseEntity<Map<String, String>> delete(@RequestBody Map<String, Long> body,
                                                       @AuthenticationPrincipal UserDetails user) {
        userService.deleteUser(body.get("userId"), user.getUsername());
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, Object> body,
                                                              @AuthenticationPrincipal UserDetails user) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String password = (String) body.get("password");
        userService.resetPassword(userId, password, user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PostMapping("/lock")
    public ResponseEntity<Map<String, String>> lock(@RequestBody Map<String, Long> body,
                                                    @AuthenticationPrincipal UserDetails user) {
        userService.lockUser(body.get("userId"), user.getUsername());
        return ResponseEntity.ok(Map.of("message", "User locked successfully"));
    }

    @PostMapping("/unlock")
    public ResponseEntity<Map<String, String>> unlock(@RequestBody Map<String, Long> body,
                                                      @AuthenticationPrincipal UserDetails user) {
        userService.unlockUser(body.get("userId"), user.getUsername());
        return ResponseEntity.ok(Map.of("message", "User unlocked successfully"));
    }

    private DtoModels.UserRequest toUserRequest(DtoModels.UserUpdateRequest request) {
        return DtoModels.UserRequest.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .roles(request.getRoles())
                .build();
    }
}
