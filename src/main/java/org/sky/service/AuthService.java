package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.mindrot.jbcrypt.BCrypt;
import org.sky.dto.ApiResponse;
import org.sky.dto.auth.AdminRegisterRequest;
import org.sky.dto.auth.LoginRequest;
import org.sky.dto.auth.LoginResponse;
import org.sky.exception.ValidationException;
import org.sky.model.Admin;
import org.sky.model.Branch;
import org.sky.model.Seller;
import org.sky.model.User;
import org.sky.repository.AdminRepository;
import org.sky.repository.BranchRepository;
import org.sky.repository.SellerRepository;
import org.sky.repository.UserRepository;
import org.sky.util.JwtUtil;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class AuthService {
    
    private static final Logger log = Logger.getLogger(AuthService.class);
    
    @Inject
    JwtUtil jwtUtil;
    
    @Inject
    UserRepository userRepository;
    
    @Inject
    AdminRepository adminRepository;
    
    @Inject
    BranchRepository branchRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @WithTransaction
    public Uni<ApiResponse<LoginResponse>> registerAdmin(AdminRegisterRequest request) {
        return userRepository.findByEmail(request.email())
                .chain(existingUser -> {
                    if (existingUser != null) {
                        throw ValidationException.duplicateField("email", request.email());
                    }
                    return adminRepository.findByRuc(request.ruc());
                })
                .chain(existingAdmin -> {
                    if (existingAdmin != null) {
                        throw ValidationException.duplicateField("ruc", request.ruc());
                    }
                    return createAdminAndUser(request);
                });
    }
    
    private Uni<ApiResponse<LoginResponse>> createAdminAndUser(AdminRegisterRequest request) {
        return Uni.createFrom().item(() -> {
            // Create user
            User user = new User();
            user.email = request.email();
            user.password = BCrypt.hashpw(request.password(), BCrypt.gensalt());
            user.role = User.UserRole.ADMIN;
            user.isVerified = false;
            return user;
        })
        .chain(user -> userRepository.persist(user)
                .chain(persistedUser -> {
                    // Create admin
                    Admin admin = new Admin();
                    admin.user = persistedUser;
                    admin.businessName = request.businessName();
                    admin.businessType = Admin.BusinessType.valueOf(request.businessType());
                    admin.ruc = request.ruc();
                    admin.contactName = request.contactName();
                    admin.phone = request.phone();
                    admin.address = request.address();
                    return adminRepository.persist(admin);
                })
                .chain(persistedAdmin -> {
                    // Create default branch
                    Branch branch = new Branch();
                    branch.admin = persistedAdmin;
                    branch.name = "Sucursal Principal";
                    branch.code = "MAIN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    branch.address = request.address();
                    return branchRepository.persist(branch);
                })
                .chain(branch -> {
                    // Generate tokens
                    String accessToken = jwtUtil.generateAccessToken(branch.admin.user.id, branch.admin.user.role);
                    String refreshToken = jwtUtil.generateRefreshToken(branch.admin.user.id);
                    
                    LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                            branch.admin.user.id, branch.admin.user.email, branch.admin.user.role, 
                            branch.admin.id, branch.admin.businessName, branch.admin.user.isVerified
                    );
                    
                    LoginResponse response = new LoginResponse(accessToken, refreshToken, 3600L, userInfo);
                    return Uni.createFrom().item(ApiResponse.success("Administrador registrado exitosamente", response));
                }));
    }
    
    @WithTransaction
    public Uni<ApiResponse<LoginResponse>> login(LoginRequest request) {
        return userRepository.findByEmailAndRole(request.email(), request.role())
                .chain(user -> {
                    if (user == null) {
                        throw ValidationException.invalidField("credentials", request.email(), "Invalid email or password");
                    }
                    
                    // Check if user is active
                    if (!user.isActive) {
                        throw ValidationException.invalidField("user", request.email(), "User account is inactive");
                    }
                    
                    // Verify password
                    if (!BCrypt.checkpw(request.password(), user.password)) {
                        throw ValidationException.invalidField("credentials", request.email(), "Invalid email or password");
                    }
                    
                    // Update last login and device fingerprint
                    user.lastLogin = LocalDateTime.now();
                    user.deviceFingerprint = request.deviceFingerprint();
                    
                    return userRepository.persist(user)
                            .chain(updatedUser -> {
                                // Generate tokens
                                String accessToken = jwtUtil.generateAccessToken(updatedUser.id, updatedUser.role);
                                String refreshToken = jwtUtil.generateRefreshToken(updatedUser.id);
                                
                                // Get business info for admin
                                if (updatedUser.role == User.UserRole.ADMIN) {
                                    return adminRepository.findByUserId(updatedUser.id)
                                            .chain(admin -> {
                                                String businessName = admin != null ? admin.businessName : null;
                                                Long businessId = admin != null ? admin.id : null;
                                                
                                                LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                                                        updatedUser.id, updatedUser.email, updatedUser.role, 
                                                        businessId, businessName, updatedUser.isVerified
                                                );
                                                
                                                LoginResponse response = new LoginResponse(accessToken, refreshToken, 3600L, userInfo);
                                                return Uni.createFrom().item(ApiResponse.success("Login exitoso", response));
                                            });
                                } else {
                                    LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                                            updatedUser.id, updatedUser.email, updatedUser.role, 
                                            null, null, updatedUser.isVerified
                                    );
                                    
                                    LoginResponse response = new LoginResponse(accessToken, refreshToken, 3600L, userInfo);
                                    return Uni.createFrom().item(ApiResponse.success("Login exitoso", response));
                                }
                            });
                });
    }
    
    @WithTransaction
    public Uni<ApiResponse<LoginResponse>> refreshToken(String refreshToken) {
        Long userId = jwtUtil.validateRefreshToken(refreshToken);
        if (userId == null) {
            throw ValidationException.invalidField("refreshToken", refreshToken, "Invalid or expired refresh token");
        }
        
        return userRepository.findById(userId)
                .chain(user -> {
                    if (user == null || !user.isActive) {
                        throw ValidationException.invalidField("user", userId.toString(), "User not found or inactive");
                    }
                    
                    // Generate new tokens
                    String newAccessToken = jwtUtil.generateAccessToken(user.id, user.role);
                    String newRefreshToken = jwtUtil.generateRefreshToken(user.id);
                    
                    // Get business info for admin
                    if (user.role == User.UserRole.ADMIN) {
                        return adminRepository.findByUserId(user.id)
                                .chain(admin -> {
                                    String businessName = admin != null ? admin.businessName : null;
                                    Long businessId = admin != null ? admin.id : null;
                                    
                                    LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                                            user.id, user.email, user.role, businessId, businessName, user.isVerified
                                    );
                                    
                                    LoginResponse response = new LoginResponse(newAccessToken, newRefreshToken, 3600L, userInfo);
                                    return Uni.createFrom().item(ApiResponse.success("Token renovado exitosamente", response));
                                });
                    } else {
                        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                                user.id, user.email, user.role, null, null, user.isVerified
                        );
                        
                        LoginResponse response = new LoginResponse(newAccessToken, newRefreshToken, 3600L, userInfo);
                        return Uni.createFrom().item(ApiResponse.success("Token renovado exitosamente", response));
                    }
                });
    }
    
    @WithTransaction
    public Uni<ApiResponse<String>> logout(Long userId) {
        return userRepository.findById(userId)
                .chain(user -> {
                    if (user != null) {
                        user.deviceFingerprint = null;
                        return userRepository.persist(user)
                                .map(u -> ApiResponse.success("Logout exitoso"));
                    }
                    return Uni.createFrom().item(ApiResponse.success("Logout exitoso"));
                });
    }
    
    @WithTransaction
    public Uni<ApiResponse<LoginResponse>> loginByPhone(String phone) {
        log.info("🔐 AuthService.loginByPhone() - Intentando login con teléfono: " + phone);
        
        return sellerRepository.findByPhone(phone)
                .chain(seller -> {
                    if (seller == null) {
                        log.warn("❌ Vendedor no encontrado con teléfono: " + phone);
                        throw ValidationException.invalidField("phone", phone, "Vendedor no encontrado con este número de teléfono");
                    }
                    
                    log.info("✅ Vendedor encontrado: " + seller.sellerName + " (ID: " + seller.id + ")");
                    
                    // Validar estado del vendedor
                    if (!seller.isActive) {
                        log.warn("❌ Vendedor inactivo: " + seller.sellerName);
                        throw ValidationException.invalidField("seller", phone, "Vendedor inactivo - contacte al administrador");
                    }
                    
                    // Validar afiliación
                    if (seller.affiliationCode == null || seller.affiliationCode.trim().isEmpty()) {
                        log.warn("❌ Vendedor sin código de afiliación: " + seller.sellerName);
                        throw ValidationException.invalidField("affiliation", phone, "Vendedor no está afiliado correctamente");
                    }
                    
                    // Validar branch (debe estar afiliado a una sucursal)
                    if (seller.branch == null) {
                        log.warn("❌ Vendedor sin sucursal asignada: " + seller.sellerName);
                        throw ValidationException.invalidField("branch", phone, "Vendedor no tiene sucursal asignada");
                    }
                    
                    User user = seller.user;
                    if (user == null) {
                        log.warn("❌ Usuario asociado no encontrado para vendedor: " + seller.sellerName);
                        throw ValidationException.invalidField("user", phone, "Usuario asociado no encontrado");
                    }
                    
                    if (!user.isActive) {
                        log.warn("❌ Usuario inactivo: " + user.email);
                        throw ValidationException.invalidField("user", phone, "Usuario inactivo - contacte al administrador");
                    }
                    
                    if (!user.isVerified) {
                        log.warn("❌ Usuario no verificado: " + user.email);
                        throw ValidationException.invalidField("user", phone, "Usuario no verificado - contacte al administrador");
                    }
                    
                    log.info("✅ Todas las validaciones pasaron para: " + seller.sellerName);
                    
                    // Update last login
                    user.lastLogin = LocalDateTime.now();
                    
                    return userRepository.persist(user)
                            .chain(updatedUser -> {
                                // Generate tokens
                                String accessToken = jwtUtil.generateAccessToken(updatedUser.id, updatedUser.role);
                                String refreshToken = jwtUtil.generateRefreshToken(updatedUser.id);
                                
                                LoginResponse loginResponse = new LoginResponse(
                                        accessToken, refreshToken, 3600L,
                                        new LoginResponse.UserInfo(
                                                updatedUser.id, updatedUser.email, updatedUser.role,
                                                seller.branch.id, seller.branch.name, updatedUser.isVerified
                                        )
                                );
                                
                                log.info("✅ Login exitoso para vendedor: " + seller.sellerName + " (ID: " + seller.id + ")");
                                return Uni.createFrom().item(ApiResponse.success("Login exitoso", loginResponse));
                            });
                });
    }
}
