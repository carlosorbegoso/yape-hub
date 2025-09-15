package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.sky.dto.ApiResponse;
import org.sky.dto.qr.*;
import org.sky.model.AffiliationCode;
import org.sky.model.Branch;
import org.sky.model.QrCode;
import org.sky.repository.BranchRepository;
import org.sky.repository.AffiliationCodeRepository;
import org.sky.repository.QrCodeRepository;
import org.sky.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class QrService {

  @Inject
  BranchRepository branchRepository;

  @Inject
  AffiliationCodeRepository affiliationCodeRepository;

  @Inject
  QrCodeRepository qrCodeRepository;


  @WithTransaction
  public Uni<ApiResponse<AffiliationCodeResponse>> generateAffiliationCode(Long adminId,
                                                                           Integer expirationHours,
                                                                           Integer maxUses,
                                                                           Long branchId,
                                                                           String notes) {
    Uni<Branch> branchUni;

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
      AffiliationCode code = new AffiliationCode();
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
    return "AFF_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
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

}
