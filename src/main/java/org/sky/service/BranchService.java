package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.sky.dto.ApiResponse;
import org.sky.dto.branch.BranchCreateRequest;
import org.sky.dto.branch.BranchUpdateRequest;
import org.sky.dto.branch.BranchResponse;
import org.sky.dto.branch.BranchListResponse;
import org.sky.model.Branch;
import org.sky.repository.BranchRepository;
import org.sky.repository.AdminRepository;
import org.sky.repository.SellerRepository;
import org.sky.exception.ValidationException;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class BranchService {
    
    @Inject
    BranchRepository branchRepository;
    
    @Inject
    AdminRepository adminRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    private static final Logger log = Logger.getLogger(BranchService.class);
    
    /**
     * Crea una nueva sucursal para un administrador
     */
    @WithTransaction
    public Uni<ApiResponse<BranchResponse>> createBranch(Long adminId, BranchCreateRequest request) {
        log.info("🏢 BranchService.createBranch() - Creando sucursal para admin: " + adminId);
        log.info("🏢 Nombre: " + request.name());
        log.info("🏢 Código: " + request.code());
        log.info("🏢 Dirección: " + request.address());
        
        // Verificar que el admin existe
        return adminRepository.findById(adminId)
                .chain(admin -> {
                    if (admin == null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("adminId", adminId.toString(), "Administrador no encontrado")
                        );
                    }
                    
                    // Verificar que el código de sucursal no existe
                    return branchRepository.find("code = ?1", request.code()).firstResult()
                            .chain(existingBranch -> {
                                if (existingBranch != null) {
                                    return Uni.createFrom().failure(
                                        ValidationException.invalidField("code", request.code(), "El código de sucursal ya existe")
                                    );
                                }
                                
                                // Crear nueva sucursal
                                Branch branch = new Branch();
                                branch.admin = admin;
                                branch.name = request.name();
                                branch.code = request.code();
                                branch.address = request.address();
                                branch.isActive = true;
                                
                                return branchRepository.persist(branch)
                                        .map(savedBranch -> {
                                            log.info("✅ Sucursal creada exitosamente con ID: " + savedBranch.id);
                                            
                                            BranchResponse response = new BranchResponse(
                                                savedBranch.id,
                                                savedBranch.name,
                                                savedBranch.code,
                                                savedBranch.address,
                                                savedBranch.isActive,
                                                0L, // sellersCount se calculará después
                                                savedBranch.createdAt,
                                                savedBranch.updatedAt
                                            );
                                            
                                            return ApiResponse.success("Sucursal creada exitosamente", response);
                                        });
                            });
                });
    }
    
    /**
     * Lista las sucursales de un administrador con paginación
     */
    @WithTransaction
    public Uni<ApiResponse<BranchListResponse>> listBranches(Long adminId, int page, int size, String status) {
        log.info("📋 BranchService.listBranches() - AdminId: " + adminId);
        log.info("📋 Página: " + page + ", Tamaño: " + size + ", Status: " + status);
        
        // Validar parámetros de paginación
        final int validatedPage = Math.max(0, page);
        final int validatedSize = (size <= 0 || size > 100) ? 20 : size;
        
        // Construir query basada en filtros
        final String query;
        final List<Object> params;
        
        if ("active".equals(status)) {
            query = "admin.id = ?1 and isActive = true";
            params = List.of(adminId);
        } else if ("inactive".equals(status)) {
            query = "admin.id = ?1 and isActive = false";
            params = List.of(adminId);
        } else {
            query = "admin.id = ?1";
            params = List.of(adminId);
        }
        
        return branchRepository.find(query, params.toArray())
                .page(validatedPage, validatedSize)
                .list()
                .chain(branches -> {
                    // Crear respuestas básicas primero
                    List<BranchResponse> branchResponses = branches.stream()
                            .map(branch -> new BranchResponse(
                                branch.id,
                                branch.name,
                                branch.code,
                                branch.address,
                                branch.isActive,
                                0L, // Se calculará después
                                branch.createdAt,
                                branch.updatedAt
                            ))
                            .collect(Collectors.toList());
                    
                    // Calcular información de paginación
                    return branchRepository.count(query, params.toArray())
                            .map(totalCount -> {
                                int totalPages = (int) Math.ceil((double) totalCount / validatedSize);
                                
                                BranchListResponse.PaginationInfo paginationInfo = 
                                    new BranchListResponse.PaginationInfo(
                                        validatedPage,
                                        totalPages,
                                        totalCount,
                                        validatedSize
                                    );
                                
                                BranchListResponse response = new BranchListResponse(branchResponses, paginationInfo);
                                return ApiResponse.success("Sucursales obtenidas exitosamente", response);
                            });
                });
    }
    
    /**
     * Obtiene los detalles de una sucursal específica
     */
    @WithTransaction
    public Uni<ApiResponse<BranchResponse>> getBranchById(Long adminId, Long branchId) {
        log.info("🔍 BranchService.getBranchById() - AdminId: " + adminId + ", BranchId: " + branchId);
        
        return branchRepository.find("id = ?1 and admin.id = ?2", branchId, adminId).firstResult()
                .chain(branch -> {
                    if (branch == null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("branchId", branchId.toString(), "Sucursal no encontrada")
                        );
                    }
                    
                    // Obtener conteo de vendedores
                    return sellerRepository.count("branch.id = ?1 and isActive = true", branchId)
                            .map(sellersCount -> {
                                BranchResponse response = new BranchResponse(
                                    branch.id,
                                    branch.name,
                                    branch.code,
                                    branch.address,
                                    branch.isActive,
                                    sellersCount,
                                    branch.createdAt,
                                    branch.updatedAt
                                );
                                
                                return ApiResponse.success("Detalles de sucursal obtenidos exitosamente", response);
                            });
                });
    }
    
    /**
     * Actualiza una sucursal existente
     */
    @WithTransaction
    public Uni<ApiResponse<BranchResponse>> updateBranch(Long adminId, Long branchId, BranchUpdateRequest request) {
        log.info("✏️ BranchService.updateBranch() - AdminId: " + adminId + ", BranchId: " + branchId);
        log.info("✏️ Nombre: " + request.name());
        log.info("✏️ Código: " + request.code());
        log.info("✏️ Dirección: " + request.address());
        log.info("✏️ Activo: " + request.isActive());
        
        return branchRepository.find("id = ?1 and admin.id = ?2", branchId, adminId).firstResult()
                .chain(branch -> {
                    if (branch == null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("branchId", branchId.toString(), "Sucursal no encontrada")
                        );
                    }
                    
                    // Si se está cambiando el código, verificar que no existe
                    if (request.code() != null && !request.code().equals(branch.code)) {
                        return branchRepository.find("code = ?1 and id != ?2", request.code(), branchId).firstResult()
                                .chain(existingBranch -> {
                                    if (existingBranch != null) {
                                        return Uni.createFrom().failure(
                                            ValidationException.invalidField("code", request.code(), "El código de sucursal ya existe")
                                        );
                                    }
                                    
                                    // Actualizar campos
                                    if (request.name() != null) branch.name = request.name();
                                    if (request.code() != null) branch.code = request.code();
                                    if (request.address() != null) branch.address = request.address();
                                    if (request.isActive() != null) branch.isActive = request.isActive();
                                    
                                    return branchRepository.persist(branch)
                                            .chain(updatedBranch -> {
                                                // Obtener conteo de vendedores
                                                return sellerRepository.count("branch.id = ?1 and isActive = true", branchId)
                                                        .map(sellersCount -> {
                                                            BranchResponse response = new BranchResponse(
                                                                updatedBranch.id,
                                                                updatedBranch.name,
                                                                updatedBranch.code,
                                                                updatedBranch.address,
                                                                updatedBranch.isActive,
                                                                sellersCount,
                                                                updatedBranch.createdAt,
                                                                updatedBranch.updatedAt
                                                            );
                                                            
                                                            return ApiResponse.success("Sucursal actualizada exitosamente", response);
                                                        });
                                            });
                                });
                    } else {
                        // Actualizar campos sin cambiar código
                        if (request.name() != null) branch.name = request.name();
                        if (request.address() != null) branch.address = request.address();
                        if (request.isActive() != null) branch.isActive = request.isActive();
                        
                        return branchRepository.persist(branch)
                                .chain(updatedBranch -> {
                                    // Obtener conteo de vendedores
                                    return sellerRepository.count("branch.id = ?1 and isActive = true", branchId)
                                            .map(sellersCount -> {
                                                BranchResponse response = new BranchResponse(
                                                    updatedBranch.id,
                                                    updatedBranch.name,
                                                    updatedBranch.code,
                                                    updatedBranch.address,
                                                    updatedBranch.isActive,
                                                    sellersCount,
                                                    updatedBranch.createdAt,
                                                    updatedBranch.updatedAt
                                                );
                                                
                                                return ApiResponse.success("Sucursal actualizada exitosamente", response);
                                            });
                                });
                    }
                });
    }
    
    /**
     * Elimina una sucursal (soft delete)
     */
    @WithTransaction
    public Uni<ApiResponse<String>> deleteBranch(Long adminId, Long branchId) {
        log.info("🗑️ BranchService.deleteBranch() - AdminId: " + adminId + ", BranchId: " + branchId);
        
        return branchRepository.find("id = ?1 and admin.id = ?2", branchId, adminId).firstResult()
                .chain(branch -> {
                    if (branch == null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("branchId", branchId.toString(), "Sucursal no encontrada")
                        );
                    }
                    
                    // Verificar si tiene vendedores activos
                    return sellerRepository.count("branch.id = ?1 and isActive = true", branchId)
                            .chain(sellersCount -> {
                                if (sellersCount > 0) {
                                    return Uni.createFrom().failure(
                                        ValidationException.invalidField("branchId", branchId.toString(), 
                                            "No se puede eliminar la sucursal porque tiene vendedores activos")
                                    );
                                }
                                
                                // Soft delete - marcar como inactivo
                                branch.isActive = false;
                                return branchRepository.persist(branch)
                                        .map(updatedBranch -> {
                                            log.info("✅ Sucursal eliminada exitosamente (soft delete)");
                                            return ApiResponse.success("Sucursal eliminada exitosamente", 
                                                "La sucursal ha sido marcada como inactiva");
                                        });
                            });
                });
    }
    
    /**
     * Obtiene los vendedores de una sucursal específica
     */
    @WithTransaction
    public Uni<ApiResponse<Map<String, Object>>> getBranchSellers(Long adminId, Long branchId, int page, int size) {
        log.info("👥 BranchService.getBranchSellers() - AdminId: " + adminId + ", BranchId: " + branchId);
        log.info("👥 Página: " + page + ", Tamaño: " + size);
        
        // Primero verificar que la sucursal existe y pertenece al admin
        return branchRepository.find("id = ?1 and admin.id = ?2", branchId, adminId).firstResult()
                .chain(branch -> {
                    if (branch == null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("branchId", branchId.toString(), "Sucursal no encontrada")
                        );
                    }
                    
                    // Validar parámetros de paginación
                    final int validatedPage = Math.max(0, page);
                    final int validatedSize = (size <= 0 || size > 100) ? 20 : size;
                    
                    // Obtener vendedores de la sucursal
                    return sellerRepository.find("branch.id = ?1", branchId)
                            .page(validatedPage, validatedSize)
                            .list()
                            .chain(sellers -> {
                                // Crear respuesta con información de la sucursal y lista de vendedores
                                BranchResponse branchInfo = new BranchResponse(
                                    branch.id,
                                    branch.name,
                                    branch.code,
                                    branch.address,
                                    branch.isActive,
                                    (long) sellers.size(),
                                    branch.createdAt,
                                    branch.updatedAt
                                );
                                
                                // Calcular información de paginación
                                return sellerRepository.count("branch.id = ?1", branchId)
                                        .map(totalCount -> {
                                            int totalPages = (int) Math.ceil((double) totalCount / validatedSize);
                                            
                                            BranchListResponse.PaginationInfo paginationInfo = 
                                                new BranchListResponse.PaginationInfo(
                                                    validatedPage,
                                                    totalPages,
                                                    totalCount,
                                                    validatedSize
                                                );
                                            
                                            // Crear respuesta con información de la sucursal y lista de vendedores
                                            Map<String, Object> responseData = Map.of(
                                                "branch", branchInfo,
                                                "sellers", sellers.stream()
                                                    .map(seller -> Map.of(
                                                        "sellerId", seller.id,
                                                        "sellerName", seller.sellerName,
                                                        "email", seller.email,
                                                        "phone", seller.phone,
                                                        "isActive", seller.isActive,
                                                        "createdAt", seller.createdAt
                                                    ))
                                                    .collect(Collectors.toList()),
                                                "pagination", paginationInfo
                                            );
                                            
                                            return ApiResponse.success("Vendedores de sucursal obtenidos exitosamente", responseData);
                                        });
                            });
                });
    }
    
}
