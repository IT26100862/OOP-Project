CREATE DATABASE IF NOT EXISTS estatex_db;
USE estatex_db;

-- 1. Table: users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
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
CREATE TABLE IF NOT EXISTS properties (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    property_type VARCHAR(50) NOT NULL,
    listing_type VARCHAR(50) NOT NULL,
    price DECIMAL(15, 2) NOT NULL,
    location VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    bedrooms INT,
    bathrooms INT,
    floor_area_sqft INT,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_prop_agent (agent_id),
    INDEX idx_prop_status (status)
);

-- 3. Table: property_images
CREATE TABLE IF NOT EXISTS property_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    image_url LONGTEXT NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    INDEX idx_propimg_property (property_id)
);

-- 4. Table: bookings
CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    viewing_date DATE NOT NULL,
    time_slot VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'Pending',
    notes TEXT,
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    INDEX idx_book_customer (customer_id),
    INDEX idx_book_property (property_id)
);

-- 5. Table: inquiries
CREATE TABLE IF NOT EXISTS inquiries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    agent_reply TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'Pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    replied_at TIMESTAMP NULL,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    INDEX idx_inq_customer (customer_id),
    INDEX idx_inq_property (property_id)
);

-- 6. Table: reviews
CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'Approved',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    INDEX idx_rev_customer (customer_id),
    INDEX idx_rev_property (property_id)
);

-- 7. Table: financial_breakdowns
CREATE TABLE IF NOT EXISTS financial_breakdowns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    property_price DECIMAL(15,2) NOT NULL,
    advance_payment DECIMAL(15,2) NOT NULL,
    legal_fees DECIMAL(15,2) DEFAULT 0.00,
    stamp_duty DECIMAL(15,2) DEFAULT 0.00,
    other_fees DECIMAL(15,2) DEFAULT 0.00,
    total_initial_payment DECIMAL(15,2) NOT NULL,
    remaining_balance DECIMAL(15,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    INDEX idx_fb_customer (customer_id),
    INDEX idx_fb_property (property_id)
);

-- Seed Initial Users
INSERT INTO users (username, password, email, full_name, phone, role)
SELECT 'dilshan@user', 'userPass_dilshan123', 'dilshan.perera@gmail.com', 'Dilshan Perera', '+94 77 123 4567', 'CUSTOMER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'dilshan@user');

INSERT INTO users (username, password, email, full_name, phone, role)
SELECT 'chethana@user', 'chethana123', 'chethanapersonal2005@gmail.com', 'Chethana Gurusinghe', '+94 77 987 6543', 'CUSTOMER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'chethana@user');

INSERT INTO users (username, password, email, full_name, phone, role)
SELECT 'chaminda@agent', 'agentPass_chaminda123', 'chaminda.agent@estatex.lk', 'Chaminda Wijesekara', '+94 71 888 9900', 'AGENT'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'chaminda@agent');

INSERT INTO users (username, password, email, full_name, phone, role)
SELECT 'admin@admin', 'adminPass_master123', 'admin@estatex.lk', 'System Administrator', '+94 11 234 5678', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin@admin');

-- Seed Sample Properties
INSERT INTO properties (id, agent_id, title, property_type, listing_type, price, location, address, bedrooms, bathrooms, floor_area_sqft, description, amenities, status)
SELECT 1, (SELECT id FROM users WHERE username = 'chaminda@agent' LIMIT 1), 'Oceanview Luxury Architectural Villa', 'Villa', 'Sale', 125000000.00, 'Marine Drive, Galle', 'No. 42 Marine Drive, Galle', 5, 4, 4200, 'Breathtaking 5-bedroom architecturally designed coastal villa featuring panoramic ocean views, private infinity pool, and expansive landscaped gardens.', 'Swimming Pool, 24/7 Security, Air Conditioning, Landscaped Garden, Covered Garage, Backup Generator', 'Active'
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE id = 1);

INSERT INTO properties (id, agent_id, title, property_type, listing_type, price, location, address, bedrooms, bathrooms, floor_area_sqft, description, amenities, status)
SELECT 2, (SELECT id FROM users WHERE username = 'chaminda@agent' LIMIT 1), 'Skyline Executive View Penthouse', 'Penthouse', 'Rent', 450000.00, 'Alfred House Gardens, Colombo 03', 'Level 28, Alfred House Gardens, Colombo 03', 3, 3, 2650, 'Ultra-luxury modern penthouse offering 360-degree cityscape and ocean panoramas, floor-to-ceiling soundproof glass, and concierge amenities.', 'Gymnasium, 24/7 Security, Air Conditioning, Swimming Pool, Backup Generator, Covered Garage', 'Active'
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE id = 2);

INSERT INTO properties (id, agent_id, title, property_type, listing_type, price, location, address, bedrooms, bathrooms, floor_area_sqft, description, amenities, status)
SELECT 3, (SELECT id FROM users WHERE username = 'chaminda@agent' LIMIT 1), 'Greenfield Modern Family Residence', 'House', 'Sale', 68000000.00, 'Pelawatta, Battaramulla', '15 Greenfield Lane, Pelawatta, Battaramulla', 4, 3, 3100, 'Spacious contemporary family residence located in an upscale gated neighborhood close to international schools and lakeside recreational parks.', 'Covered Garage, Landscaped Garden, 24/7 Security, Solar Power, Staff Quarters, Backup Generator', 'Active'
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE id = 3);

INSERT INTO properties (id, agent_id, title, property_type, listing_type, price, location, address, bedrooms, bathrooms, floor_area_sqft, description, amenities, status)
SELECT 4, (SELECT id FROM users WHERE username = 'chaminda@agent' LIMIT 1), 'Lakefront Modern Architectural Villa', 'Villa', 'Sale', 95000000.00, 'Victoria Lake, Kandy', '88 Lakeside Road, Digana, Kandy', 4, 4, 3800, 'Serene waterfront retreat boasting tranquil lake views, cantilevered timber decks, solar power integration, and private boat dock.', 'Solar Power, Swimming Pool, Landscaped Garden, 24/7 Security, Covered Garage, Backup Generator', 'Active'
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE id = 4);

-- Seed Property Images
INSERT INTO property_images (property_id, image_url, is_primary)
SELECT 1, 'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=1200&q=80', TRUE
WHERE NOT EXISTS (SELECT 1 FROM property_images WHERE property_id = 1 AND is_primary = TRUE);

INSERT INTO property_images (property_id, image_url, is_primary)
SELECT 2, 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=1200&q=80', TRUE
WHERE NOT EXISTS (SELECT 1 FROM property_images WHERE property_id = 2 AND is_primary = TRUE);

INSERT INTO property_images (property_id, image_url, is_primary)
SELECT 3, 'https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=1200&q=80', TRUE
WHERE NOT EXISTS (SELECT 1 FROM property_images WHERE property_id = 3 AND is_primary = TRUE);

INSERT INTO property_images (property_id, image_url, is_primary)
SELECT 4, 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=1200&q=80', TRUE
WHERE NOT EXISTS (SELECT 1 FROM property_images WHERE property_id = 4 AND is_primary = TRUE);

-- Seed Sample Bookings
INSERT INTO bookings (customer_id, property_id, viewing_date, time_slot, status, notes)
SELECT (SELECT id FROM users WHERE username = 'dilshan@user' LIMIT 1), 1, CURDATE() + INTERVAL 2 DAY, '10:00 AM - 11:00 AM', 'Approved', 'Interested in exploring purchase options and floor plans.'
WHERE NOT EXISTS (SELECT 1 FROM bookings WHERE customer_id = (SELECT id FROM users WHERE username = 'dilshan@user' LIMIT 1) AND property_id = 1);

-- Seed Sample Inquiries
INSERT INTO inquiries (customer_id, property_id, message, agent_reply, status)
SELECT (SELECT id FROM users WHERE username = 'dilshan@user' LIMIT 1), 1, 'Hello, is the price slightly negotiable for an upfront payment?', 'Yes Dilshan, we can discuss terms during your viewing appointment!', 'Replied'
WHERE NOT EXISTS (SELECT 1 FROM inquiries WHERE customer_id = (SELECT id FROM users WHERE username = 'dilshan@user' LIMIT 1) AND property_id = 1);

-- Seed Sample Reviews
INSERT INTO reviews (customer_id, property_id, rating, comment, status)
SELECT (SELECT id FROM users WHERE username = 'dilshan@user' LIMIT 1), 1, 5, 'Spectacular property with incredible sea views and top quality finishes throughout.', 'Approved'
WHERE NOT EXISTS (SELECT 1 FROM reviews WHERE customer_id = (SELECT id FROM users WHERE username = 'dilshan@user' LIMIT 1) AND property_id = 1);
