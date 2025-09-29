package org.sky.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.exception.ValidationException;

import java.util.regex.Pattern;

@ApplicationScoped
public class DeviceFingerprintService {
    
    private static final Logger log = Logger.getLogger(DeviceFingerprintService.class);
    
    // Patrón para validar device fingerprint
    private static final Pattern DEVICE_FINGERPRINT_PATTERN = Pattern.compile("^[a-f0-9]{16,32}$");
    
    // Device fingerprints conocidos (en producción vendrían de una base de datos)
    private static final String[] KNOWN_DEVICE_FINGERPRINTS = {
        "a1b2c3d4e5f6g7h8",
        "b2c3d4e5f6g7h8i9",
        "c3d4e5f6g7h8i9j0",
        "d4e5f6g7h8i9j0k1"
    };
    
    /**
     * Valida el device fingerprint
     */
    public void validateDeviceFingerprint(String deviceFingerprint) {
        log.info("🔍 DeviceFingerprintService.validateDeviceFingerprint() - Validando device fingerprint");
        log.info("🔍 Device fingerprint: " + deviceFingerprint);
        
        // Validar formato
        if (deviceFingerprint == null || deviceFingerprint.trim().isEmpty()) {
            throw ValidationException.invalidField("deviceFingerprint", deviceFingerprint, 
                "Device fingerprint es requerido");
        }
        
        // Validar patrón
        if (!DEVICE_FINGERPRINT_PATTERN.matcher(deviceFingerprint).matches()) {
            throw ValidationException.invalidField("deviceFingerprint", deviceFingerprint, 
                "Formato de device fingerprint inválido. Debe ser hexadecimal de 16-32 caracteres");
        }
        
        // Validar si es un device fingerprint conocido
        if (!isKnownDeviceFingerprint(deviceFingerprint)) {
            log.warn("⚠️ Device fingerprint desconocido: " + deviceFingerprint);
            // En producción, podrías querer rechazar device fingerprints desconocidos
            // throw ValidationException.invalidField("deviceFingerprint", deviceFingerprint, 
            //     "Device fingerprint no autorizado");
        }
        
        log.info("✅ Device fingerprint válido: " + deviceFingerprint);
    }
    
    /**
     * Verifica si el device fingerprint es conocido
     */
    private boolean isKnownDeviceFingerprint(String deviceFingerprint) {
        for (String knownFingerprint : KNOWN_DEVICE_FINGERPRINTS) {
            if (knownFingerprint.equals(deviceFingerprint)) {
                return true;
            }
        }
        return false;
    }

}
