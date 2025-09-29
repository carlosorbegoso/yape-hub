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
                            // Generate tokens
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
        // Intentar obtener del cache primero
        return cacheService.getCachedUser(request.email(), request.role().toString())
                .chain(cachedUser -> {
                    if (cachedUser != null) {
                        // Usuario encontrado en cache, validar credenciales
                        return validateUserCredentials(cachedUser, request)
                                .chain(user -> generateTokens(user))
                                .chain(tokenData -> buildLoginResponseFromCachedUser(tokenData));
                    } else {
                        // Usuario no en cache, consultar base de datos
                        return getUserForLogin(request)
                                .chain(user -> validateUserCredentials(user, request))
                                .chain(user -> updateUserLoginInfo(user, request))
                                .chain(user -> {
                                    // Cachear usuario de forma reactiva
                                    return cacheService.cacheUser(request.email(), request.role().toString(), user)
                                            .chain(v -> generateTokens(user));
                                })
                                .chain(tokenData -> buildLoginResponse(tokenData));
                    }
                });
    }

    private Uni<User> validateUserCredentials(User user, LoginRequest request) {
        if (user == null) {
            return Uni.createFrom().failure(
                ValidationException.invalidField("credentials", request.email(), "Invalid email or password")
            );
        }
        
        if (!user.isActive) {
            return Uni.createFrom().failure(
                ValidationException.invalidField("user", request.email(), "User account is inactive")
            );
        }
        
        if (!BCrypt.checkpw(request.password(), user.password)) {
            return Uni.createFrom().failure(
                ValidationException.invalidField("credentials", request.email(), "Invalid email or password")
            );
        }
        
        return Uni.createFrom().item(user);
    }

    private Uni<User> updateUserLoginInfo(User user, LoginRequest request) {
        user.lastLogin = LocalDateTime.now();
        user.deviceFingerprint = request.deviceFingerprint();
        return userRepository.persist(user);
    }

    private Uni<TokenData> generateTokens(User user) {
        String accessToken = jwtGenerator.generateAccessToken(user.id, user.role, null);
        String refreshToken = jwtGenerator.generateRefreshToken(user.id);
        return Uni.createFrom().item(new TokenData(user, accessToken, refreshToken));
    }

    private Uni<User> getUserForLogin(LoginRequest request) {
        return userRepository.findByEmailAndRoleForLogin(request.email(), request.role());
    }

    private Uni<ApiResponse<LoginResponse>> buildLoginResponseFromCachedUser(TokenData tokenData) {
        // Para usuarios cacheados, construir respuesta directamente sin consultas adicionales
        User user = tokenData.user;
        
        
        // Para usuarios cacheados, usar datos ya cargados
        if (user.role == User.UserRole.ADMIN) {
            // Para ADMIN, buscar admin por userId
            return adminRepository.findByUserId(user.id)
                    .map(admin -> {
                        Long adminBusinessId = admin != null ? admin.id : null;
                        String adminBusinessName = admin != null ? admin.businessName : null;
                        
                        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                            user.id, user.email, user.role,
                            adminBusinessId, adminBusinessName, user.isVerified, null
                        );
                        
                        LoginResponse response = new LoginResponse(
                            tokenData.accessToken, tokenData.refreshToken, 3600L, userInfo
                        );
                        
                        return ApiResponse.success("Login exitoso", response);
                    });
        } else {
            // Para SELLER, buscar seller por userId
            return sellerRepository.findByUserId(user.id)
                    .map(seller -> {
                        Long sellerBusinessId = null;
                        String sellerBusinessName = null;
                        Long sellerIdValue = seller != null ? seller.id : null;
                        
                        if (seller != null && seller.branch != null && seller.branch.admin != null) {
                            sellerBusinessId = seller.branch.admin.id;
                            sellerBusinessName = seller.branch.admin.businessName;
                        }
                        
                        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                            user.id, user.email, user.role,
                            sellerBusinessId, sellerBusinessName, user.isVerified, sellerIdValue
                        );
                        
                        LoginResponse response = new LoginResponse(
                            tokenData.accessToken, tokenData.refreshToken, 3600L, userInfo
                        );
                        
                        return ApiResponse.success("Login exitoso", response);
                    });
        }
    }

    private Uni<ApiResponse<LoginResponse>> buildLoginResponse(TokenData tokenData) {
        User user = tokenData.user;
        
        if (user.role == User.UserRole.ADMIN) {
            return buildAdminLoginResponse(tokenData);
        } else {
            return buildSellerLoginResponse(tokenData);
        }
    }

    private Uni<ApiResponse<LoginResponse>> buildAdminLoginResponse(TokenData tokenData) {
        return adminRepository.findByUserId(tokenData.user.id)
                .map(admin -> {
                    Long businessId = admin != null ? admin.id : null;
                    String businessName = admin != null ? admin.businessName : null;
                    
                    LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                        tokenData.user.id, tokenData.user.email, tokenData.user.role,
                        businessId, businessName, tokenData.user.isVerified, null
                    );
                    
                    LoginResponse response = new LoginResponse(
                        tokenData.accessToken, tokenData.refreshToken, 3600L, userInfo
                    );
                    
                    return ApiResponse.success("Login exitoso", response);
                });
    }

    private Uni<ApiResponse<LoginResponse>> buildSellerLoginResponse(TokenData tokenData) {
        return sellerRepository.findByUserId(tokenData.user.id)
                .map(seller -> {
                    Long businessId = null;
                    String businessName = null;
                    Long sellerId = seller != null ? seller.id : null;
                    
                    if (seller != null && seller.branch != null && seller.branch.admin != null) {
                        businessId = seller.branch.admin.id;
                        businessName = seller.branch.admin.businessName;
                    }
                    
                    LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                        tokenData.user.id, tokenData.user.email, tokenData.user.role,
                        businessId, businessName, tokenData.user.isVerified, sellerId
                    );
                    
                    LoginResponse response = new LoginResponse(
                        tokenData.accessToken, tokenData.refreshToken, 3600L, userInfo
                    );
                    
                    return ApiResponse.success("Login exitoso", response);
                });
    }

  private record TokenData(User user, String accessToken, String refreshToken) {
  }
    
    @WithTransaction
    public Uni<ApiResponse<LoginResponse>> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return Uni.createFrom().item(ApiResponse.error("Invalid refresh token"));
        }

        // Intentar obtener del cache primero
        return cacheService.getCachedValidToken(refreshToken)
                .chain(cachedUserId -> {
                    if (cachedUserId != null) {
                        // Token válido en cache, obtener usuario
                        return userRepository.findByIdForRefresh(Long.parseLong(cachedUserId))
                                .chain(user -> {
                                    if (user == null || !user.isActive) {
                                        return Uni.createFrom().item(ApiResponse.error("User not found or inactive"));
                                    }
                                    return generateTokens(user)
                                            .chain(tokenData -> buildLoginResponseFromCachedUser(tokenData));
                                });
                    } else {
                        // Token no en cache, validar y procesar
                        return cacheService.getCachedJwtValidation(refreshToken)
                                .chain(cachedValidation -> {
                                    if (cachedValidation != null && !cachedValidation) {
                                        return Uni.createFrom().item(ApiResponse.error("Invalid refresh token"));
                                    }
                                    
                                    if (cachedValidation == null) {
                                        // No está en cache, validar JWT
                                        return jwtValidator.isValidRefreshToken(refreshToken)
                                                .chain(isValid -> {
                                                    // Cachear resultado de validación de forma reactiva
                                                    return cacheService.cacheJwtValidation(refreshToken, isValid)
                                                            .chain(v -> {
                                                                if (!isValid) {
                                                                    return Uni.createFrom().item(ApiResponse.error("Invalid refresh token"));
                                                                }
                                                                return validateAndExtractUser(refreshToken);
                                                            });
                                                });
                                    } else {
                                        // Token válido en cache, procesar
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
                return generateTokens(user)
                    .chain(tokenData -> buildLoginResponseFromCachedUser(tokenData));
            });
    }

  private Uni<LoginResponse> buildLoginResponse(User user) {
    String accessToken = jwtGenerator.generateAccessToken(
        user.id,
        user.role,
        null
    );
    String refreshToken = jwtGenerator.generateRefreshToken(user.id);
    Long expiresIn = jwtGenerator.getAccessTokenExpirySeconds();

    return switch (user.role) {
      case ADMIN -> adminRepository.findByUserId(user.id)
          .map(admin -> new LoginResponse(
              accessToken,
              refreshToken,
              expiresIn,
              new LoginResponse.UserInfo(
                  user.id,
                  user.email,
                  user.role,
                  admin != null ? admin.id : null,
                  admin != null ? admin.businessName : null,
                  user.isVerified,
                  null
              )
          ));

      case SELLER -> sellerRepository.findByUserId(user.id)
          .chain(seller -> {
            if (seller == null) {
              return Uni.createFrom().item(new LoginResponse(
                  accessToken,
                  refreshToken,
                  expiresIn,
                  new LoginResponse.UserInfo(
                      user.id,
                      user.email,
                      user.role,
                      null,
                      null,
                      user.isVerified,
                      null
                  )
              ));
            }

            return Uni.createFrom().item(seller.branch)
                .chain(branch -> {
                  if (branch == null || branch.admin == null) {
                    return Uni.createFrom().item(new LoginResponse(
                        accessToken,
                        refreshToken,
                        expiresIn,
                        new LoginResponse.UserInfo(
                            user.id,
                            user.email,
                            user.role,
                            null,
                            null,
                            user.isVerified,
                            seller.id
                        )
                    ));
                  }
                  
                  return Uni.createFrom().item(new LoginResponse(
                      accessToken,
                      refreshToken,
                      expiresIn,
                      new LoginResponse.UserInfo(
                          user.id,
                          user.email,
                          user.role,
                          branch.admin.id,
                          branch.admin.businessName,
                          user.isVerified,
                          seller.id
                      )
                  ));
                });
          });
    };
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
