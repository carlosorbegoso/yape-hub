package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.SellerEntity;

import java.time.LocalDateTime;
import java.util.List;

import static io.smallrye.config._private.ConfigLogging.log;

@ApplicationScoped
public class SellerRepository implements PanacheRepository<SellerEntity> {

    public Uni<SellerEntity> findByPhone(String phone) {
        return find("SELECT s FROM SellerEntity s JOIN FETCH s.user JOIN FETCH s.branch b JOIN FETCH b.admin WHERE s.phone = ?1", phone).firstResult();
    }

    public Uni<List<SellerEntity>> findByBranchId(Long branchId) {
        return find("branch.id = ?1 ORDER BY id", branchId).range(0, 50).list(); // Limited to 50 for low-resource efficiency
    }

    public Uni<List<SellerEntity>> findByAdminId(Long adminId) {
        return find("SELECT s FROM SellerEntity s JOIN FETCH s.branch b WHERE b.admin.id = ?1 ORDER BY s.id", adminId).range(0, 50).list(); // Limited to 50 for low-resource efficiency
    }

  public Uni<SellerPaginationResult> findSellersByAdminWithPagination(
      Long adminId,
      LocalDateTime startDate,
      LocalDateTime endDate,
      int page,
      int size
  ) {
    return count(
        "branch.admin.id = ?1 AND affiliationDate >= ?2 AND affiliationDate <= ?3",
        adminId, startDate, endDate
    )
        .chain(totalCount -> {
          // If no sellers found, return empty result
          if (totalCount == 0) {
            return Uni.createFrom().item(
                new SellerPaginationResult(List.of(), 0L)
            );
          }

          // Calculate total pages, ensuring at least 1
          int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / size));

          // Validate and adjust page number
          int validPage = Math.min(Math.max(page, 1), totalPages);

          return find(
              "SELECT s FROM SellerEntity s " +
                  "JOIN FETCH s.branch b " +
                  "WHERE b.admin.id = ?1 " +
                  "AND s.affiliationDate >= ?2 " +
                  "AND s.affiliationDate <= ?3 " +
                  "ORDER BY s.affiliationDate DESC",
              adminId, startDate, endDate)
              .page(validPage - 1, size)
              .list()
              .map(sellers -> {
                log.info("Pagination Debug - Total Count: " + totalCount +
                    ", Total Pages: " + totalPages +
                    ", Requested Page: " + page +
                    ", Valid Page: " + validPage);
                return new SellerPaginationResult(sellers, totalCount);
              });
        });
  }

    public Uni<SellerEntity> findByUserId(Long userId) {
        return find("SELECT s FROM SellerEntity s JOIN FETCH s.branch b JOIN FETCH b.admin WHERE s.user.id = ?1", userId).firstResult();
    }

    public Uni<SellerEntity> findBySellerIdAndAdminId(Long sellerId, Long adminId) {
        return find("id = ?1 and branch.admin.id = ?2", sellerId, adminId).firstResult();
    }
    
    public Uni<Long> countActiveByBranchId(Long branchId) {
        return count("branch.id = ?1 and isActive = true", branchId);
    }
    
    public Uni<SellerPaginationResult> findByBranchIdWithPagination(Long branchId, int page, int size) {
        return find("branch.id = ?1", branchId)
                .page(page, size)
                .list()
                .chain(sellers -> 
                    count("branch.id = ?1", branchId)
                            .map(totalCount -> new SellerPaginationResult(sellers, totalCount))
                );
    }
    
    public record SellerPaginationResult(List<SellerEntity> sellers, Long totalCount) {}
}
