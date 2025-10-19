package org.sky.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.dto.response.notification.YapeNotificationResponse;
import org.sky.exception.ValidationException;

import java.util.Base64;
import java.util.regex.Pattern;

@ApplicationScoped
public class YapeDecryptionService {
    
    private static final Logger log = Logger.getLogger(YapeDecryptionService.class);
    
    // Patrones para extraer información de la notificación de Yape
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("S/\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern YAPE_CODE_PATTERN = Pattern.compile("cód\\.\\s*de\\s*seguridad\\s*es:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SENDER_NAME_PATTERN = Pattern.compile("([^\\s]+\\s+[^\\s]+\\s+[^\\s]+)\\s+te\\s+envió");
    
    /**
     * Desencripta la notificación de Yape y extrae los datos de la transacción
     */
    public YapeNotificationResponse decryptYapeNotification(String encryptedNotification, String deviceFingerprint) {
        try {
            // Validar que la notificación no esté vacía
            if (encryptedNotification == null || encryptedNotification.trim().isEmpty()) {
                throw ValidationException.invalidField("encryptedNotification", encryptedNotification, 
                    "Notificación no puede estar vacía");
            }
            
            // Simular desencriptación usando deviceFingerprint como clave
            String decryptedData = simulateDecryption(encryptedNotification, deviceFingerprint);
            
            // Extraer información de la transacción
            YapeTransactionData transactionData = extractTransactionData(decryptedData);
            
            // Validar datos extraídos
            validateTransactionData(transactionData);
            
            // Crear respuesta

          return new YapeNotificationResponse(
              System.currentTimeMillis(), // notificationId
              transactionData.transactionId,
              transactionData.amount,
              transactionData.senderPhone,
              transactionData.senderName,
              transactionData.receiverPhone,
              transactionData.status,
              java.time.LocalDateTime.now(),
              "Transacción procesada exitosamente"
          );

            
        } catch (Exception e) {
            log.error("❌ Error en desencriptación: " + e.getMessage());
            throw ValidationException.invalidField("encryptedNotification", encryptedNotification, 
                "Error desencriptando notificación: " + e.getMessage());
        }
    }
    
    /**
     * Desencripta la notificación usando el deviceFingerprint como clave
     */
    private String simulateDecryption(String encryptedNotification, String deviceFingerprint) {
        try {
            if (isValidBase64(encryptedNotification)) {
                byte[] decodedBytes = Base64.getDecoder().decode(encryptedNotification);
                String decodedString = new String(decodedBytes);
              return decryptWithFingerprint(decodedString, deviceFingerprint);
            } else {
                return encryptedNotification;
            }
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Error procesando notificación: " + e.getMessage());
        }
    }
    
    /**
     * Simula desencriptación usando deviceFingerprint como clave
     * En producción, aquí usarías algoritmos de encriptación reales
     */
    private String decryptWithFingerprint(String encryptedMessage, String deviceFingerprint) {
        try {
            StringBuilder decrypted = new StringBuilder();
            byte[] fingerprintBytes = deviceFingerprint.getBytes();
            
            for (int i = 0; i < encryptedMessage.length(); i++) {
                char encryptedChar = encryptedMessage.charAt(i);
                byte keyByte = fingerprintBytes[i % fingerprintBytes.length];
                char decryptedChar = (char) (encryptedChar ^ keyByte);
                decrypted.append(decryptedChar);
            }
            
            String result = decrypted.toString();
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error en desencriptación: " + e.getMessage());
            throw new IllegalArgumentException("Error desencriptando con fingerprint: " + e.getMessage());
        }
    }
    
    /**
     * Extrae los datos de la transacción del texto desencriptado
     */
    private YapeTransactionData extractTransactionData(String decryptedData) {
        YapeTransactionData data = new YapeTransactionData();

        
        // Extraer el texto del mensaje desde el JSON
        String messageText = extractTextFromJson(decryptedData);
        
        // Extraer monto (formato: "S/ 0.1")
        var amountMatcher = AMOUNT_PATTERN.matcher(messageText);
        if (amountMatcher.find()) {
            data.amount = Double.parseDouble(amountMatcher.group(1));
        } else {
            log.warn("⚠️ No se pudo extraer el monto del texto: " + messageText);
        }
        
        // Extraer código de Yape (formato: "El cód. de seguridad es: 148")
        var yapeCodeMatcher = YAPE_CODE_PATTERN.matcher(messageText);
        if (yapeCodeMatcher.find()) {
            String yapeCode = yapeCodeMatcher.group(1);
            
            // Generar Transaction ID único: YAPE_codigoYape
            // El deduplicationHash del frontend maneja la prevención de duplicados
            data.transactionId = "YAPE_" + yapeCode;
        } else {
            log.warn("⚠️ No se pudo extraer el código de Yape del texto: " + messageText);
        }
        
        // Extraer nombre del remitente (formato: "Carlos Orbegoso L. te envió")
        var senderNameMatcher = SENDER_NAME_PATTERN.matcher(messageText);
        if (senderNameMatcher.find()) {
            data.senderName = senderNameMatcher.group(1).trim();
        } else {
            log.warn("⚠️ No se pudo extraer el nombre del remitente del texto: " + messageText);
        }
        
        // Para teléfonos, usar valores por defecto ya que no están en el mensaje
        data.senderPhone = "000000000"; // Valor por defecto
        data.receiverPhone = "000000000"; // Valor por defecto
        
        // Estado por defecto para mensajes de Yape
        data.status = "COMPLETED";

        return data;
    }
    
    /**
     * Extrae el texto del mensaje desde el JSON desencriptado
     */
    private String extractTextFromJson(String jsonData) {
        try {
            // Buscar el campo "text" en el JSON
            var textMatcher = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]+)\"").matcher(jsonData);
            if (textMatcher.find()) {
                return textMatcher.group(1);
            }
            
            // Si no encuentra "text", buscar "fullText"
            var fullTextMatcher = Pattern.compile("\"fullText\"\\s*:\\s*\"([^\"]+)\"").matcher(jsonData);
            if (fullTextMatcher.find()) {
                return fullTextMatcher.group(1);
            }
            
            // Si no encuentra ninguno, usar el texto completo
            return jsonData;
        } catch (Exception e) {
            log.warn("⚠️ Error extrayendo texto del JSON: " + e.getMessage());
            return jsonData;
        }
    }
    
    /**
     * Valida los datos extraídos de la transacción
     */
    private void validateTransactionData(YapeTransactionData data) {
        if (data.amount == null || data.amount <= 0) {
            throw ValidationException.invalidField("amount", data.amount != null ? data.amount.toString() : "null", 
                "Monto de transacción inválido");
        }
        
        if (data.transactionId == null || data.transactionId.trim().isEmpty()) {
            throw ValidationException.invalidField("transactionId", data.transactionId, 
                "ID de transacción inválido");
        }
        
        if (data.senderName == null || data.senderName.trim().isEmpty()) {
            throw ValidationException.invalidField("senderName", data.senderName, 
                "Nombre del remitente inválido");
        }
    }
    
    /**
     * Valida si el string es Base64 válido
     */
    private boolean isValidBase64(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Clase interna para almacenar datos de transacción
     */
    private static class YapeTransactionData {
        String transactionId;
        Double amount;
        String senderPhone;
        String receiverPhone;
        String senderName;
        String status;
    }
}
