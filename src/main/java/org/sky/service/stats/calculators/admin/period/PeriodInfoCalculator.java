package org.sky.service.stats.calculators.admin.period;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.response.common.PeriodInfo;
import org.sky.dto.response.stats.SalesStatsResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
@ApplicationScoped
public class PeriodInfoCalculator {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public PeriodInfo createPeriodInfo(LocalDate startDate, LocalDate endDate) {
        int daysDiff = calculateDaysDifference(startDate, endDate);
        
        return new PeriodInfo(
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

