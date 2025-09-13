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
                    if (seller == null || !seller.branch.admin.id.equals(adminId)) {
                        return Uni.createFrom().item(ApiResponse.<SellerResponse>error("Vendedor no encontrado"));
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
                    if (seller == null || !seller.branch.admin.id.equals(adminId)) {
                        return Uni.createFrom().item(ApiResponse.<String>error("Vendedor no encontrado"));
                    }
                    
                    if ("pause".equals(action)) {
                        seller.isActive = false;
                        return sellerRepository.persist(seller)
                                .map(persistedSeller -> ApiResponse.success("Vendedor pausado exitosamente"));
                    } else if ("delete".equals(action)) {
                        return sellerRepository.delete(seller)
                                .chain(deletedSeller -> userRepository.delete(seller.user))
                                .map(deletedUser -> ApiResponse.success("Vendedor eliminado exitosamente"));
                    } else {
                        return Uni.createFrom().item(ApiResponse.<String>error("Acción inválida"));
                    }
                });
    }
    
    /**
     * Obtiene todos los vendedores afiliados a un administrador específico
     */
    public Uni<ApiResponse<SellerListResponse>> getSellersByAdmin(Long adminId) {
        return sellerRepository.findByAdminId(adminId)
                .map(sellers -> {
                    List<SellerResponse> sellerResponses = sellers.stream()
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
                            1, // currentPage
                            1, // totalPages
                            sellerResponses.size(), // totalItems
                            sellerResponses.size() // itemsPerPage
                    );
                    
                    SellerListResponse listResponse = new SellerListResponse(
                            sellerResponses,
                            pagination
                    );
                    
                    return ApiResponse.success("Vendedores obtenidos exitosamente", listResponse);
                });
    }
}
