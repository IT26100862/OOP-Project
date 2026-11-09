-- Seed Users for EstateX User Management System
-- Demonstrates the 3 system roles using role-identifying username formats (e.g. name@user, name@agent, name@admin)
-- All users have distinct unique passwords and unique usernames:

-- 1. Standard Customer User (format: name@user -> navigates to customer portal: ../user/dashboard.html)
INSERT INTO users (username, password, email, full_name, phone, role)
SELECT 'dilshan@user', 'userPass_dilshan123', 'dilshan.perera@gmail.com', 'Dilshan Perera', '+94 77 123 4567', 'CUSTOMER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'dilshan@user');

-- 2. Certified Real Estate Agent (format: name@agent -> navigates to agent portal: ../agent/dashboard.html)
INSERT INTO users (username, password, email, full_name, phone, role)
SELECT 'chaminda@agent', 'agentPass_chaminda123', 'chaminda.agent@estatex.lk', 'Chaminda Wijesekara', '+94 71 888 9900', 'AGENT'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'chaminda@agent');

-- 3. System Administrator (format: name@admin -> navigates to admin portal: ../admin/dashboard.html)
INSERT INTO users (username, password, email, full_name, phone, role)
SELECT 'admin@admin', 'adminPass_master123', 'admin@estatex.lk', 'System Administrator', '+94 11 234 5678', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin@admin');
