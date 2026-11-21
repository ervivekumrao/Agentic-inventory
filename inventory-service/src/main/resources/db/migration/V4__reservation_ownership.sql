-- V4 Migration: Reservation Ownership & Security Authorization
ALTER TABLE inventory_reservations ADD COLUMN IF NOT EXISTS reserved_by_user VARCHAR(100);
ALTER TABLE inventory_reservations ADD COLUMN IF NOT EXISTS owner_service_id VARCHAR(100);
ALTER TABLE inventory_reservation_units ADD COLUMN IF NOT EXISTS reserved_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_reservation_user ON inventory_reservations(reserved_by_user);
