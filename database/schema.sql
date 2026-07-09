# Medical Billing System - Database Schema

CREATE DATABASE IF NOT EXISTS medical_billing_db;
USE medical_billing_db;

-- Roles
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

-- Users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(15),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Categories
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);

-- Suppliers
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_name VARCHAR(150) NOT NULL,
    gst_number VARCHAR(20),
    contact_person VARCHAR(100),
    phone VARCHAR(15),
    email VARCHAR(100),
    address VARCHAR(500),
    state VARCHAR(50),
    pin_code VARCHAR(10),
    outstanding_balance DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);

-- Customers
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(150) NOT NULL,
    phone VARCHAR(15),
    email VARCHAR(100),
    address VARCHAR(500),
    age INT,
    gender VARCHAR(10),
    doctor_name VARCHAR(100),
    gst_number VARCHAR(20),
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);

-- Medicines
CREATE TABLE IF NOT EXISTS medicines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medicine_code VARCHAR(50) NOT NULL UNIQUE,
    medicine_name VARCHAR(200) NOT NULL,
    category_id BIGINT,
    brand VARCHAR(100),
    batch_number VARCHAR(50),
    expiry_date DATE,
    manufacturing_date DATE,
    hsn_code VARCHAR(20),
    gst_percent DECIMAL(5,2) DEFAULT 0,
    purchase_price DECIMAL(10,2) NOT NULL,
    selling_price DECIMAL(10,2) NOT NULL,
    mrp DECIMAL(10,2),
    discount_percent DECIMAL(5,2) DEFAULT 0,
    rack_number VARCHAR(20),
    minimum_stock INT NOT NULL DEFAULT 10,
    current_stock INT NOT NULL DEFAULT 0,
    barcode VARCHAR(100),
    supplier_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

-- Purchases
CREATE TABLE IF NOT EXISTS purchases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    supplier_id BIGINT NOT NULL,
    purchase_date DATE NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    gst_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    grand_total DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE TABLE IF NOT EXISTS purchase_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    purchase_price DECIMAL(10,2) NOT NULL,
    gst_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    subtotal DECIMAL(12,2) NOT NULL,
    expiry_date DATE,
    batch_number VARCHAR(50),
    FOREIGN KEY (purchase_id) REFERENCES purchases(id),
    FOREIGN KEY (medicine_id) REFERENCES medicines(id)
);

-- Sales
CREATE TABLE IF NOT EXISTS sales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT,
    user_id BIGINT NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    gst_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    grand_total DECIMAL(12,2) NOT NULL DEFAULT 0,
    amount_paid DECIMAL(12,2) NOT NULL DEFAULT 0,
    return_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    payment_mode VARCHAR(10) NOT NULL,
    sale_date DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS sales_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    batch_number VARCHAR(50),
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    discount_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    gst_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    subtotal DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (sale_id) REFERENCES sales(id),
    FOREIGN KEY (medicine_id) REFERENCES medicines(id)
);

-- Payments
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id BIGINT,
    supplier_id BIGINT,
    amount DECIMAL(12,2) NOT NULL,
    payment_mode VARCHAR(10) NOT NULL,
    pending BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(500),
    payment_date DATETIME NOT NULL,
    FOREIGN KEY (sale_id) REFERENCES sales(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

-- Returns
CREATE TABLE IF NOT EXISTS returns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    return_number VARCHAR(50) NOT NULL UNIQUE,
    return_type VARCHAR(20) NOT NULL,
    medicine_id BIGINT NOT NULL,
    sale_id BIGINT,
    purchase_id BIGINT,
    quantity INT NOT NULL,
    refund_amount DECIMAL(12,2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    user_id BIGINT NOT NULL,
    return_date DATETIME NOT NULL,
    FOREIGN KEY (medicine_id) REFERENCES medicines(id),
    FOREIGN KEY (sale_id) REFERENCES sales(id),
    FOREIGN KEY (purchase_id) REFERENCES purchases(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Prescriptions
CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    notes VARCHAR(500),
    uploaded_by BIGINT,
    uploaded_at DATETIME NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

-- Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    username VARCHAR(100),
    details VARCHAR(1000),
    ip_address VARCHAR(50),
    timestamp DATETIME NOT NULL
);

-- Shop Settings
CREATE TABLE IF NOT EXISTS shop_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_name VARCHAR(200) NOT NULL DEFAULT 'Medical Shop',
    logo_path VARCHAR(500),
    gst_number VARCHAR(20),
    address VARCHAR(500),
    phone VARCHAR(15),
    email VARCHAR(100),
    invoice_footer VARCHAR(1000),
    default_gst_percent DECIMAL(5,2) DEFAULT 12.00,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);
