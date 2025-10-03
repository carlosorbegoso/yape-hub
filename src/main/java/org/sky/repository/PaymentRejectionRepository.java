package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentRejectionEntity;

@ApplicationScoped
public class PaymentRejectionRepository implements PanacheRepository<PaymentRejectionEntity> {
    
    // Métodos específicos para PaymentRejection si son necesarios
}
