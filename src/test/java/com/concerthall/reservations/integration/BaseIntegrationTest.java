package com.concerthall.reservations.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for integration tests that use the running Docker Compose services.
 *
 * Prerequisites:
 * - Run: docker compose up
 * - Services must be available at localhost:
 *   - PostgreSQL: localhost:5432
 *   - App: localhost:8080
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/reservations",
    "spring.datasource.username=reservations_user",
    "spring.datasource.password=reservations_pass"
})
public abstract class BaseIntegrationTest {
    // Uses running Docker Compose services
}
