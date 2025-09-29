package org.sky.service.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.exception.ValidationException;
import org.sky.model.AffiliationCode;
import org.sky.model.Branch;
import org.sky.model.Seller;
import org.sky.model.User;

import java.time.LocalDateTime;
import java.util.function.Function;

@ApplicationScoped
public  class UserValidations {

  public static Function<User, Uni<User>> validateActiveUser() {
    return user -> {
      if (user == null) {
        return Uni.createFrom().failure(
          ValidationException.invalidField("user", "null", "User not found")
        );
      }
      if (Boolean.FALSE.equals(user.isActive)) {
        return Uni.createFrom().failure(
          ValidationException.invalidField("user", user.email, "User account is inactive")
        );
      }
      return Uni.createFrom().item(user);
    };
  }

  public static Function<User, Uni<User>> validatePassword(String rawPassword) {
    return user -> {
      if (!org.mindrot.jbcrypt.BCrypt.checkpw(rawPassword, user.password)) {
        return Uni.createFrom().failure(
          ValidationException.invalidField("credentials", user.email, "Invalid email or password")
        );
      }
      return Uni.createFrom().item(user);
    };
  }

  public static Uni<Seller> validateSeller(Seller seller, String phone) {
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

  public static Uni<Branch> validateBranch(Branch branch) {
    if (branch == null || !branch.isActive) {
      return Uni.createFrom().failure(
        ValidationException.invalidField("branch", "null", "La sucursal está inactiva o no existe")
      );
    }
    return Uni.createFrom().item(branch);
  }

  public static Uni<AffiliationCode> validateAffiliationCode(AffiliationCode code, String affiliationCode, Branch sellerBranch) {
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

}
