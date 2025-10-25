-- Datos de prueba para testing
-- Este archivo se ejecuta automáticamente en el perfil de test

-- Insertar datos de prueba para usuarios
INSERT INTO users (id, username, password_hash, role, created_at, updated_at) VALUES 
(1, 'admin@test.com', '$2a$10$test.hash', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'seller@test.com', '$2a$10$test.hash', 'SELLER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insertar datos de prueba para administradores
INSERT INTO admins (id, user_id, business_name, contact_name, email, phone, created_at, updated_at) VALUES 
(1, 1, 'Test Business', 'Test Admin', 'admin@test.com', '+51999999999', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insertar datos de prueba para sucursales
INSERT INTO branches (id, admin_id, branch_name, address, phone, is_active, created_at, updated_at) VALUES 
(1, 1, 'Test Branch', 'Test Address 123', '+51888888888', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insertar datos de prueba para vendedores
INSERT INTO sellers (id, user_id, seller_name, email, phone, branch_id, is_active, affiliation_code, affiliation_date, created_at, updated_at) VALUES 
(1, 2, 'Test Seller', 'seller@test.com', '+51777777777', 1, true, 'TEST001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);