# Temple Vazhipadu Booking Service (`temple-vazhipadu-backend`)

A production-ready vazhipadu (temple offering) booking backend service built for temple boards using **Java 17** and **Spring Boot 3.x**.

---

## Tech Stack
- **Language**: Java 17 (Azul Zulu OpenJDK 17)
- **Framework**: Spring Boot 3.2.5
- **Database**: H2 in PostgreSQL compatibility mode (embedded relational database)
- **Persistence**: Spring Data JPA / Hibernate 6
- **Database Migrations**: Flyway
- **Testing**: JUnit 5, MockMvc, Concurrent ExecutorService Tests

---

## Problem Breakdown & PR Structure
This project is built across four structured pull requests adhering to professional Git discipline:

1. **[PR #1: Offerings Catalogue & Order Placement](https://github.com/vignesh-k/temple-vazhipadu-backend/pull/1)** (`feature/p1-offerings`)
   - Offerings catalog CRUD (name, price, active status)
   - Order placement with server-side total computation
   - Validation & standardized RFC 7807-compliant JSON error responses
2. **[PR #2: High-Performance Order Search & Pagination](https://github.com/vignesh-k/temple-vazhipadu-backend/pull/2)** (`feature/p2-order-search`)
   - Composite indexed search by `templeId`, `bookingDate`, `status`, and `phoneNumber`
   - Stable cursor/keyset & page-based pagination to support hundreds of thousands of records
   - Flyway database schema migrations
3. **[PR #3: Thread-Safe Daily Offering Limits](https://github.com/vignesh-k/temple-vazhipadu-backend/pull/3)** (`feature/p3-daily-limits`)
   - Configurable optional daily limit per offering
   - Pessimistic write locking on daily offering quotas to prevent overselling under high concurrency
   - Automated stress test verifying concurrent order placement
4. **[PR #4: Order Cancellation & IST Daily Collection Reports](https://github.com/vignesh-k/temple-vazhipadu-backend/pull/4)** (`feature/p4-cancellation-and-reports`)
   - Order cancellation with automatic quota rollback
   - Daily collection report for finance audit (paisa-exact `BigDecimal` totals, breakdown per offering)
   - Indian Standard Time (IST / Asia/Kolkata) date boundary evaluation

---

## How to Build and Run

### Prerequisites
- Java 17+
- Maven 3.8+ (or use the included `./mvnw`)

### Build & Run Tests
```bash
./mvnw clean test
```

### Run Application
```bash
./mvnw spring-boot:run
```
The application will start on `http://localhost:8080`.
The H2 database console is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:templedb`).
