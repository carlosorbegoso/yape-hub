package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.Notification;

@ApplicationScoped
public class NotificationRepository implements PanacheRepository<Notification> {


}