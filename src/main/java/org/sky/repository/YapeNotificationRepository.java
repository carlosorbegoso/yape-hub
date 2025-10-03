package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.YapeNotificationEntity;

@ApplicationScoped
public class YapeNotificationRepository implements PanacheRepository<YapeNotificationEntity> {
}
