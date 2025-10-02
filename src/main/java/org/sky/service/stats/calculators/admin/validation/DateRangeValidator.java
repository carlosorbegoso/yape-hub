package org.sky.service.stats.calculators.admin.validation;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
@ApplicationScoped
public class DateRangeValidator {
    
    private static final int MAX_DAYS_ALLOWED = 30;
    
    public Uni<Void> validateDateRange(LocalDate startDate, LocalDate endDate) {
        int daysDiff = calculateDaysDifference(startDate, endDate);
        
        if (daysDiff > MAX_DAYS_ALLOWED) {
            return Uni.createFrom().failure(new RuntimeException(
                    "Período muy amplio. Máximo " + MAX_DAYS_ALLOWED + " días permitidos para estadísticas."));
        }
        
        return Uni.createFrom().voidItem();
    }
    
    private int calculateDaysDifference(LocalDate startDate, LocalDate endDate) {
        return startDate.until(endDate).getDays();
    }
}

