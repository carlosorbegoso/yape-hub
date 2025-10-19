package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.mindrot.jbcrypt.BCrypt;

import org.sky.dto.request.seller.AffiliateSellerRequest;
import org.sky.dto.response.ApiResponse;
import org.sky.dto.response.common.PaginationInfo;
import org.sky.dto.response.seller.SellerListResponse;
import org.sky.dto.response.seller.SellerResponse;
import org.sky.dto.response.seller.SellerRegistrationResponse;
import org.sky.model.SellerEntity;
import org.sky.model.UserEntityEntity;
import org.sky.model.UserRole;
import org.sky.repository.AffiliationCodeRepository;
import org.sky.repository.BranchRepository;
import org.sky.repository.SellerRepository;
import org.sky.repository.UserRepository;
import org.sky.exception.ValidationException;
import org.sky.util.jwt.JwtExtractor;
import org.sky.util.jwt.JwtValidator;
import org.sky.util.jwt.JwtGenerator;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SellerService {
    
    private static final Logger log = Logger.getLogger(SellerService.class);
    
    @Inject
    AffiliationCodeRepository affiliationCodeRepository;
    
    @Inject
    UserRepository userRepository;

    @Inject
    SubscriptionService subscriptionService;
    
    @Inject
    BranchRepository branchRepository;
    
    @Inject
    SellerRepository sellerRepository;

    
    @Inject
    JwtGenerator jwtGenerator;

  @WithTransaction
    public Uni<ApiResponse<SellerRegistrationResponse>> affiliateSeller(Long adminId, AffiliateSellerRequest request) {
        return affiliationCodeRepository.findByAffiliationCode(request.affiliationCode())
                .chain(affiliationCode -> {
                    if (affiliationCode == null || !affiliationCode.isActive) {
                        return Uni.createFrom().item(ApiResponse.<SellerRegistrationResponse>error("Código de afiliación inválido"));
                    }
                    
                    if (affiliationCode.remainingUses <= 0) {
                        return Uni.createFrom().item(ApiResponse.<SellerRegistrationResponse>error("Código de afiliación agotado"));
                    }
                    
                    // Verificar límites de vendedores según el plan
                    return sellerRepository.findByAdminId(adminId)
                            .chain(existingSellers -> {
                                int currentSellerCount = existingSellers.size();
                                int newSellerCount = currentSellerCount + 1;

                                return subscriptionService.checkSubscriptionLimits(adminId, newSellerCount)
                                        .chain(withinLimits -> {
                                            log.info("🔍 SubscriptionService.checkSubscriptionLimits() - AdminId: " + adminId + ", SellersNeeded: " + newSellerCount + ", WithinLimits: " + withinLimits);
                                            if (!withinLimits) {
                                                // Obtener información detallada del límite para el mensaje de error
                                                return subscriptionService.getSellerLimitsInfo(adminId)
                                                        .map(limitsInfo -> {
                                                            String errorMessage = String.format(
                                                                "Límite de vendedores excedido. Plan actual: %s (máximo %d vendedores). " +
                                                                "Intenta agregar %d vendedores pero ya tienes %d. " +
                                                                "Considera actualizar tu plan de suscripción.",
                                                                limitsInfo.planName(),
                                                                limitsInfo.maxSellers(),
                                                                newSellerCount,
                                                                currentSellerCount
                                                            );
                                                            return ApiResponse.<SellerRegistrationResponse>error(errorMessage);
                                                        });
                                            }
                                            
                                            // Verificar si el teléfono ya existe
                                            return sellerRepository.findByPhone(request.phone())
                                                    .chain(existingSeller -> {
                                                        if (existingSeller != null) {
                                                            return Uni.createFrom().item(ApiResponse.<SellerRegistrationResponse>error("El número de teléfono ya está registrado"));
                                                        }
                                                        
                                                        // Generar email automático basado en el teléfono
                                                        String autoEmail = "seller_" + request.phone().replaceAll("[^0-9]", "") + "@yapechamo.com";
                                                        
                                                        return userRepository.findByEmail(autoEmail)
                                        .chain(existingUser -> {
                                            if (existingUser != null) {
                                                return Uni.createFrom().item(ApiResponse.<SellerRegistrationResponse>error("Error interno: email automático ya existe"));
                                            }
                                            
                                            return branchRepository.findById(affiliationCode.branch.id)
                                        .chain(branch -> {
                                            if (branch == null || !branch.admin.id.equals(adminId)) {
                                                return Uni.createFrom().item(ApiResponse.<SellerRegistrationResponse>error("Sucursal no encontrada"));
                                            }
                                            
                                            // Create user with auto-generated credentials
                                            UserEntityEntity user = new UserEntityEntity();
                                            user.email = autoEmail;
                                            user.password = BCrypt.hashpw("auto_password_" + request.phone(), BCrypt.gensalt());
                                            user.role = UserRole.SELLER;
                                            user.isVerified = true;
                                            
                                            return userRepository.persist(user)
                                                    .chain(persistedUser -> {
                                                        // Create seller
                                                        SellerEntity seller = new SellerEntity();
                                                        seller.user = persistedUser;
                                                        seller.sellerName = request.sellerName();
                                                        seller.email = autoEmail;
                                                        seller.phone = request.phone();
                                                        seller.branch = branch;
                                                        seller.affiliationCode = request.affiliationCode();
                                                        seller.affiliationDate = java.time.LocalDateTime.now();
                                                        
                                                        return sellerRepository.persist(seller)
                                                                .chain(persistedSeller -> {
                                                                    // Update affiliation code usage
                                                                    affiliationCode.remainingUses--;
                                                                    return affiliationCodeRepository.persist(affiliationCode)
                                                                            .map(updatedCode -> {
                                                                                // Generar token JWT para el seller
                                                                                String token = jwtGenerator.generateAccessToken(
                                                                                    persistedUser.id,
                                                                                    UserRole.SELLER,
                                                                                    persistedSeller.id
                                                                                );
                                                                                
                                                                                SellerRegistrationResponse response = new SellerRegistrationResponse(
                                                                                        persistedSeller.id, persistedSeller.sellerName, 
                                                                                        persistedSeller.email, persistedSeller.phone,
                                                                                        persistedSeller.branch.id, persistedSeller.branch.name, 
                                                                                        persistedSeller.isActive, persistedSeller.isOnline,
                                                                                        persistedSeller.totalPayments, persistedSeller.totalAmount, 
                                                                                        persistedSeller.lastPayment, persistedSeller.affiliationDate,
                                                                                        token
                                                                                );
                                                                                return ApiResponse.success("Vendedor registrado exitosamente con token", response);
                                                                            });
                                                                });
                                                    });
                                        });
                                                    });
                                            });
                                        });
                            });
                });
    }
    
    public Uni<ApiResponse<SellerListResponse>> listSellers(Long adminId, int page, int limit, Long branchId, String status) {
        Uni<List<SellerEntity>> sellersUni;
        
        if (branchId != null) {
            sellersUni = sellerRepository.findByBranchId(branchId);
        } else {
            sellersUni = sellerRepository.findByAdminId(adminId);
        }
        
        return sellersUni.map(sellers -> {
            // Filter by status
            if (status != null && !status.equals("all")) {
                boolean isActive = status.equals("active");
                sellers = sellers.stream()
                        .filter(seller -> seller.isActive.equals(isActive))
                        .collect(Collectors.toList());
            }
            
            // Pagination
            int totalItems = sellers.size();
            int totalPages = (int) Math.ceil((double) totalItems / limit);
            int startIndex = (page - 1) * limit;
            int endIndex = Math.min(startIndex + limit, totalItems);
            
            List<SellerEntity> paginatedSellers = sellers.subList(startIndex, endIndex);
            
            List<SellerResponse> sellerResponses = paginatedSellers.stream()
                    .map(seller -> new SellerResponse(
                            seller.id, seller.sellerName, seller.email, seller.phone,
                            seller.branch.id, seller.branch.name, seller.isActive, seller.isOnline,
                            seller.totalPayments, seller.totalAmount, seller.lastPayment, seller.affiliationDate
                    ))
                    .collect(Collectors.toList());
            
              PaginationInfo pagination = new   PaginationInfo(
                    page, totalPages, totalItems, limit, page < totalPages, page > 1
            );
            
            SellerListResponse response = new SellerListResponse(sellerResponses, pagination);
            
            return ApiResponse.success("Vendedores obtenidos exitosamente", response);
        });
    }
    
    @WithTransaction
    public Uni<ApiResponse<SellerResponse>> updateSeller(Long adminId, Long sellerId, String name, String phone, Boolean isActive) {
        return sellerRepository.findById(sellerId)
                .chain(seller -> {
                    if (seller == null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("sellerId", sellerId.toString(), "Vendedor no encontrado")
                        );
                    }
                    if (!seller.branch.admin.id.equals(adminId)) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("adminId", adminId.toString(), "No tienes permisos para actualizar este vendedor")
                        );
                    }
                    
                    if (name != null) {
                        seller.sellerName = name;
                    }
                    if (phone != null) {
                        seller.phone = phone;
                    }
                    if (isActive != null) {
                        seller.isActive = isActive;
                    }
                    
                    return sellerRepository.persist(seller)
                            .map(persistedSeller -> {
                                SellerResponse response = new SellerResponse(
                                        persistedSeller.id, persistedSeller.sellerName, persistedSeller.email, persistedSeller.phone,
                                        persistedSeller.branch.id, persistedSeller.branch.name, persistedSeller.isActive, persistedSeller.isOnline,
                                        persistedSeller.totalPayments, persistedSeller.totalAmount, persistedSeller.lastPayment, persistedSeller.affiliationDate
                                );
                                
                                return ApiResponse.success("Vendedor actualizado exitosamente", response);
                            });
                });
    }
    
    @WithTransaction
    public Uni<ApiResponse<String>> deleteSeller(Long adminId, Long sellerId, String action, String reason) {
        return sellerRepository.findById(sellerId)
                .chain(seller -> {
                    if (seller == null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("sellerId", sellerId.toString(), "Vendedor no encontrado")
                        );
                    }
                    if (!seller.branch.admin.id.equals(adminId)) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("adminId", adminId.toString(), "No tienes permisos para dar de baja este vendedor")
                        );
                    }
                    
                    if ("pause".equals(action)) {
                        seller.isActive = false;
                        return sellerRepository.persist(seller)
                                .map(persistedSeller -> ApiResponse.success("Vendedor dado de baja exitosamente (soft delete)"));
                    } else if ("delete".equals(action)) {
                        // Para hard delete, primero eliminamos el usuario asociado
                        if (seller.user != null) {
                            return userRepository.delete(seller.user)
                                    .chain(deletedUser -> sellerRepository.delete(seller))
                                    .map(deletedSeller -> ApiResponse.success("Vendedor eliminado permanentemente de la base de datos"));
                        } else {
                            return sellerRepository.delete(seller)
                                    .map(deletedSeller -> ApiResponse.success("Vendedor eliminado permanentemente de la base de datos"));
                        }
                    } else {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("action", action, "Acción inválida. Valores permitidos: 'pause' o 'delete'")
                        );
                    }
                });
    }
  public Uni<ApiResponse<SellerListResponse>> getSellersByAdmin(Long adminId, int page, int limit, LocalDate startDate, LocalDate endDate) {

    // Normalize pagination parameters
    final int finalPage = Math.max(page, 1);
    final int finalLimit = limit < 1 || limit > 100 ? 20 : limit;

    return sellerRepository.findSellersByAdminWithPagination(
            adminId,
            startDate.atStartOfDay(),
            endDate.atTime(23,59,59),
            finalPage,
            finalLimit
        )
        .map(result -> {
          int totalPages = result.totalCount() > 0
              ? (int) Math.ceil((double) result.totalCount() / finalLimit)
              : 1;

          List<SellerResponse> sellerResponses = result.sellers().stream()
              .map(seller -> new SellerResponse(
                  seller.id,
                  seller.sellerName,
                  seller.email,
                  seller.phone,
                  seller.branch.id,
                  seller.branch.name,
                  seller.isActive,
                  seller.isOnline,
                  seller.totalPayments,
                  seller.totalAmount,
                  seller.lastPayment,
                  seller.affiliationDate
              ))
              .toList();

          PaginationInfo pagination = new PaginationInfo(
              1, // Always use page 1 for zero count
              totalPages, // Total pages (at least 1)
              result.totalCount(), // Total items
              finalLimit, // Items per page
              false, // hasNext
              false // hasPrevious
          );

          SellerListResponse listResponse = new SellerListResponse(
              sellerResponses,
              pagination
          );


          return ApiResponse.success(
              result.totalCount() > 0
                  ? "Vendedores obtenidos exitosamente"
                  : "No se encontraron vendedores en el rango de fechas especificado",
              listResponse
          );
        });
  }
}
