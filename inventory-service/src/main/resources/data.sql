-- Smart Inventory System - Initial Database Seed Script (Executed via Spring SQL Init)

-- 1. Seed Roles
INSERT INTO roles (name, description) VALUES ('ROLE_ADMIN', 'Default ROLE_ADMIN');
INSERT INTO roles (name, description) VALUES ('ROLE_MANAGER', 'Default ROLE_MANAGER');
INSERT INTO roles (name, description) VALUES ('ROLE_ENTRY_OPERATOR', 'Default ROLE_ENTRY_OPERATOR');
INSERT INTO roles (name, description) VALUES ('ROLE_USER', 'Default ROLE_USER');

-- 2. Seed Default Users (Password: password encoded with BCrypt)
INSERT INTO users (username, password, enabled, created_at, updated_at) VALUES ('admin', '$2a$10$hlkL97m7kBQ0T5d79drhKe7KvsU8n6KbAHACMcnysxQhNEbWVhNXC', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO users (username, password, enabled, created_at, updated_at) VALUES ('manager', '$2a$10$hlkL97m7kBQ0T5d79drhKe7KvsU8n6KbAHACMcnysxQhNEbWVhNXC', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO users (username, password, enabled, created_at, updated_at) VALUES ('operator', '$2a$10$hlkL97m7kBQ0T5d79drhKe7KvsU8n6KbAHACMcnysxQhNEbWVhNXC', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO users (username, password, enabled, created_at, updated_at) VALUES ('viewer', '$2a$10$hlkL97m7kBQ0T5d79drhKe7KvsU8n6KbAHACMcnysxQhNEbWVhNXC', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign roles to users
-- (Assumes IDs 1-4 for roles and users)
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);
INSERT INTO user_roles (user_id, role_id) VALUES (2, 2);
INSERT INTO user_roles (user_id, role_id) VALUES (3, 3);
INSERT INTO user_roles (user_id, role_id) VALUES (4, 4);

-- 3. Seed Sample Inventory Items
INSERT INTO inventory_items (sku, quantity, reorder_threshold, warehouse_location, updated_at) VALUES ('LAPTOP-PRO-15', 50, 10, 'Warehouse-North', CURRENT_TIMESTAMP);
INSERT INTO inventory_items (sku, quantity, reorder_threshold, warehouse_location, updated_at) VALUES ('SMARTPHONE-X', 37, 10, 'Warehouse-South', CURRENT_TIMESTAMP);
INSERT INTO inventory_items (sku, quantity, reorder_threshold, warehouse_location, updated_at) VALUES ('WIRELESS-BUDS', 24, 10, 'Warehouse-East', CURRENT_TIMESTAMP);
INSERT INTO inventory_items (sku, quantity, reorder_threshold, warehouse_location, updated_at) VALUES ('MECHANICAL-KEYBOARD', 11, 10, 'Warehouse-West', CURRENT_TIMESTAMP);
INSERT INTO inventory_items (sku, quantity, reorder_threshold, warehouse_location, updated_at) VALUES ('4K-MONITOR', 63, 10, 'Central-Hub', CURRENT_TIMESTAMP);
INSERT INTO inventory_items (sku, quantity, reorder_threshold, warehouse_location, updated_at) VALUES ('DENIM-JACKET', 50, 10, 'Warehouse-North', CURRENT_TIMESTAMP);
INSERT INTO inventory_items (sku, quantity, reorder_threshold, warehouse_location, updated_at) VALUES ('RUNNING-SHOES', 37, 10, 'Warehouse-South', CURRENT_TIMESTAMP);
INSERT INTO inventory_items (sku, quantity, reorder_threshold, warehouse_location, updated_at) VALUES ('MERINO-SWEATER', 24, 10, 'Warehouse-East', CURRENT_TIMESTAMP);
INSERT INTO inventory_items (sku, quantity, reorder_threshold, warehouse_location, updated_at) VALUES ('COTTON-HOODIE', 11, 10, 'Warehouse-West', CURRENT_TIMESTAMP);
INSERT INTO inventory_items (sku, quantity, reorder_threshold, warehouse_location, updated_at) VALUES ('LEATHER-BELT', 63, 10, 'Central-Hub', CURRENT_TIMESTAMP);
