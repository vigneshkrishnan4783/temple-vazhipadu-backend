# Temple Vazhipadu Booking Service (`temple-vazhipadu-backend`)

A production-ready vazhipadu (temple offering) booking backend service built for temple boards using **Java 17** and **Spring Boot 3.x**.

---

## 🏛️ Domain Overview
A *vazhipadu* is a sacred ritual offering performed at a Hindu temple on behalf of a devotee. The devotee pays a fixed fee, and the temple priest performs the ritual on a chosen date in the name of a person and their birth star (*nakshatra*).

This backend service handles offerings catalogue management, order placement with atomic price calculations, high-performance counter order searches, thread-safe daily offering limits under high concurrency, order cancellations, and Indian Standard Time (IST) financial audit collection reports.

---

## 🛠️ Tech Stack & Architecture
- **Language**: Java 17 (Azul Zulu OpenJDK 17 LTS)
- **Framework**: Spring Boot 3.2.5
- **Database**: H2 in PostgreSQL compatibility mode (`jdbc:h2:mem:templedb;MODE=PostgreSQL`)
- **Persistence**: Spring Data JPA / Hibernate 6
- **Database Migrations**: Flyway (Flyway Core 9.x)
- **Concurrency Control**: Pessimistic Write Locking (`PESSIMISTIC_WRITE` / `SELECT ... FOR UPDATE`)
- **Monetary Arithmetic**: `java.math.BigDecimal` (scale 2, `RoundingMode.HALF_UP`)
- **Testing**: JUnit 5, MockMvc, Concurrent `ExecutorService` (20-thread simultaneous booking test)

---

## 🌿 Pull Requests & Git History
Built strictly according to team Git workflow with independent feature branches, descriptive step commits, peer review line comments, and merged PRs:

| PR # | Branch | Summary | Review Comment & Fix | Status |
|:---:|:---|:---|:---|:---:|
| **[#1](https://github.com/vigneshkrishnan4783/temple-vazhipadu-backend/pull/1)** ([Doc](docs/pull-requests/PR1-offerings-and-orders.md)) | `feature/p1-offerings` | Offerings catalog CRUD, server-side order total calculation, and standardized error model | Validated & rejected duplicate offering IDs in single order requests | **Merged** |
| **[#2](https://github.com/vigneshkrishnan4783/temple-vazhipadu-backend/pull/2)** ([Doc](docs/pull-requests/PR2-order-search.md)) | `feature/p2-order-search` | High-volume order search by temple, date, status, phone number with composite indexes and pagination | Capped page size at max 100 to prevent OOM/GC pressure | **Merged** |
| **[#3](https://github.com/vigneshkrishnan4783/temple-vazhipadu-backend/pull/3)** ([Doc](docs/pull-requests/PR3-daily-limits.md)) | `feature/p3-daily-limits` | Thread-safe daily quota enforcement with pessimistic row locking and 20-thread stress tests | Clarified error message when quota is already saturated vs newly exceeded | **Merged** |
| **[#4](https://github.com/vigneshkrishnan4783/temple-vazhipadu-backend/pull/4)** ([Doc](docs/pull-requests/PR4-cancellation-and-reports.md)) | `feature/p4-cancellation-and-reports` | Order cancellation with automatic quota freeing and paisa-accurate IST daily collection reports | Enforced strict scale 2 with `RoundingMode.HALF_UP` on financial grand totals | **Merged** |

*Detailed descriptions and line comment reviews for all PRs are documented in `docs/pull-requests/`.*

---

## 🚀 How to Run the Application

### Prerequisites
- Java 17+ installed
- Maven 3.8+ (or use the pre-configured wrapper `./mvnw`)

### Running the Application
```bash
./mvnw spring-boot:run
```
The server will start on port `8080`.
The embedded H2 Web Console is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:templedb`, Username: `sa`, Password: *blank*).

---

## 🧪 Running Automated Tests
Run the entire suite of 14 unit, integration, and high-concurrency stress tests:
```bash
./mvnw clean test
```

### Key Test Coverage:
1. `OrderApiIntegrationTest`:
   - Acceptance: 2 Pushpanjali @ ₹30 + 1 Archana @ ₹50 = ₹110.00.
   - Acceptance: Inactive offerings are rejected with 400 Bad Request.
   - Acceptance: Unified JSON error responses.
2. `OrderSearchIntegrationTest`:
   - Acceptance: Searching by phone number returns only that devotee's orders.
   - Acceptance: Stable pagination between page 0 and page 1 with zero gaps and zero duplicate records.
3. `DailyLimitIntegrationTest`:
   - Acceptance: Daily limit 5 with 4 booked: order for 2 rejected, order for 1 succeeds.
   - Acceptance: **20 simultaneous orders** for 1 each against a daily limit of 5 -> **Exactly 5 succeed, 15 rejected** under concurrent `ExecutorService` load.
4. `OrderCancellationAndReportIntegrationTest`:
   - Acceptance: Cancelled orders free daily limits immediately.
   - Acceptance: Cancelled orders are excluded from financial reports.
   - Acceptance: Order placed at 00:15 IST is counted on that Indian calendar date.
   - Acceptance: Report grand total matches sum of individual offerings to the exact paisa.

---

## 📡 API Endpoints & Sample Requests

### 1. Create an Offering (Catalog)
```bash
curl -X POST http://localhost:8080/api/v1/offerings \
  -H "Content-Type: application/json" \
  -d '{
    "templeId": 101,
    "name": "Pushpanjali",
    "price": 30.00,
    "active": true,
    "dailyLimit": 50
  }'
```

### 2. Place an Order
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "templeId": 101,
    "devoteeName": "Anand Kumar",
    "phoneNumber": "9876543210",
    "nakshatra": "Rohini",
    "bookingDate": "2026-11-15",
    "items": [
      { "offeringId": 1, "quantity": 2 }
    ]
  }'
```

### 3. Search Orders (Counter Staff)
```bash
# Filter by devotee phone number
curl "http://localhost:8080/api/v1/orders?templeId=101&phoneNumber=9876543210&page=0&size=20"

# Filter by booking date and confirmed status
curl "http://localhost:8080/api/v1/orders?templeId=101&bookingDate=2026-11-15&status=CONFIRMED&page=0&size=20"
```

### 4. Cancel an Order
```bash
curl -X POST http://localhost:8080/api/v1/orders/1/cancel
```

### 5. Daily Collection Report (IST)
```bash
curl "http://localhost:8080/api/v1/reports/daily-collection?templeId=101&date=2026-11-05"
```
**Sample Response:**
```json
{
  "templeId": 101,
  "reportDate": "2026-11-05",
  "totalOrders": 1,
  "totalAmount": 60.00,
  "offerings": [
    {
      "offeringId": 1,
      "offeringName": "Pushpanjali",
      "quantity": 2,
      "totalAmount": 60.00
    }
  ]
}
```

---

## 🔒 Concurrency & Financial Precision Design

### Why Pessimistic Locking on Daily Limits?
When multiple counter operators and devotees book simultaneously, optimistic locking (`@Version`) would cause high transaction rollback rates and poor user experience. By taking a pessimistic write lock (`SELECT ... FOR UPDATE`) on the `Offering` row ordered by `offeringId ASC`, requests serialize cleanly with zero deadlock and zero overselling.

### Exact-to-the-Paisa Financial Calculations
In accordance with banking and finance auditing standards, all currency fields use `java.math.BigDecimal` with scale 2 and `RoundingMode.HALF_UP`. Floating-point types (`float`, `double`) are strictly prohibited to avoid binary rounding imprecision.
