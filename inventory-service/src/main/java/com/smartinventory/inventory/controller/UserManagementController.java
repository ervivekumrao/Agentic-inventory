package com.smartinventory.inventory.controller;

import com.smartinventory.inventory.model.RoleEntity;
import com.smartinventory.inventory.model.UserEntity;
import com.smartinventory.inventory.repository.RoleRepository;
import com.smartinventory.inventory.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class UserManagementController {

    private static final Logger log = LoggerFactory.getLogger(UserManagementController.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        log.debug("GET /api/admin/users requested");
        List<UserResponseDto> users = userRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or @userRepository.count() == 0")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.debug("POST /api/admin/users requested for username: {}", request.username());
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        UserEntity user = new UserEntity(request.username(), passwordEncoder.encode(request.password()));
        if (request.roles() != null && !request.roles().isEmpty()) {
            Set<RoleEntity> roles = request.roles().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseGet(() -> roleRepository.save(new RoleEntity(roleName, "Role " + roleName))))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        UserEntity savedUser = userRepository.save(user);
        log.info("User created successfully: {}", savedUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(savedUser));
    }

    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> updateUserRoles(@PathVariable Long userId, @RequestBody Set<String> roleNames) {
        log.debug("PUT /api/admin/users/{}/roles requested", userId);
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Set<RoleEntity> roles = roleNames.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName)))
                .collect(Collectors.toSet());

        user.setRoles(roles);
        UserEntity updatedUser = userRepository.save(user);
        log.info("Roles updated successfully for user: {}", updatedUser.getUsername());
        return ResponseEntity.ok(toDto(updatedUser));
    }

    @PutMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> updateUserStatus(@PathVariable Long userId, @RequestParam boolean enabled) {
        log.debug("PUT /api/admin/users/{}/status requested with enabled={}", userId, enabled);
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setEnabled(enabled);
        UserEntity updatedUser = userRepository.save(user);
        log.info("User status updated for {}: enabled={}", updatedUser.getUsername(), enabled);
        return ResponseEntity.ok(toDto(updatedUser));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleEntity>> getAllRoles() {
        log.debug("GET /api/admin/roles requested");
        return ResponseEntity.ok(roleRepository.findAll());
    }

    private UserResponseDto toDto(UserEntity user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());
        return new UserResponseDto(user.getId(), user.getUsername(), user.isEnabled(), roleNames, user.getCreatedAt().toString());
    }

    public record CreateUserRequest(
            @NotBlank(message = "Username is mandatory") String username,
            @NotBlank(message = "Password is mandatory") String password,
            Set<String> roles
    ) {}

    public record UserResponseDto(
            Long id,
            String username,
            boolean enabled,
            Set<String> roles,
            String createdAt
    ) {}
}
