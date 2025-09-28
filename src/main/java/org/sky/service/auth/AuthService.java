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
import org.sky.model.Branch;
import org.sky.model.User;
import org.sky.repository.AdminRepository;
import org.sky.repository.AffiliationCodeRepository;
import org.sky.repository.BranchRepository;
import org.sky.repository.SellerRepository;
import org.sky.repository.UserRepository;
import org.sky.service.SubscriptionService;

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
                    // Asignar suscripción gratuita por defecto
                    return subscriptionService.subscribeToFreePlan(branch.admin.id)
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
                            });
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
                                String accessToken = jwtGenerator.generateAccessToken(updatedUser.id, updatedUser.role, null);
                                String refreshToken = jwtGenerator.generateRefreshToken(updatedUser.id);
                                
                                // Get business info for admin
                                if (updatedUser.role == User.UserRole.ADMIN) {
                                    return adminRepository.findByUserId(updatedUser.id)
                                            .chain(admin -> {
                                                String businessName = admin != null ? admin.businessName : null;
                                                Long businessId = admin != null ? admin.id : null;
                                                
                                                LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                                                        updatedUser.id, updatedUser.email, updatedUser.role, 
                                                        businessId, businessName, updatedUser.isVerified, null
                                                );
                                                
                                                LoginResponse response = new LoginResponse(accessToken, refreshToken, 3600L, userInfo);
                                                return Uni.createFrom().item(ApiResponse.success("Login exitoso", response));
                                            });
                                } else {
                                    // Para SELLERs, obtener información de la sucursal y admin
                                    return sellerRepository.findByUserId(updatedUser.id)
                                            .chain(seller -> {
                                                if (seller == null || seller.branch == null || seller.branch.admin == null) {
                                                    LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                                                            updatedUser.id, updatedUser.email, updatedUser.role, 
                                                            null, null, updatedUser.isVerified, seller != null ? seller.id : null
                                                    );
                                                    LoginResponse response = new LoginResponse(accessToken, refreshToken, 3600L, userInfo);
                                                    return Uni.createFrom().item(ApiResponse.success("Login exitoso", response));
                                                }
                                                
                                                LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                                                        updatedUser.id, updatedUser.email, updatedUser.role, 
                                                        seller.branch.admin.id, seller.branch.admin.businessName, 
                                                        updatedUser.isVerified, seller.id
                                                );
                                                
                                                LoginResponse response = new LoginResponse(accessToken, refreshToken, 3600L, userInfo);
                                                return Uni.createFrom().item(ApiResponse.success("Login exitoso", response));
                                            });
                                }
                            });
                });
    }
    
    @WithTransaction
    public Uni<ApiResponse<LoginResponse>> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return Uni.createFrom().item(ApiResponse.error("Invalid refresh token"));
        }

        return jwtValidator.isValidRefreshToken(refreshToken)
            .onItem().transformToUni(isValid -> {
                if (!isValid) {
                    return Uni.createFrom().item(ApiResponse.error("Invalid refresh token"));
                }
                return validateAndExtractUser(refreshToken);
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
        return userRepository.findById(userId)
            .onItem().ifNull().failWith(() -> ValidationException.invalidField("user", userId.toString(), "User not found"))
            .chain(user -> {
                if (!user.isActive) {
                    throw ValidationException.invalidField("user", userId.toString(), "User account is inactive");
                }
                return buildLoginResponse(user)
                    .map(loginResponse -> ApiResponse.success("Token refreshed successfully", loginResponse));
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
                  null // no sellerId para admin
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
                      null, // no businessId
                      null, // no businessName
                      user.isVerified,
                      null // no sellerId
                  )
              ));
            }
            
            // Obtener información de la sucursal y admin
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
                            null, // no businessId
                            null, // no businessName
                            user.isVerified,
                            seller.id // sellerId
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
                          branch.admin.id, // businessId del admin
                          branch.admin.businessName, // businessName del admin
                          user.isVerified,
                          seller.id // sellerId
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
                                .map(u -> ApiResponse.success("Logout exitoso"));
                    }
                    return Uni.createFrom().item(ApiResponse.success("Logout exitoso"));
                });
    }
    

    
    @WithTransaction
    public Uni<ApiResponse<SellerLoginWithAffiliationResponse>> loginByPhoneWithAffiliation(String phone, String affiliationCode) {
        
        return sellerRepository.findByPhone(phone)
                .chain(seller -> {
                    if (seller == null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("phone", phone, "Vendedor no encontrado con este número de teléfono")
                        );
                    }
                    
                    // Validar estado del vendedor
                    if (!seller.isActive) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("seller", phone, "Vendedor inactivo - contacte al administrador")
                        );
                    }
                    
                    // Validar que el vendedor tenga sucursal asignada
                    if (seller.branch == null) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("branch", phone, "Vendedor no tiene sucursal asignada")
                        );
                    }
                    
                    // Validar que la sucursal esté activa
                    if (!seller.branch.isActive) {
                        return Uni.createFrom().failure(
                            ValidationException.invalidField("branch", seller.branch.id.toString(), "La sucursal está inactiva")
                        );
                    }
                    
                    // Validar código de afiliación
                    return affiliationCodeRepository.findByAffiliationCode(affiliationCode)
                            .chain(code -> {
                                if (code == null || !code.isActive) {
                                    return Uni.createFrom().failure(
                                        ValidationException.invalidField("affiliationCode", affiliationCode, "Código de afiliación inválido")
                                    );
                                }

                                // Verificar que el código no haya expirado
                                if (code.expiresAt != null && code.expiresAt.isBefore(LocalDateTime.now())) {
                                    return Uni.createFrom().failure(
                                        ValidationException.invalidField("affiliationCode", affiliationCode, "Código de afiliación expirado")
                                    );
                                }

                                // Verificar que el código tenga usos restantes
                                if (code.remainingUses <= 0) {
                                    return Uni.createFrom().failure(
                                        ValidationException.invalidField("affiliationCode", affiliationCode, "Código de afiliación agotado")
                                    );
                                }

                                // Verificar que el vendedor esté afiliado a la sucursal del código
                                if (!seller.branch.id.equals(code.branch.id)) {
                                    return Uni.createFrom().failure(
                                        ValidationException.invalidField("affiliationCode", affiliationCode,
                                            "El vendedor no está afiliado a esta sucursal")
                                    );
                                }
                                
                                // Reducir usos del código
                                code.remainingUses--;
                                
                                return affiliationCodeRepository.persist(code)
                                        .chain(updatedCode -> {
                                            // Generar token JWT
                                            User user = seller.user;
                                            if (user == null) {
                                                return Uni.createFrom().failure(
                                                    ValidationException.invalidField("user", phone, "Usuario no encontrado")
                                                );
                                            }
                                            
                                            // Actualizar último login
                                            user.lastLogin = LocalDateTime.now();
                                            
                                            return userRepository.persist(user)
                                                    .chain(updatedUser -> {
                                                        // Generar token de acceso con sellerId
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
                                                    });
                                        });
                            });
                });
    }
}
