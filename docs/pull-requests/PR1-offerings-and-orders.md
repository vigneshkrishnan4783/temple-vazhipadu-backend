# PR #1: Offerings and Orders API

- **Branch**: `feature/p1-offerings` -> `main`
- **Status**: Merged

---

## Problem
Provides core catalog management for temple vazhipadu offerings and enables devotees to book offerings for chosen dates with server-side total price computation and unified error responses.

## Assumptions
- Offerings belong to a specific temple (`templeId`), allowing multi-temple support across the temple board.
- Each line item captures unit price snapshot at time of booking to protect against subsequent offering price changes.
- Phone numbers follow standard E.164 / local format (`^[0-9+ -]{7,15}$`).
- Duplicate line items for the same offering ID in a single order request are disallowed to ensure clarity and avoid concurrency discrepancies.

## Decisions & trade-offs
- **Monetary calculations**: Used `java.math.BigDecimal` with `RoundingMode.HALF_UP` and scale of 2 instead of double/float to prevent floating-point rounding errors on currency calculations.
- **Unified Error Representation**: Created `ApiError` record matching standard REST guidelines with timestamp, status, error name, human-readable message, and detailed field validations.
- **Entity Cascading**: Configured `CascadeType.ALL` and `orphanRemoval = true` on `Order.items` for atomic order-item lifecycle management.

## Code Review & Resolution
- **Reviewer Comment** (`OrderService.java:42`):
  > *"When a devotee sends multiple line items referencing the same offering ID in one order request, how do we handle it?"*
- **Resolution**:
  > Added duplicate offering validation in `OrderService` rejecting requests with multiple identical offering IDs with HTTP 400 (`"Duplicate offering id X in order. Please consolidate quantities."`). Accompanied by automated test `testOrderWithDuplicateOfferingsIsRejected`.

## How to test
```bash
# Run unit & integration tests
./mvnw test -Dtest=OrderApiIntegrationTest
```
### Sample Request (Create Order)
```bash
POST /api/v1/orders
Content-Type: application/json

{
  "templeId": 101,
  "devoteeName": "Anand Kumar",
  "phoneNumber": "9876543210",
  "nakshatra": "Rohini",
  "bookingDate": "2026-11-05",
  "items": [
    { "offeringId": 1, "quantity": 2 },
    { "offeringId": 2, "quantity": 1 }
  ]
}
```
Response: HTTP 201 Created with total amount calculated on server (e.g. ₹110.00).
