package com.concerthall.reservations.controller;

import com.concerthall.reservations.dto.request.CreatePowerUserRequest;
import com.concerthall.reservations.dto.response.UserResponse;
import com.concerthall.reservations.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'POWER_USER')")
    @Operation(summary = "Get all users (Admin/Power User only)")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        final List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal final UserDetails userDetails) {
        final UserResponse user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/power-user")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create power user (Admin only)")
    public ResponseEntity<UserResponse> createPowerUser(@Valid @RequestBody final CreatePowerUserRequest request) {
        final UserResponse user = userService.createPowerUser(request);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user if removable (Admin only)")
    public ResponseEntity<Void> deleteUser(@PathVariable final UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
