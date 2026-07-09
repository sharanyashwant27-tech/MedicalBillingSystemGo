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

    @GetMapping("/{id}")
    public ResponseEntity<DtoModels.UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<DtoModels.UserResponse> create(@Valid @RequestBody DtoModels.UserRequest request,
                                                           @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(userService.createUser(request, user.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DtoModels.UserResponse> update(@PathVariable Long id,
                                                         @Valid @RequestBody DtoModels.UserRequest request,
                                                         @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(userService.updateUser(id, request, user.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserDetails user) {
        userService.deleteUser(id, user.getUsername());
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable Long id,
                                                              @RequestBody Map<String, String> body,
                                                              @AuthenticationPrincipal UserDetails user) {
        userService.resetPassword(id, body.get("password"), user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<Map<String, String>> lock(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserDetails user) {
        userService.lockUser(id, user.getUsername());
        return ResponseEntity.ok(Map.of("message", "User locked successfully"));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<Map<String, String>> unlock(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserDetails user) {
        userService.unlockUser(id, user.getUsername());
        return ResponseEntity.ok(Map.of("message", "User unlocked successfully"));
    }
}
