package org.sky.service.stats.calculators.strategy;

import org.sky.model.PaymentNotification;
import java.util.List;

/**
 * Record para encapsular la entrada de la estrategia de conteo por estado
 */
public record PaymentStatusInput(
    List<PaymentNotification> payments,
    String status
) {}
