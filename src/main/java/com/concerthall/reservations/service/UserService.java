package com.concerthall.reservations.service;

import com.concerthall.reservations.domain.User;
import com.concerthall.reservations.domain.enums.UserRole;
import com.concerthall.reservations.dto.request.CreatePowerUserRequest;
import com.concerthall.reservations.dto.response.UserResponse;
import com.concerthall.reservations.exception.ResourceNotFoundException;
import com.concerthall.reservations.exception.UserNotRemovableException;
import com.concerthall.reservations.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(final String email) {
        final User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    @Transactional
    public UserResponse createPowerUser(final CreatePowerUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        final User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.POWER_USER)
                .isRemovable(true)
                .build();

        final User savedUser = userRepository.save(user);
        log.info("Power user created: {}", savedUser.getEmail());

        return toResponse(savedUser);
    }

    @Transactional
    public void deleteUser(final UUID id) {
        final User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getIsRemovable()) {
            throw new UserNotRemovableException("This user cannot be removed");
        }

        userRepository.delete(user);
        log.info("User deleted: {}", user.getEmail());
    }

    private UserResponse toResponse(final User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isRemovable(user.getIsRemovable())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
