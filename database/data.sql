-- Seed data (optional - application DataInitializer handles this automatically)
USE medical_billing_db;

INSERT IGNORE INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_PHARMACIST'), ('ROLE_CASHIER');
