package org.sky.service.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.mindrot.jbcrypt.BCrypt;
import org.sky.dto.ApiResponse;
import org.sky.dto.auth.AdminRegisterRequest;
import org.sky.dto.auth.LoginRequest;
import org.sky.dto.auth.LoginResponse;
import org.sky.dto.auth.SellerLoginWithAffiliationResponse;
import org.sky.exception.ValidationException;
import org.sky.model.Admin;
import org.sky.model.AffiliationCode;
import org.sky.model.Branch;
import org.sky.model.Seller;
import org.sky.model.User;
import org.sky.repository.AdminRepository;
import org.sky.repository.AffiliationCodeRepository;
import org.sky.repository.BranchRepository;
import org.sky.repository.SellerRepository;
import org.sky.repository.UserRepository;
import org.sky.service.SubscriptionService;
import org.sky.service.CacheService;

import org.sky.util.jwt.JwtExtractor;
import org.sky.util.jwt.JwtGenerator;
import org.sky.util.jwt.JwtValidator;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class AuthService {
    
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
    CachedLoginStrategy cachedLoginStrategy;
    
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
                .chain(branch -> subscriptionService.subscribeToFreePlan(branch.admin.id)
                        .chain(subscription -> {
                            String accessToken = jwtGenerator.generateAccessToken(branch.admin.user.id, branch.admin.user.role, null);
                            String refreshToken = jwtGenerator.generateRefreshToken(branch.admin.user.id);

                            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                                    branch.admin.user.id, branch.admin.user.email, branch.admin.user.role,
                                    branch.admin.id, branch.admin.businessName, branch.admin.user.isVerified, null
                            );

                            LoginResponse response = new LoginResponse(accessToken, refreshToken, 3600L, userInfo);
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

        return cacheService.getCachedValidToken(refreshToken)
                .chain(cachedUserId -> {
                    if (cachedUserId != null) {
                        return userRepository.findByIdForRefresh(Long.parseLong(cachedUserId))
                                .chain(user -> {
                                    if (user == null || !user.isActive) {
                                        return Uni.createFrom().item(ApiResponse.error("User not found or inactive"));
                                    }
                                    return tokenService.generateTokens(user)
                                            .chain(tokenData -> loginResponseBuilder.buildLoginResponseFromCachedUser(tokenData));
                                });
                    } else {
                        return cacheService.getCachedJwtValidation(refreshToken)
                                .chain(cachedValidation -> {
                                    if (cachedValidation != null && !cachedValidation) {
                                        return Uni.createFrom().item(ApiResponse.error("Invalid refresh token"));
                                    }
                                    
                                    if (cachedValidation == null) {
                                        return jwtValidator.isValidRefreshToken(refreshToken)
                                                .chain(isValid -> {
                                                    return cacheService.cacheJwtValidation(refreshToken, isValid)
                                                            .chain(v -> {
                                                                if (!isValid) {
                                                                    return Uni.createFrom().item(ApiResponse.error("Invalid refresh token"));
                                                                }
                                                                return validateAndExtractUser(refreshToken);
                                                            });
                                                });
                                    } else {
                                        return validateAndExtractUser(refreshToken);
                                    }
                                });
                    }
                });
    }

    private Uni<ApiResponse<LoginResponse>> validateAndExtractUser(String refreshToken) {
        return jwtValidator.parseToken(refreshToken)
            .onItem().ifNull().failWith(() -> ValidationException.invalidField("refreshToken", refreshToken, "Invalid or expired refresh token"))
            .chain(jwtExtractor::extractUserId)
            .onItem().ifNull().failWith(() -> ValidationException.invalidField("userId", "null", "User ID not found in token"))
            .chain(this::validateUserAndBuildResponse);
    }

    private Uni<ApiResponse<LoginResponse>> validateUserAndBuildResponse(Long userId) {
        return userRepository.findByIdForRefresh(userId)
            .onItem().ifNull().failWith(() -> ValidationException.invalidField("user", userId.toString(), "User not found"))
            .chain(user -> {
                if (!user.isActive) {
                    throw ValidationException.invalidField("user", userId.toString(), "User account is inactive");
                }
                return tokenService.generateTokens(user)
                    .chain(tokenData -> loginResponseBuilder.buildLoginResponseFromCachedUser(tokenData));
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

    private Uni<Seller> validateSellerBranch(Seller seller, String phone) {
        if (seller.branch == null) {
            return Uni.createFrom().failure(
                ValidationException.invalidField("branch", phone, "Vendedor no tiene sucursal asignada")
            );
        }
        return UserValidations.validateBranch(seller.branch)
                .map(validatedBranch -> seller);
    }

    private Uni<Seller> validateAndProcessAffiliationCode(Seller seller, String affiliationCode) {
        return Uni.combine()
                .all()
                .unis(
                    Uni.createFrom().item(seller.branch),
                    affiliationCodeRepository.findByAffiliationCode(affiliationCode)
                )
                .asTuple()
                .chain(tuple -> {
                    Branch branch = tuple.getItem1();
                    AffiliationCode code = tuple.getItem2();
                    
                    return UserValidations.validateAffiliationCode(code, affiliationCode, branch)
                            .chain(validatedCode -> {
                                validatedCode.remainingUses--;
                                return affiliationCodeRepository.persist(validatedCode)
                                        .map(updatedCode -> seller);
                            });
                });
    }

    private Uni<Seller> updateUserLastLogin(Seller seller) {
        User user = seller.user;
        if (user == null) {
            return Uni.createFrom().failure(
                ValidationException.invalidField("user", seller.phone, "Usuario no encontrado")
            );
        }
        
        user.lastLogin = LocalDateTime.now();
        return userRepository.persist(user)
                .map(updatedUser -> seller);
    }

    private Uni<ApiResponse<SellerLoginWithAffiliationResponse>> generateLoginResponse(Seller seller, String affiliationCode) {
        User user = seller.user;
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
}
