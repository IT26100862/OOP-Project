-- EstateX Real Estate Management System Database Schema
-- MySQL 8.0 Compatible Script

CREATE DATABASE IF NOT EXISTS estatex_db;

USE estatex_db;

-- 1. Table: users
-- Stores user account credentials, profile data, and system roles.
-- Roles: CUSTOMER, AGENT, ADMIN
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- Stored as secure hash
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    profile_image LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_role (role)
);

-- 2. Table: properties
-- Stores all real estate listings managed by the Agents.
CREATE TABLE IF NOT EXISTS properties (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    property_type VARCHAR(50) NOT NULL, -- e.g., 'Villa', 'Apartment', 'Penthouse', 'House', 'Commercial'
    listing_type VARCHAR(50) NOT NULL, -- e.g., 'Sale', 'Rent'
    price DECIMAL(15, 2) NOT NULL,
    location VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    bedrooms INT,
    bathrooms INT,
    floor_area_sqft INT,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'Under Review', -- e.g., 'Active', 'Under Review', 'Draft'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_prop_agent (agent_id),
    INDEX idx_prop_status (status)
);

-- 3. Table: property_images
-- Stores multiple photos for a single property.
CREATE TABLE IF NOT EXISTS property_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    INDEX idx_propimg_property (property_id)
);

-- 4. Table: bookings
-- Manages viewing appointments between Customers and Agents (via Property).
CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    viewing_date DATE NOT NULL,
    time_slot VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'Pending', -- e.g., 'Pending', 'Approved', 'Rejected', 'Completed', 'Cancelled'
    notes TEXT,
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    INDEX idx_book_customer (customer_id),
    INDEX idx_book_property (property_id)
);

-- 5. Table: inquiries
-- Manages questions/messages sent from Customers to Agents regarding properties.
CREATE TABLE IF NOT EXISTS inquiries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    agent_reply TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'Pending', -- e.g., 'Pending', 'Replied'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    replied_at TIMESTAMP NULL,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    INDEX idx_inq_customer (customer_id),
    INDEX idx_inq_property (property_id)
);

-- 6. Table: reviews
-- Allows Customers to leave feedback on Properties.
CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'Pending', -- e.g., 'Pending', 'Approved', 'Rejected'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    INDEX idx_rev_customer (customer_id),
    INDEX idx_rev_property (property_id)
);
