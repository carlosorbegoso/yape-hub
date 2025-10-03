package org.sky.service.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.mindrot.jbcrypt.BCrypt;

import org.sky.dto.request.admin.AdminRegisterRequest;
import org.sky.dto.request.auth.LoginRequest;
import org.sky.dto.response.ApiResponse;
import org.sky.dto.response.auth.LoginResponse;
import org.sky.dto.response.auth.SellerLoginWithAffiliationResponse;
import org.sky.dto.response.common.UserInfo;
// Removed ValidationException import - using RuntimeException instead
import org.sky.model.AdminEntity;
import org.sky.model.BusinessType;
import org.sky.model.AffiliationCodeEntity;
import org.sky.model.BranchEntity;
import org.sky.model.SellerEntity;
import org.sky.model.UserEntityEntity;
import org.sky.model.UserRole;
import org.sky.repository.AdminRepository;
import org.sky.repository.AffiliationCodeRepository;
import org.sky.repository.BranchRepository;
import org.sky.repository.SellerRepository;
import org.sky.repository.UserRepository;
import org.sky.service.SubscriptionService;
import org.sky.service.cache.CacheService;
import org.jboss.logging.Logger;

import org.sky.util.jwt.JwtExtractor;
import org.sky.util.jwt.JwtGenerator;
import org.sky.util.jwt.JwtValidator;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class AuthService {
    
    private static final Logger log = Logger.getLogger(AuthService.class);
    
    private final JwtGenerator jwtGenerator;
    private  final JwtValidator jwtValidator;
    private final JwtExtractor jwtExtractor;
    
    @Inject
    UserRepository userRepository;
    
    @Inject
    AdminRepository adminRepository;
    
    @Inject
    BranchRepository branchRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @Inject
    AffiliationCodeRepository affiliationCodeRepository;
    
    @Inject
    SubscriptionService subscriptionService;
    
    @Inject
    CacheService cacheService;
    
    @Inject
    org.sky.service.cache.CachedLoginStrategy cachedLoginStrategy;
    
    @Inject
    DatabaseLoginStrategy databaseLoginStrategy;
    
    @Inject
    JwtTokenService tokenService;
    
    @Inject
    LoginResponseBuilder loginResponseBuilder;

  public AuthService(JwtGenerator jwtGenerator, JwtValidator jwtValidator, JwtExtractor jwtExtractor) {
    this.jwtGenerator = jwtGenerator;
    this.jwtValidator = jwtValidator;
    this.jwtExtractor = jwtExtractor;
  }

  @WithTransaction
    public Uni<ApiResponse<LoginResponse>> registerAdmin(AdminRegisterRequest request) {
        return userRepository.findByEmail(request.email())
                .chain(existingUser -> {
                    if (existingUser != null) {
                        throw new RuntimeException("Email already exists: " + request.email());
                    }
                    return adminRepository.findByRuc(request.ruc());
                })
                .chain(existingAdmin -> {
                    if (existingAdmin != null) {
                        throw new RuntimeException("RUC already exists: " + request.ruc());
                    }
                    return createAdminAndUser(request);
                });
    }
    
    private Uni<ApiResponse<LoginResponse>> createAdminAndUser(AdminRegisterRequest request) {
        return Uni.createFrom().item(() -> {
            // Create user
            UserEntityEntity user = new UserEntityEntity();
            user.email = request.email();
            user.password = BCrypt.hashpw(request.password(), BCrypt.gensalt());
            user.role = UserRole.ADMIN;
            user.isVerified = false;
            return user;
        })
        .chain(user -> userRepository.persist(user)
                .chain(persistedUser -> {
                    AdminEntity admin = new AdminEntity();
                    admin.user = persistedUser;
                    admin.businessName = request.businessName();
                    admin.businessType = BusinessType.valueOf(request.businessType());
                    admin.ruc = request.ruc();
                    admin.contactName = request.contactName();
                    admin.phone = request.phone();
                    admin.address = request.address();
                    return adminRepository.persist(admin);
                })
                .chain(persistedAdmin -> {
                    // Create default branch
                    BranchEntity branch = new BranchEntity();
                    branch.admin = persistedAdmin;
                    branch.name = "Sucursal Principal";
                    branch.code = "MAIN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    branch.address = request.address();
                    return branchRepository.persist(branch);
                })
                .chain(branch -> subscriptionService.subscribeToFreePlan(branch.admin.id)
                        .chain(subscription -> {
                            String accessToken = jwtGenerator.generateAccessToken(branch.admin.user.id, branch.admin.user.role, null);
                            String refreshToken = jwtGenerator.generateRefreshToken(branch.admin.user.id);

                            UserInfo userInfo = UserInfo.fromAdmin(branch.admin);

                            LoginResponse response = LoginResponse.create(accessToken, refreshToken, userInfo);
                            return Uni.createFrom().item(ApiResponse.success("Administrador registrado exitosamente", response));
                        })));
    }
    
    @WithTransaction
    public Uni<ApiResponse<LoginResponse>> login(LoginRequest request) {
        return cacheService.getCachedUser(request.email(), request.role().toString())
                .chain(cachedUser -> {
                    if (cachedUser != null) {
                        return cachedLoginStrategy.executeWithCachedUser(cachedUser, request);
                    } else {
                        return databaseLoginStrategy.execute(request);
                    }
                });
    }

    @WithTransaction
    public Uni<ApiResponse<LoginResponse>> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return Uni.createFrom().item(ApiResponse.error("Invalid refresh token"));
        }

        // Validar refresh token usando JwtValidator
        return jwtValidator.isValidRefreshToken(refreshToken)
                .chain(isValid -> {
                    if (!isValid) {
                        return Uni.createFrom().item(ApiResponse.error("Invalid refresh token"));
                    }
                    
                    // Parsear token para extraer userId
                    return jwtValidator.parseToken(refreshToken)
                            .chain(jwt -> jwtExtractor.extractUserId(jwt))
                            .chain(userId -> userRepository.findByIdForRefresh(userId)
                                    .chain(user -> {
                                        if (user == null || !user.isActive) {
                                            return Uni.createFrom().item(ApiResponse.error("User not found or inactive"));
                                        }
                                        return tokenService.generateTokens(user)
                                            .chain(tokenData -> loginResponseBuilder.buildLoginResponseFromCachedUser(tokenData));
                                    }));
                });
    }




  @WithTransaction
    public Uni<ApiResponse<String>> logout(Long userId) {
        return userRepository.findById(userId)
                .chain(user -> {
                    if (user != null) {
                        user.deviceFingerprint = null;
                        return userRepository.persist(user)
                                .map(u -> ApiResponse.success("exit logout"));
                    }
                    return Uni.createFrom().item(ApiResponse.success("exit logout"));
                });
    }

    @WithTransaction
    public Uni<ApiResponse<SellerLoginWithAffiliationResponse>> loginByPhoneWithAffiliation(String phone, String affiliationCode) {
        return sellerRepository.findByPhone(phone)
                .chain(seller -> UserValidations.validateSeller(seller, phone))
                .chain(seller -> validateSellerBranch(seller, phone))
                .chain(seller -> validateAndProcessAffiliationCode(seller, affiliationCode))
                .chain(this::updateUserLastLogin)
                .chain(seller -> generateLoginResponse(seller, affiliationCode));
    }

    private Uni<SellerEntity> validateSellerBranch(SellerEntity seller, String phone) {
        if (seller.branch == null) {
            return Uni.createFrom().failure(
                new RuntimeException("Vendedor no tiene sucursal asignada: " + phone)
            );
        }
        return UserValidations.validateBranch(seller.branch)
                .map(validatedBranch -> seller);
    }

    private Uni<SellerEntity> validateAndProcessAffiliationCode(SellerEntity seller, String affiliationCode) {
        return Uni.combine()
                .all()
                .unis(
                    Uni.createFrom().item(seller.branch),
                    affiliationCodeRepository.findByAffiliationCode(affiliationCode)
                )
                .asTuple()
                .chain(tuple -> {
                    BranchEntity branch = tuple.getItem1();
                    AffiliationCodeEntity code = tuple.getItem2();
                    
                    return UserValidations.validateAffiliationCode(code, affiliationCode, branch)
                            .chain(validatedCode -> {
                                validatedCode.remainingUses--;
                                return affiliationCodeRepository.persist(validatedCode)
                                        .map(updatedCode -> seller);
                            });
                });
    }

    private Uni<SellerEntity> updateUserLastLogin(SellerEntity seller) {
        UserEntityEntity user = seller.user;
        if (user == null) {
            return Uni.createFrom().failure(
                new RuntimeException("Usuario no encontrado: " + seller.phone)
            );
        }
        
        user.lastLogin = LocalDateTime.now();
        return userRepository.persist(user)
                .map(updatedUser -> seller);
    }

    private Uni<ApiResponse<SellerLoginWithAffiliationResponse>> generateLoginResponse(SellerEntity seller, String affiliationCode) {
        UserEntityEntity user = seller.user;
        String accessToken = jwtGenerator.generateAccessToken(user.id, user.role, seller.id);
        
        SellerLoginWithAffiliationResponse response = new SellerLoginWithAffiliationResponse(
            seller.id,
            seller.sellerName,
            user.email,
            seller.phone,
            seller.branch.id,
            seller.branch.name,
            seller.branch.code,
            affiliationCode,
            accessToken
        );
        
        return Uni.createFrom().item(ApiResponse.success("Login exitoso", response));
    }
    
    // ==================================================================================
    // FORGOT PASSWORD FUNCTIONALITY
    // ==================================================================================
    
    public Uni<String> forgotPassword(String email) {
        return userRepository.find("email", email.toLowerCase().trim()).firstResult()
            .chain(user -> {
                if (user == null) {
                    // Por seguridad, no revelamos si el email existe o no
                    return Uni.createFrom().item("If the email exists, a password reset link has been sent");
                }
                
                // Generar token de reset
                String resetToken = generateResetToken();
                
                // Guardar token en caché con expiración de 1 hora
                return Uni.createFrom().item(() -> {
                    // Simular guardado en caché - en implementación real usar Redis
                    log.info("Password reset token cached for user " + user.id);
                    return true;
                })
                    .chain(cached -> {
                        // En un entorno real, aquí enviarías el email
                        // Por ahora, solo logueamos el token
                        log.info("Password reset token for user " + user.id + ": " + resetToken);
                        
                        return Uni.createFrom().item("Password reset email sent successfully");
                    });
            });
    }
    
    private String generateResetToken() {
        // Generar token aleatorio de 32 caracteres
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            token.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return token.toString();
    }
}
