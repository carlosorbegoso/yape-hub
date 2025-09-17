package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentRejection;

@ApplicationScoped
public class PaymentRejectionRepository implements PanacheRepository<PaymentRejection> {
    
    // Métodos específicos para PaymentRejection si son necesarios
}
