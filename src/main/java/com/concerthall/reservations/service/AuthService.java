package com.concerthall.reservations.service;

import com.concerthall.reservations.domain.User;
import com.concerthall.reservations.domain.enums.UserRole;
import com.concerthall.reservations.dto.request.LoginRequest;
import com.concerthall.reservations.dto.request.RegisterRequest;
import com.concerthall.reservations.dto.response.AuthResponse;
import com.concerthall.reservations.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(final RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        final User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .isRemovable(true)
                .build();

        final User savedUser = userRepository.save(user);
        log.info("New customer registered: {}", savedUser.getEmail());

        final UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());

        // Include role in JWT claims
        final Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", savedUser.getRole().name());
        final String token = jwtService.generateToken(extraClaims, userDetails);

        return AuthResponse.builder()
                .token(token)
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(final LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        final User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        // Include role in JWT claims
        final Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        final String token = jwtService.generateToken(extraClaims, userDetails);

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
