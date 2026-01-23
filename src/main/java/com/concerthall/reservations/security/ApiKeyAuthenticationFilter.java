package com.concerthall.reservations.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Value("${service.api.key}")
    private String serviceApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Only apply to internal API endpoints
        if (requestPath.startsWith("/api/internal/")) {
            String apiKey = request.getHeader("X-API-Key");

            if (apiKey != null && apiKey.equals(serviceApiKey)) {
                log.debug("Valid API key received for internal endpoint: {}", requestPath);

                // Create authentication with SERVICE role
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                "service",
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVICE"))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("Invalid or missing API key for internal endpoint: {}", requestPath);
            }
        }

        filterChain.doFilter(request, response);
    }
}
