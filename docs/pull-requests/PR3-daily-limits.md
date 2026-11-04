# PR #3: Thread-Safe Daily Offering Limits

- **Branch**: `feature/p3-daily-limits` -> `main`
- **Status**: Merged

---

## Problem
Guarantees offering daily booking constraints under heavy simultaneous booking volume (e.g. online devotees and counter staff competing for 50 Udayasthamana Poojas) without overselling.

## Assumptions
- `dailyLimit` is an optional attribute on an offering (`null` implies unlimited availability).
- Daily quotas are evaluated per offering per calendar date (`bookingDate`).
- Quotas are reserved at order creation time within the database transaction.

## Decisions & trade-offs
- **Pessimistic Locking vs Optimistic Locking**:
  - *Chosen*: Database pessimistic write locking (`SELECT ... FOR UPDATE`) on the target `Offering` row combined with deterministic lock acquisition ordering (`offeringId ASC`).
  - *Why*: Under high contention (e.g., 20 simultaneous requests competing for the last remaining slots), optimistic locking (`@Version`) causes frequent rollbacks and high retry overhead. Pessimistic locking guarantees strict serializability and zero overselling with immediate fast failure for excess requests.
- **Dedicated Daily Quota Table**:
  - Introduced `daily_offering_quotas` with composite unique constraint `(offering_id, booking_date)`. This keeps offering metadata clean and enables atomic quota adjustments.

## Code Review & Resolution
- **Reviewer Comment** (`OrderService.java:82`):
  > *"If an offering's daily limit was lowered after existing orders were booked, or if the quota is already saturated, the error message should clearly distinguish between 'already reached' and 'would exceed with requested quantity'."*
- **Resolution**:
  > Updated `OrderService` to differentiate when `bookedCount >= dailyLimit` vs `bookedCount + requestedQty > dailyLimit`, providing clear diagnostic messages for counter staff and devotees.

## How to test
```bash
# Run daily limit and 20-thread concurrency tests
./mvnw test -Dtest=DailyLimitIntegrationTest
```
- Includes `testConcurrentOrdersRespectDailyLimit` which spawns 20 parallel threads against a limit of 5 using `CountDownLatch`, asserting that exactly 5 succeed (201 Created) and 15 are rejected (409 Conflict).
