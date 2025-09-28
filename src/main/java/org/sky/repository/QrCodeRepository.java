package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.QrCode;

@ApplicationScoped
public class QrCodeRepository implements PanacheRepository<QrCode> {


}
