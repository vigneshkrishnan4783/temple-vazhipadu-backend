# PR #2: High-Performance Order Search & Pagination

- **Branch**: `feature/p2-order-search` -> `main`
- **Status**: Merged

---

## Problem
Enables temple counter staff to rapidly filter and search orders by temple, date, status, and devotee phone number across hundreds of thousands of records using composite database indexes and deterministic pagination.

## Assumptions
- `templeId` is mandatory for all search operations to maintain strict tenant/temple data isolation.
- `phoneNumber` search performs exact matching against indexed phone numbers for fast, index-backed lookup at counter operations.
- Orders must be displayed in reverse chronological order with deterministic tie-breaking (`createdAt DESC, id DESC`).

## Decisions & trade-offs
- **Composite Index Design**: Added Flyway migration `V2__add_search_indexes.sql`:
  - `idx_orders_temple_date_status` (`temple_id, booking_date, status`): optimized for the counter's daily booking lookup.
  - `idx_orders_temple_phone` (`temple_id, phone_number`): targeted index for phone number searches.
  - `idx_orders_stable_sort` (`temple_id, created_at DESC, id DESC`): avoids in-memory sort operations on deep pagination pages.
- **Dynamic Specification**: Used Spring Data `JpaSpecificationExecutor` with `CriteriaBuilder` to assemble SQL queries dynamically without raw SQL or string concatenation.
- **Keyset vs Offset Pagination**: Implemented offset-based pagination backed by composite indexes for compatibility with counter UI page controls, paired with deterministic tie-breaker `id DESC` to ensure zero gaps or duplicates.

## Code Review & Resolution
- **Reviewer Comment** (`OrderService.java:125`):
  > *"If a client requests a massive page size like `size=50000`, the server could experience severe GC pressure or out-of-memory errors. We should enforce an upper limit."*
- **Resolution**:
  > Capped maximum page size to 100 in `OrderService.searchOrders`. Any request with `size > 100` is safely bounded to 100.

## How to test
```bash
# Run search and pagination tests
./mvnw test -Dtest=OrderSearchIntegrationTest
```
### Sample Request (Search by Phone)
```bash
GET /api/v1/orders?templeId=101&phoneNumber=9876543210&page=0&size=20
```
### Sample Request (Filtered by Date and Status)
```bash
GET /api/v1/orders?templeId=101&bookingDate=2026-11-05&status=CONFIRMED&page=0&size=20
```
