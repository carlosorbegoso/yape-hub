package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentNotification;

@ApplicationScoped
public class PaymentNotificationRepository implements PanacheRepository<PaymentNotification> {
    
    // Métodos específicos para notificaciones de pago
    // Los métodos básicos de PanacheRepository ya están disponibles
}
