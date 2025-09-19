-- Agregar columna deduplication_hash a payment_notifications
ALTER TABLE payment_notifications 
ADD COLUMN deduplication_hash VARCHAR(255) NOT NULL DEFAULT '';

-- Crear índice único para deduplication_hash
CREATE UNIQUE INDEX idx_payment_notifications_deduplication_hash 
ON payment_notifications (deduplication_hash);

-- Actualizar registros existentes con un hash temporal
UPDATE payment_notifications 
SET deduplication_hash = CONCAT('legacy_', id, '_', UNIX_TIMESTAMP(created_at))
WHERE deduplication_hash = '';
