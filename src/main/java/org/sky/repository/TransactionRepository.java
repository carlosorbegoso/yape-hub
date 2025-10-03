package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.TransactionEntity;

@ApplicationScoped
public class TransactionRepository implements PanacheRepository<TransactionEntity> {
}