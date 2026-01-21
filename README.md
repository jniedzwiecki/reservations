# Concert Hall Ticket Reservation System

A comprehensive Spring Boot application for managing concert hall ticket sales with role-based access control, JWT authentication, and real-time capacity management.

## Features

- **Role-based Access Control**: Admin, Power Users, and Customers
- **JWT Authentication**: Secure token-based authentication
- **Event Management**: Create, update, and manage concert events
- **Ticket Reservations**: One ticket per customer per event with race condition protection
- **Real-time Capacity Tracking**: Pessimistic locking prevents overbooking
- **Swagger UI**: Interactive API documentation
- **Comprehensive Testing**: Unit and integration tests with Testcontainers
- **Elasticsearch Logging**: Structured logging for analytics
- **Docker Deployment**: Complete docker-compose setup

## Technology Stack

- **Java 21**
- **Spring Boot 3.2.1**
- **PostgreSQL 16** (business data)
- **Elasticsearch 8.11.0** (logging)
- **Flyway** (database migrations)
- **JWT** (authentication)
- **JUnit 5 + Testcontainers** (testing)
- **Docker & Docker Compose** (deployment)

---

# Initial Requirements:
- Create an application to sell and validate tickets to a concert hall. There is currently one concert hall. So let's stick to creating 
ticketing to only that one.

## users:
- There is admin user, power users and regular users (customers)
  - Admin user is created by a script. His role is only needed to create power users (venue administrators). Further he has identical privileges as power users,
just his account cannot be removed
  - All power users are allowed to edit repertoire and manage tickets
  - Regular users can only buy tickets choosing the event. They register into application with an email address and a password.
For now no confirmation e-mail is being sent.

## events:
- An event has a date, time and capacity. Only one type of tickets are sold.
- Power users can see the state of tickets sold for an event
- Customers can see the repertoire with available tickets

## tickets
- Currently tickets are just reservations, since there is no technical way yet to process with payments 
- Customers can check their tickets (tickets they have bought with state future/past) with details visible

## Testing
- I want for testing purposes to be able to use scripts to create initial data - same as I would do it by calling REST

## technical requirement:
- java with spring boot project, maven
- create junit5 tests for all functionality
- logs stored in Elastic
- business data stored in PostgreSql
- migrations done by flyway 
- communication with frontend with REST
- JWT used as a way to authenticate
- JWT secret stored for now in config file
- whole solution deployed as docker-compose.

# Perspective:
- Tickets are sold at the set price but the price can be different starting at the specific dates (to be set up by power users)

# Technical perspective:
- move tickets to Cassandra?

---

## Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 16 (optional for local development)

### Running with Docker Compose (Recommended)

1. **Clone the repository**
```bash
git clone <repository-url>
cd reservations
```

2. **Build and start all services**
```bash
docker-compose up --build
```

This will start:
- **Application**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **Elasticsearch**: http://localhost:9200
- **Kibana**: http://localhost:5601
- **Swagger UI**: http://localhost:8080/swagger-ui.html

3. **Stop services**
```bash
docker-compose down
```

### Running Locally for Development

1. **Start only infrastructure services**
```bash
docker-compose up postgres elasticsearch kibana
```

2. **Run the application**
```bash
./mvnw spring-boot:run
```

3. **Access the application**
- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

## Default Admin Credentials

- **Email**: admin@concerthall.com
- **Password**: admin123

## API Usage Examples

### 1. Register as a Customer

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "password123"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "customer@example.com",
  "role": "CUSTOMER"
}
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@concerthall.com",
    "password": "admin123"
  }'
```

### 3. Create an Event (Admin/Power User)

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Rock Concert Night",
    "description": "An amazing rock concert",
    "eventDateTime": "2026-03-15T20:00:00",
    "capacity": 100,
    "price": 50.00,
    "status": "PUBLISHED"
  }'
```

### 4. List Published Events (All Users)

```bash
curl -X GET http://localhost:8080/api/events \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 5. Reserve a Ticket (Customer)

```bash
curl -X POST http://localhost:8080/api/tickets/reserve \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": 1
  }'
```

### 6. View My Tickets (Customer)

```bash
curl -X GET http://localhost:8080/api/tickets/my-tickets \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 7. Get Event Sales Statistics (Admin/Power User)

```bash
curl -X GET http://localhost:8080/api/events/1/sales \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Response:
```json
{
  "eventId": 1,
  "eventName": "Rock Concert Night",
  "capacity": 100,
  "ticketsSold": 45,
  "availableTickets": 55,
  "revenue": 2250.00,
  "occupancyRate": 45.0
}
```

## Testing

### Run All Tests

```bash
./mvnw test
```

### Run Integration Tests Only

```bash
./mvnw test -Dtest="*IntegrationTest"
```

### Run Specific Test

```bash
./mvnw test -Dtest=TicketReservationIntegrationTest
```

### Key Test Scenarios

1. **Concurrent Booking Test**: Simulates 10 concurrent users trying to book tickets for an event with capacity 5. Verifies that exactly 5 tickets are sold and race conditions are handled correctly.

2. **Duplicate Prevention Test**: Ensures customers cannot book multiple tickets for the same event.

3. **Capacity Check Test**: Validates that bookings fail when event is sold out.

4. **Authentication Tests**: Verifies registration, login, and JWT token generation.

## Database Migrations

Flyway migrations are located in `src/main/resources/db/migration/`:

- `V1__init_schema.sql`: Creates users, events, and tickets tables
- `V2__insert_admin_user.sql`: Creates default admin user

Migrations run automatically on application startup.

## Project Structure

```
src/
├── main/
│   ├── java/com/concerthall/reservations/
│   │   ├── config/              # Security, JWT, Swagger configuration
│   │   ├── controller/          # REST endpoints
│   │   ├── domain/              # JPA entities
│   │   │   └── enums/           # Enums (UserRole, EventStatus, TicketStatus)
│   │   ├── dto/                 # Request/Response objects
│   │   ├── exception/           # Custom exceptions and global handler
│   │   ├── repository/          # Spring Data JPA repositories
│   │   ├── security/            # JWT filter, UserDetailsService
│   │   └── service/             # Business logic
│   └── resources/
│       ├── db/migration/        # Flyway SQL scripts
│       ├── application.yml      # Application configuration
│       └── logback-spring.xml   # Elasticsearch logging config
└── test/
    ├── java/com/concerthall/reservations/
    │   ├── integration/         # Integration tests with Testcontainers
    │   └── service/             # Unit tests
    └── resources/
        └── application-test.yml # Test configuration
```

## Environment Variables

Configure these in production:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/reservations
SPRING_DATASOURCE_USERNAME=reservations_user
SPRING_DATASOURCE_PASSWORD=reservations_pass
SPRING_ELASTICSEARCH_URIS=http://elasticsearch:9200
JWT_SECRET=your-production-secret-key-at-least-256-bits
```

## Key Implementation Details

### Race Condition Prevention

The ticket reservation system uses pessimistic locking to prevent overbooking:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT e FROM Event e WHERE e.id = :id")
Optional<Event> findByIdWithPessimisticLock(@Param("id") Long id);
```

### One Ticket Per Event Constraint

Database constraint ensures customers can only book one ticket per event:

```sql
CONSTRAINT unique_user_event UNIQUE (user_id, event_id, status)
```

### JWT Configuration

Tokens expire after 24 hours (configurable via `jwt.expiration` property).

## Monitoring & Logging

### View Logs in Kibana

1. Open http://localhost:5601
2. Create index pattern: `logstash-*`
3. View logs in Discover section

### Structured Logging

All API requests, business events, and errors are logged in JSON format to Elasticsearch with context:
- User ID
- Request ID
- Event ID
- Ticket ID

## API Documentation

Interactive API documentation is available at:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs

## Security

- Passwords are hashed using BCrypt
- Minimum password length: 5 characters
- JWT tokens required for all endpoints except `/api/auth/**`
- Role-based access control using Spring Security
- Admin users cannot be deleted (`is_removable = false`)

## Future Enhancements

- Dynamic pricing schedules
- Email confirmation on registration
- Payment processing integration
- QR code generation for tickets
- Ticket transfers between users
- Multi-venue support
- Cassandra migration for high-volume ticket data

## License

[Add your license here]

## Contributing

[Add contribution guidelines here]
