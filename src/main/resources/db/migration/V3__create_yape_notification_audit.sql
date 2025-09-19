-- Crear tabla de auditoría para notificaciones de Yape
CREATE TABLE yape_notification_audit (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    encrypted_notification TEXT NOT NULL,
    device_fingerprint VARCHAR(255) NOT NULL,
    timestamp BIGINT NOT NULL,
    deduplication_hash VARCHAR(255) NOT NULL UNIQUE,
    decryption_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    decryption_error TEXT,
    extracted_amount DECIMAL(10,2),
    extracted_sender_name VARCHAR(255),
    extracted_yape_code VARCHAR(50),
    transaction_id VARCHAR(255),
    payment_notification_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Crear índices para optimizar consultas
CREATE INDEX idx_yape_audit_admin_id ON yape_notification_audit (admin_id);
CREATE INDEX idx_yape_audit_deduplication_hash ON yape_notification_audit (deduplication_hash);
CREATE INDEX idx_yape_audit_decryption_status ON yape_notification_audit (decryption_status);
CREATE INDEX idx_yape_audit_created_at ON yape_notification_audit (created_at);
CREATE INDEX idx_yape_audit_payment_notification_id ON yape_notification_audit (payment_notification_id);

-- Agregar comentarios para documentación
COMMENT ON TABLE yape_notification_audit IS 'Auditoría de notificaciones de Yape - Guarda todas las notificaciones recibidas';
COMMENT ON COLUMN yape_notification_audit.encrypted_notification IS 'Notificación encriptada original recibida';
COMMENT ON COLUMN yape_notification_audit.deduplication_hash IS 'Hash único para prevenir duplicados';
COMMENT ON COLUMN yape_notification_audit.decryption_status IS 'Estado de desencriptación: PENDING, SUCCESS, FAILED';
COMMENT ON COLUMN yape_notification_audit.payment_notification_id IS 'ID de la notificación de pago creada (si fue exitosa)';
