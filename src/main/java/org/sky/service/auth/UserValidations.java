package org.sky.service.auth;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.exception.ValidationException;
import org.sky.model.AffiliationCode;
import org.sky.model.Branch;
import org.sky.model.Seller;
import org.sky.model.User;

import java.time.LocalDateTime;

@ApplicationScoped
public class UserValidations {

  public static void validateActiveUser(User user) {
    if (user == null) {
      throw ValidationException.invalidField("user", "null", "User not found");
    }
    if (Boolean.FALSE.equals(user.isActive)) {
      throw ValidationException.invalidField("user", user.email, "User account is inactive");
    }
  }

  public static void validatePassword(User user, String password) {
    if (!org.mindrot.jbcrypt.BCrypt.checkpw(password, user.password)) {
      throw ValidationException.invalidField("credentials", user.email, "Invalid email or password");
    }
  }
  public static void validateSeller(Seller seller, String phone) {
    if (seller == null) {
      throw ValidationException.invalidField("phone", phone, "Vendedor no encontrado con este número de teléfono");
    }
    if (Boolean.FALSE.equals(seller.isActive)) {
      throw ValidationException.invalidField("seller", phone, "Vendedor inactivo - contacte al administrador");
    }
  }

  public static void validateBranch(Branch branch) {
    if (branch == null || !branch.isActive) {
      throw ValidationException.invalidField("branch", "null", "La sucursal está inactiva o no existe");
    }
  }

  public static void validateAffiliationCode(AffiliationCode code, String affiliationCode, Branch sellerBranch) {
    if (code == null || !code.isActive) {
      throw ValidationException.invalidField("affiliationCode", affiliationCode, "Código inválido");
    }
    if (code.expiresAt != null && code.expiresAt.isBefore(LocalDateTime.now())) {
      throw ValidationException.invalidField("affiliationCode", affiliationCode, "Código expirado");
    }
    if (code.remainingUses <= 0) {
      throw ValidationException.invalidField("affiliationCode", affiliationCode, "Código agotado");
    }
    if (!sellerBranch.id.equals(code.branch.id)) {
      throw ValidationException.invalidField("affiliationCode", affiliationCode, "Sucursal no coincide");
    }
  }
}
