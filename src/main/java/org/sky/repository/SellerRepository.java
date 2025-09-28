package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.Seller;

import java.util.List;

@ApplicationScoped
public class SellerRepository implements PanacheRepository<Seller> {


  public Uni<Seller> findByPhone(String phone) {
    return find("phone", phone).firstResult();
  }

  public Uni<List<Seller>> findByBranchId(Long branchId) {
    return find("branch.id", branchId).list();
  }

  public Uni<List<Seller>> findByAdminId(Long adminId) {
    return find("branch.admin.id", adminId).list();
  }

  public Uni<Seller> findByUserId(Long userId) {
    return find("user.id", userId).firstResult();
  }

  public Uni<Seller> findBySellerIdAndAdminId(Long sellerId, Long adminId) {
    return find("id = ?1 and branch.admin.id = ?2", sellerId, adminId).firstResult();
  }

}