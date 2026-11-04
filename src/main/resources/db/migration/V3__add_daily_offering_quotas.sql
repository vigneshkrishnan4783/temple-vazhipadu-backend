-- V3: Schema for tracking daily quotas and limits per offering per date

CREATE TABLE daily_offering_quotas (
    id BIGSERIAL PRIMARY KEY,
    offering_id BIGINT NOT NULL,
    booking_date DATE NOT NULL,
    booked_count INT NOT NULL DEFAULT 0,
    CONSTRAINT uk_quota_offering_date UNIQUE (offering_id, booking_date),
    CONSTRAINT fk_quota_offering FOREIGN KEY (offering_id) REFERENCES offerings(id) ON DELETE CASCADE
);

CREATE INDEX idx_quota_offering_date ON daily_offering_quotas (offering_id, booking_date);
