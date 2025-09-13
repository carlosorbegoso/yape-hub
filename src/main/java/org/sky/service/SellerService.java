package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.mindrot.jbcrypt.BCrypt;
import org.sky.dto.ApiResponse;
import org.sky.dto.seller.AffiliateSellerRequest;
import org.sky.dto.seller.SellerListResponse;
import org.sky.dto.seller.SellerResponse;
import org.sky.model.AffiliationCode;
import org.sky.model.Branch;
import org.sky.model.Seller;
import org.sky.model.User;
import org.sky.repository.AffiliationCodeRepository;
import org.sky.repository.BranchRepository;
import org.sky.repository.SellerRepository;
import org.sky.repository.UserRepository;
import org.sky.exception.ValidationException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class SellerService {
    
    @Inject
    AffiliationCodeRepository affiliationCodeRepository;
    
    @Inject
    UserRepository userRepository;
    
    @Inject
    BranchRepository branchRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @WithTransaction
    public Uni<ApiResponse<SellerResponse>> affiliateSeller(Long adminId, AffiliateSellerRequest request) {
        return affiliationCodeRepository.findByAffiliationCode(request.affiliationCode())
                .chain(affiliationCode -> {
                    if (affiliationCode == null || !affiliationCode.isActive) {
                        return Uni.createFrom().item(ApiResponse.<SellerResponse>error("Código de afiliación inválido"));
                    }
                    
                    if (affiliationCode.remainingUses <= 0) {
                        return Uni.createFrom().item(ApiResponse.<SellerResponse>error("Código de afiliación agotado"));
                    }
                    
                    // Verificar si el teléfono ya existe
                    return sellerRepository.findByPhone(request.phone())
                            .chain(existingSeller -> {
                                if (existingSeller != null) {
                                    return Uni.createFrom().item(ApiResponse.<SellerResponse>error("El número de teléfono ya está registrado"));
                                }
                                
                                // Generar email automático basado en el teléfono
                                String autoEmail = "seller_" + request.phone().replaceAll("[^0-9]", "") + "@yapechamo.com";
                                
                                return userRepository.findByEmail(autoEmail)
                                        .chain(existingUser -> {
                                            if (existingUser != null) {
                                                return Uni.createFrom().item(ApiResponse.<SellerResponse>error("Error interno: email automático ya existe"));
                                            }
                                            
                                            return branchRepository.findById(affiliationCode.branch.id)
                                        .chain(branch -> {
                                            if (branch == null || !branch.admin.id.equals(adminId)) {
                                                return Uni.createFrom().item(ApiResponse.<SellerResponse>error("Sucursal no encontrada"));
                                            }
                                            
                                            // Create user with auto-generated credentials
                                            User user = new User();
                                            user.email = autoEmail;
                                            user.password = BCrypt.hashpw("auto_password_" + request.phone(), BCrypt.gensalt());
                                            user.role = User.UserRole.SELLER;
                                            user.isVerified = true;
                                            
                                            return userRepository.persist(user)
                                                    .chain(persistedUser -> {
                                                        // Create seller
                                                        Seller seller = new Seller();
                                                        seller.user = persistedUser;
                                                        seller.sellerName = request.sellerName();
                                                        seller.email = autoEmail;
                                                        seller.phone = request.phone();
                                                        seller.branch = branch;
                                                        seller.affiliationCode = request.affiliationCode();
                                                        
                                                        return sellerRepository.persist(seller)
                                                                .chain(persistedSeller -> {
                                                                    // Update affiliation code usage
                                                                    affiliationCode.remainingUses--;
                                                                    return affiliationCodeRepository.persist(affiliationCode)
                                                                            .map(updatedCode -> {
                                                                                SellerResponse response = new SellerResponse(
                                                                                        persistedSeller.id, persistedSeller.sellerName, 
                                                                                        persistedSeller.email, persistedSeller.phone,
                                                                                        persistedSeller.branch.id, persistedSeller.branch.name, 
                                                                                        persistedSeller.isActive, persistedSeller.isOnline,
                                                                                        persistedSeller.totalPayments, persistedSeller.totalAmount, 
                                                                                        persistedSeller.lastPayment, persistedSeller.affiliationDate
                                                                                );
                                                                                return ApiResponse.success("Vendedor afiliado exitosamente", response);
                                                                            });
                                                                });
                                                    });
                                        });
                                        });
                            });
                });
    }
    
    public Uni<ApiResponse<SellerListResponse>> listSellers(Long adminId, int page, int limit, Long branchId, String status) {
        Uni<List<Seller>> sellersUni;
        
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
            
            List<Seller> paginatedSellers = sellers.subList(startIndex, endIndex);
            
            List<SellerResponse> sellerResponses = paginatedSellers.stream()
                    .map(seller -> new SellerResponse(
                            seller.id, seller.sellerName, seller.email, seller.phone,
                            seller.branch.id, seller.branch.name, seller.isActive, seller.isOnline,
                            seller.totalPayments, seller.totalAmount, seller.lastPayment, seller.affiliationDate
                    ))
                    .collect(Collectors.toList());
            
            SellerListResponse.PaginationInfo pagination = new SellerListResponse.PaginationInfo(
                    page, totalPages, totalItems, limit
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
    
    /**
     * Obtiene todos los vendedores afiliados a un administrador específico con paginación
     */
    public Uni<ApiResponse<SellerListResponse>> getSellersByAdmin(Long adminId, int page, int limit) {
        // Validar parámetros de paginación
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20; // Máximo 100 elementos por página
        
        // Hacer las variables finales para usar en lambda
        final int finalPage = page;
        final int finalLimit = limit;
        final int offset = (finalPage - 1) * finalLimit;
        
        return sellerRepository.findByAdminId(adminId)
                .chain(allSellers -> {
                    // Calcular información de paginación
                    int totalItems = allSellers.size();
                    int totalPages = (int) Math.ceil((double) totalItems / finalLimit);
                    
                    // Aplicar paginación
                    List<Seller> paginatedSellers = allSellers.stream()
                            .skip(offset)
                            .limit(finalLimit)
                            .collect(Collectors.toList());
                    
                    List<SellerResponse> sellerResponses = paginatedSellers.stream()
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
                            .collect(Collectors.toList());
                    
                    SellerListResponse.PaginationInfo pagination = new SellerListResponse.PaginationInfo(
                            finalPage, // currentPage
                            totalPages, // totalPages
                            totalItems, // totalItems
                            finalLimit // itemsPerPage
                    );
                    
                    SellerListResponse listResponse = new SellerListResponse(
                            sellerResponses,
                            pagination
                    );
                    
                    return Uni.createFrom().item(ApiResponse.success("Vendedores obtenidos exitosamente", listResponse));
                });
    }
}
