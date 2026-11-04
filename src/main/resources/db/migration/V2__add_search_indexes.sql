-- V2: Composite and targeted indexes for high-volume order lookup and pagination

-- Counter lookup index: filtering by temple, booking date and status
CREATE INDEX idx_orders_temple_date_status ON orders (temple_id, booking_date, status);

-- Phone number search index: fast devotee lookup by phone number
CREATE INDEX idx_orders_temple_phone ON orders (temple_id, phone_number);

-- Stable pagination index: index-backed ordering by creation time and tie-breaker id
CREATE INDEX idx_orders_stable_sort ON orders (temple_id, created_at DESC, id DESC);
