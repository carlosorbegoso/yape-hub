package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import org.sky.dto.response.ApiResponse;
import org.sky.dto.response.qr.*;
import org.sky.exception.ValidationException;
import org.sky.model.AffiliationCodeEntity;
import org.sky.model.BranchEntity;
import org.sky.repository.BranchRepository;
import org.sky.repository.AffiliationCodeRepository;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import org.sky.dto.response.auth.SellerLoginWithAffiliationResponse;

// Imports para generar imagen QR
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.sky.service.auth.AuthService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@ApplicationScoped
public class QrService {

  @Inject
  BranchRepository branchRepository;

  @Inject
  AffiliationCodeRepository affiliationCodeRepository;

  @Inject
  AuthService authService;


  @WithTransaction
  public Uni<ApiResponse<AffiliationCodeResponse>> generateAffiliationCode(Long adminId,
                                                                           Integer expirationHours,
                                                                           Integer maxUses,
                                                                           Long branchId,
                                                                           String notes) {
    Uni<BranchEntity> branchUni;

    if (branchId != null) {
      branchUni = branchRepository.findById(branchId)
          .chain(branch -> {
            if (branch == null || !branch.admin.id.equals(adminId)) {
              return Uni.createFrom().failure(new RuntimeException("Sucursal no encontrada"));
            }
            return Uni.createFrom().item(branch);
          });
    } else {
      // Get first branch of admin
      branchUni = branchRepository.findByAdminId(adminId)
          .chain(branches -> {
            if (branches.isEmpty()) {
              return Uni.createFrom().failure(new RuntimeException("No hay sucursales disponibles"));
            }
            return Uni.createFrom().item(branches.get(0));
          });
    }

    return branchUni.chain(branch -> {
      // Generate affiliation code
      String affiliationCode = generateAffiliationCodeString();

      // Calculate expiration
      LocalDateTime expiresAt = null;
      if (expirationHours != null && expirationHours > 0) {
        expiresAt = LocalDateTime.now().plusHours(expirationHours);
      }

      // Create affiliation code
      AffiliationCodeEntity code = new AffiliationCodeEntity();
      code.affiliationCode = affiliationCode;
      code.branch = branch;
      code.expiresAt = expiresAt;
      code.maxUses = maxUses != null ? maxUses : 1;
      code.remainingUses = code.maxUses;
      code.notes = notes;

      return affiliationCodeRepository.persist(code)
          .map(persistedCode -> {
            AffiliationCodeResponse response = new AffiliationCodeResponse(
                persistedCode.affiliationCode, persistedCode.expiresAt, persistedCode.maxUses, persistedCode.remainingUses, persistedCode.branch.id
            );
            return ApiResponse.success("Código de afiliación generado exitosamente", response);
          });
    });
  }

  public Uni<ApiResponse<ValidateAffiliationCodeResponse>> validateAffiliationCode(String affiliationCode) {
    return affiliationCodeRepository.findByAffiliationCode(affiliationCode)
        .map(code -> {
          if (code == null || !code.isActive) {
            throw ValidationException.invalidField("affiliationCode", affiliationCode, "Código de afiliación no encontrado o inactivo");
          }

          if (code.remainingUses <= 0) {
            throw ValidationException.invalidField("affiliationCode", affiliationCode, "Código de afiliación agotado");
          }

          if (code.expiresAt != null && code.expiresAt.isBefore(LocalDateTime.now())) {
            throw ValidationException.invalidField("affiliationCode", affiliationCode, "Código de afiliación expirado");
          }

          ValidateAffiliationCodeResponse response = new ValidateAffiliationCodeResponse(
              true, code.branch.admin.businessName, code.branch.name, code.expiresAt
          );

          return ApiResponse.success("Código de afiliación válido", response);
        });
  }


  private String generateAffiliationCodeString() {
    // Generar código más corto: AFF + 6 dígitos aleatorios
    Random random = new Random();
    int randomNumber = random.nextInt(900000) + 100000; // Genera número entre 100000 y 999999
    return "AFF" + randomNumber;
  }

  /**
   * Obtiene el adminId asociado a un código de afiliación
   */
  public Uni<Long> getAdminIdFromAffiliationCode(String affiliationCode) {
    return affiliationCodeRepository.findByAffiliationCode(affiliationCode)
        .chain(affiliationCodeEntity -> {
          if (affiliationCodeEntity == null) {
            return Uni.createFrom().failure(new RuntimeException("Código de afiliación no encontrado"));
          }

          // Obtener el adminId a través de la sucursal
          return branchRepository.findById(affiliationCodeEntity.branch.id)
              .map(branch -> {
                if (branch == null) {
                  throw new RuntimeException("Sucursal no encontrada");
                }
                return branch.admin.id;
              });
        });
  }

  /**
   * Genera un QR con Base64 que contiene el código de afiliación
   */
  @WithTransaction
  public Uni<ApiResponse<QrBase64Response>> generateQrBase64(String affiliationCode) {
    return affiliationCodeRepository.findByAffiliationCode(affiliationCode)
        .chain(code -> {
          if (code == null || !code.isActive) {
            return Uni.createFrom().failure(new RuntimeException("Código de afiliación no encontrado o inactivo"));
          }

          if (code.remainingUses <= 0) {
            return Uni.createFrom().failure(new RuntimeException("Código de afiliación agotado"));
          }

          if (code.expiresAt != null && code.expiresAt.isBefore(LocalDateTime.now())) {
            return Uni.createFrom().failure(new RuntimeException("Código de afiliación expirado"));
          }

          // Crear datos del QR (JSON con información del código de afiliación)
          String qrData = String.format(
            "{\"affiliationCode\":\"%s\",\"branchId\":%d,\"adminId\":%d,\"expiresAt\":\"%s\",\"maxUses\":%d}",
            code.affiliationCode,
            code.branch.id,
            code.branch.admin.id,
            code.expiresAt != null ? code.expiresAt.toString() : "null",
            code.maxUses
          );

          try {
            // Generar imagen QR real
            String qrBase64 = generateQRCodeImage(qrData);
            
            QrBase64Response response = new QrBase64Response(
              code.affiliationCode,
              qrBase64,
              code.expiresAt != null ? code.expiresAt.toString() : null,
              code.maxUses,
              code.remainingUses,
              code.branch.name,
              code.branch.admin.businessName
            );

            return Uni.createFrom().item(ApiResponse.success("QR generado exitosamente", response));
          } catch (Exception e) {
            return Uni.createFrom().failure(new RuntimeException("Error generando imagen QR: " + e.getMessage()));
          }
        });
  }

  /**
   * Genera una imagen QR real y la convierte a Base64
   */
  private String generateQRCodeImage(String data) throws WriterException, IOException {
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 300, 300);
    
    ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
    MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
    byte[] pngData = pngOutputStream.toByteArray();
    
    return Base64.getEncoder().encodeToString(pngData);
  }

  /**
   * Login de vendedor usando QR con Base64
   */
  @WithTransaction
  public Uni<ApiResponse<SellerLoginWithAffiliationResponse>> loginWithQr(String qrData, String phone) {
    try {
      // Decodificar Base64
      String decodedData = new String(Base64.getDecoder().decode(qrData), StandardCharsets.UTF_8);
      
      // Parsear JSON simple (en un caso real usarías Jackson)
      String affiliationCode = extractAffiliationCodeFromJson(decodedData);
      
      if (affiliationCode == null) {
        return Uni.createFrom().failure(new RuntimeException("QR inválido: no se pudo extraer el código de afiliación"));
      }

      // Validar el código de afiliación
      return validateAffiliationCode(affiliationCode)
          .chain(validationResponse -> {
            if (!validationResponse.isSuccess()) {
              return Uni.createFrom().failure(new RuntimeException("Código de afiliación inválido: " + validationResponse.message()));
            }

            // Realizar login con el código de afiliación y teléfono
            return authService.loginByPhoneWithAffiliation(phone, affiliationCode);
          });
    } catch (Exception e) {
      return Uni.createFrom().failure(new RuntimeException("Error decodificando QR: " + e.getMessage()));
    }
  }

  /**
   * Extrae el código de afiliación de un JSON simple
   */
  private String extractAffiliationCodeFromJson(String json) {
    try {
      // Buscar el patrón "affiliationCode":"valor"
      int startIndex = json.indexOf("\"affiliationCode\":\"");
      if (startIndex == -1) return null;
      
      startIndex += 19; // Longitud de "affiliationCode":"
      int endIndex = json.indexOf("\"", startIndex);
      if (endIndex == -1) return null;
      
      return json.substring(startIndex, endIndex);
    } catch (Exception e) {
      return null;
    }
  }

}
