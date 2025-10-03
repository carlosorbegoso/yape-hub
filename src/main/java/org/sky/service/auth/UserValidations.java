package org.sky.service.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.mindrot.jbcrypt.BCrypt;
import org.sky.dto.auth.LoginRequest;
import org.sky.exception.ValidationException;
import org.sky.model.AffiliationCodeEntity;
import org.sky.model.BranchEntity;
import org.sky.model.SellerEntity;
import org.sky.model.UserEntityEntity;

import java.time.LocalDateTime;

@ApplicationScoped
public  class UserValidations {

  public static Uni<SellerEntity> validateSeller(SellerEntity seller, String phone) {
    if (seller == null) {
      return Uni.createFrom().failure(
        ValidationException.invalidField("phone", phone, "Vendedor no encontrado con este número de teléfono")
      );
    }
    if (Boolean.FALSE.equals(seller.isActive)) {
      return Uni.createFrom().failure(
        ValidationException.invalidField("seller", phone, "Vendedor inactivo - contacte al administrador")
      );
    }
    return Uni.createFrom().item(seller);
  }

  public static Uni<BranchEntity> validateBranch(BranchEntity branch) {
    if (branch == null || !branch.isActive) {
      return Uni.createFrom().failure(
        ValidationException.invalidField("branch", "null", "La sucursal está inactiva o no existe")
      );
    }
    return Uni.createFrom().item(branch);
  }

  public static Uni<AffiliationCodeEntity> validateAffiliationCode(AffiliationCodeEntity code, String affiliationCode, BranchEntity sellerBranch) {
    if (code == null || !code.isActive) {
      return Uni.createFrom().failure(
        ValidationException.invalidField("affiliationCode", affiliationCode, "Código inválido")
      );
    }
    if (code.expiresAt != null && code.expiresAt.isBefore(LocalDateTime.now())) {
      return Uni.createFrom().failure(
        ValidationException.invalidField("affiliationCode", affiliationCode, "Código expirado")
      );
    }
    if (code.remainingUses <= 0) {
      return Uni.createFrom().failure(
        ValidationException.invalidField("affiliationCode", affiliationCode, "Código agotado")
      );
    }
    if (!sellerBranch.id.equals(code.branch.id)) {
      return Uni.createFrom().failure(
        ValidationException.invalidField("affiliationCode", affiliationCode, "Sucursal no coincide")
      );
    }
    return Uni.createFrom().item(code);
  }
  public Uni<UserEntityEntity> validateUserCredentials(UserEntityEntity user, LoginRequest request) {
    if (user == null) {
      return Uni.createFrom().failure(
          ValidationException.invalidField("credentials", request.email(), "Invalid email or password")
      );
    }

    if (Boolean.FALSE.equals(user.isActive)) {
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


}
