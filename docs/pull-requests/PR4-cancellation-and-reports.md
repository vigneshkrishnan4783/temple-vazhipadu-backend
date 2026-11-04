# PR #4: Order Cancellation & IST Daily Collection Reports

- **Branch**: `feature/p4-cancellation-and-reports` -> `main`
- **Status**: Merged

---

## Problem
Enables temple staff to cancel orders (releasing reserved daily offering limits) and generates an exact, paisa-accurate daily collection report counted strictly according to Indian Standard Time (IST / Asia/Kolkata).

## Assumptions
- An order can only be cancelled if it is currently in `CONFIRMED` status. Cancelling an already cancelled order returns HTTP 400 Bad Request.
- Daily collection reports reflect revenue collected on that date (evaluated by order creation timestamp `createdAt` converted to Indian Standard Time).
- Only non-cancelled (`CONFIRMED`) orders contribute to revenue and offering quantities in the report.

## Decisions & trade-offs
- **Indian Standard Time (IST) Date Boundary Evaluation**:
  - Implemented `TimeUtils` converting requested date to `[atStartOfDay(Asia/Kolkata), plusDays(1).atStartOfDay(Asia/Kolkata))` in UTC `Instant`.
  - Orders placed at `00:15 IST` (which corresponds to `18:45 UTC` of the previous calendar day) are correctly attributed to the Indian calendar date.
- **Paisa-Exact Monetary Precision**:
  - Computed using `BigDecimal` with scale 2 and `RoundingMode.HALF_UP`.
  - Verified by tests that `report.totalAmount == sum(offering.totalAmount)`.
- **Atomic Daily Quota Rollback**:
  - When an order is cancelled, pessimistic locking (`findByIdForUpdate`) is applied on offering rows, decrementing `DailyOfferingQuota.bookedCount` and immediately restoring slots for devotees.

## Code Review & Resolution
- **Reviewer Comment** (`ReportService.java:55`):
  > *"When computing `calculatedGrandTotal` for the finance collection report, we must ensure the scale is strictly set to 2 decimal places with `RoundingMode.HALF_UP` on the final sum to guarantee exact paisa alignment even if offerings or discounts ever introduce fractions."*
- **Resolution**:
  > Enforced `calculatedGrandTotal.setScale(2, RoundingMode.HALF_UP)` on report aggregation. Tested with odd paise values (e.g., ₹33.33 + ₹66.67 = ₹166.66).

## How to test
```bash
# Run cancellation and report tests
./mvnw test -Dtest=OrderCancellationAndReportIntegrationTest
```
### Sample Request (Cancel Order)
```bash
POST /api/v1/orders/1/cancel
```
### Sample Request (Daily Collection Report)
```bash
GET /api/v1/reports/daily-collection?templeId=101&date=2026-11-05
```
Response contains total orders, total amount (paisa exact), and breakdown per offering.
