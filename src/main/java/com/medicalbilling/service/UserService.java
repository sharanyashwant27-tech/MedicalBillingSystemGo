package com.medicalbilling.service;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.entity.Role;
import com.medicalbilling.entity.RoleType;
import com.medicalbilling.entity.User;
import com.medicalbilling.exception.BusinessException;
import com.medicalbilling.exception.ResourceNotFoundException;
import com.medicalbilling.repository.RoleRepository;
import com.medicalbilling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public List<DtoModels.UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public DtoModels.UserResponse getUserById(Long id) {
        return toResponse(findUser(id));
    }

    @Transactional
    public DtoModels.UserResponse createUser(DtoModels.UserRequest request, String adminUsername) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }
        Set<Role> roles = resolveRoles(request.getRoles());
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .roles(roles)
                .build();
        User saved = userRepository.save(Objects.requireNonNull(user));
        auditService.log("CREATE", "User", saved.getId(), adminUsername, "Created user: " + saved.getUsername());
        return toResponse(saved);
    }

    @Transactional
    public DtoModels.UserResponse updateUser(Long id, DtoModels.UserRequest request, String adminUsername) {
        User user = findUser(id);
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRoles(resolveRoles(request.getRoles()));
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        User saved = userRepository.save(Objects.requireNonNull(user));
        auditService.log("UPDATE", "User", saved.getId(), adminUsername, "Updated user: " + saved.getUsername());
        return toResponse(saved);
    }

    @Transactional
    public void deleteUser(Long id, String adminUsername) {
        User user = Objects.requireNonNull(findUser(id));
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException("Cannot delete default admin user");
        }
        userRepository.delete(user);
        auditService.log("DELETE", "User", id, adminUsername, "Deleted user: " + user.getUsername());
    }

    @Transactional
    public void resetPassword(Long id, String newPassword, String adminUsername) {
        User user = findUser(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(Objects.requireNonNull(user));
        auditService.log("RESET_PASSWORD", "User", id, adminUsername, "Reset password for: " + user.getUsername());
    }

    @Transactional
    public void lockUser(Long id, String adminUsername) {
        User user = findUser(id);
        user.setAccountNonLocked(false);
        userRepository.save(Objects.requireNonNull(user));
        auditService.log("LOCK", "User", id, adminUsername, "Locked user: " + user.getUsername());
    }

    @Transactional
    public void unlockUser(Long id, String adminUsername) {
        User user = findUser(id);
        user.setAccountNonLocked(true);
        userRepository.save(Objects.requireNonNull(user));
        auditService.log("UNLOCK", "User", id, adminUsername, "Unlocked user: " + user.getUsername());
    }

    private User findUser(Long id) {
        Long userId = Objects.requireNonNull(id);
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Set<Role> resolveRoles(List<RoleType> roleTypes) {
        Set<Role> roles = new HashSet<>();
        for (RoleType roleType : roleTypes) {
            Role role = roleRepository.findByName(roleType)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleType));
            roles.add(role);
        }
        return roles;
    }

    private DtoModels.UserResponse toResponse(User user) {
        return DtoModels.UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()))
                .enabled(user.isEnabled())
                .accountNonLocked(user.isAccountNonLocked())
                .build();
    }
}
