package org.sky.service.stats.calculators.admin.period;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.stats.SalesStatsResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
@ApplicationScoped
public class PeriodInfoCalculator {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public SalesStatsResponse.PeriodInfo createPeriodInfo(LocalDate startDate, LocalDate endDate) {
        int daysDiff = calculateDaysDifference(startDate, endDate);
        
        return new SalesStatsResponse.PeriodInfo(
                formatDate(startDate),
                formatDate(endDate),
                daysDiff + 1
        );
    }
    
    private int calculateDaysDifference(LocalDate startDate, LocalDate endDate) {
        return startDate.until(endDate).getDays();
    }
    
    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }
}

