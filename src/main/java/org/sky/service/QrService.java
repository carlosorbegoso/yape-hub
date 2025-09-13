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
    public Uni<ApiResponse<QrResponse>> generateQrCode(Long adminId, GenerateQrRequest request) {
        Uni<Branch> branchUni;
        
        if (request.branchId() != null) {
            branchUni = branchRepository.findById(request.branchId())
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
            // Generate QR data
            String qrData = generateQrData(request.type(), branch.id);
            
            // Calculate expiration
            LocalDateTime expiresAt = null;
            if (request.expirationHours() != null && request.expirationHours() > 0) {
                expiresAt = LocalDateTime.now().plusHours(request.expirationHours());
            }
            
            // Create QR code
            QrCode qrCode = new QrCode();
            qrCode.qrData = qrData;
            qrCode.type = request.type();
            qrCode.branch = branch;
            qrCode.expiresAt = expiresAt;
            qrCode.maxUses = request.maxUses() != null ? request.maxUses() : 1;
            qrCode.remainingUses = qrCode.maxUses;
            qrCode.qrImageUrl = generateQrImageUrl(qrData);
            
            return qrCodeRepository.persist(qrCode)
                    .map(persistedQrCode -> {
                        QrResponse response = new QrResponse(
                                persistedQrCode.id, persistedQrCode.qrData, persistedQrCode.qrImageUrl, persistedQrCode.expiresAt,
                                persistedQrCode.maxUses, persistedQrCode.remainingUses, persistedQrCode.branch.id
                        );
                        return ApiResponse.success("Código QR generado exitosamente", response);
                    });
        });
    }
    
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
    
    public Uni<ApiResponse<List<QrResponse>>> getActiveQrCodes(Long adminId) {
        return branchRepository.findByAdminId(adminId)
                .chain(branches -> {
                    List<Uni<List<QrCode>>> qrCodesUnis = branches.stream()
                            .map(branch -> qrCodeRepository.findActiveByBranchId(branch.id))
                            .collect(Collectors.toList());
                    
                    return Uni.combine().all().unis(qrCodesUnis)
                            .with(qrCodesLists -> {
                                List<QrCode> allQrCodes = new ArrayList<>();
                                for (Object list : qrCodesLists) {
                                    if (list instanceof List) {
                                        allQrCodes.addAll((List<QrCode>) list);
                                    }
                                }
                                
                                List<QrResponse> responses = allQrCodes.stream()
                                        .map(qr -> new QrResponse(
                                                qr.id, qr.qrData, qr.qrImageUrl, qr.expiresAt,
                                                qr.maxUses, qr.remainingUses, qr.branch.id
                                        ))
                                        .collect(Collectors.toList());
                                
                                return ApiResponse.success("Códigos QR activos obtenidos exitosamente", responses);
                            });
                });
    }
    
    @WithTransaction
    public Uni<ApiResponse<String>> revokeQrCode(Long adminId, Long qrId) {
        return qrCodeRepository.findById(qrId)
                .chain(qrCode -> {
                    if (qrCode == null || !qrCode.branch.admin.id.equals(adminId)) {
                        return Uni.createFrom().item(ApiResponse.<String>error("Código QR no encontrado"));
                    }
                    
                    qrCode.isActive = false;
                    
                    return qrCodeRepository.persist(qrCode)
                            .map(persistedQrCode -> ApiResponse.success("Código QR revocado exitosamente"));
                });
    }
    
    private String generateQrData(QrCode.QrType type, Long branchId) {
        return "YAPE_" + type.name() + "_" + branchId + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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
    
    private String generateQrImageUrl(String qrData) {
        return "http://localhost:8080/qr/" + qrData + ".png";
    }
}
