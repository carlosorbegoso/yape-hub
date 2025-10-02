package org.sky.service.stats.calculators.admin.sales;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.stats.AnalyticsSummaryResponse;
import org.sky.dto.stats.DailySalesData;
import org.sky.dto.stats.HourlySalesData;
import org.sky.dto.stats.WeeklySalesData;
import org.sky.dto.stats.MonthlySalesData;
import org.sky.model.PaymentNotification;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class AdminSalesCalculator {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    
    public List<DailySalesData> calculateDailySalesData(List<PaymentNotification> payments, 
                                                                               LocalDate startDate, 
                                                                               LocalDate endDate) {
        List<DailySalesData> dailySales = new ArrayList<>();
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            final LocalDate date = currentDate;
            
            var dayPayments = filterPaymentsByDate(payments, date);
            var confirmedPayments = filterPaymentsByStatus(dayPayments, CONFIRMED_STATUS);
            
            var transactions = (long) dayPayments.size();
            var sales = calculateTotalSales(confirmedPayments);
            var dayName = getDayName(date);
            
            dailySales.add(new DailySalesData(
                date.toString(),
                dayName,
                sales,
                transactions
            ));
            
            currentDate = currentDate.plusDays(1);
        }
        
        return dailySales;
    }
    
    public List<HourlySalesData> calculateHourlySalesData(List<PaymentNotification> payments, 
                                                                                 LocalDate startDate, 
                                                                                 LocalDate endDate) {
        List<HourlySalesData> hourlySales = new ArrayList<>();
        
        for (int hour = 0; hour < 24; hour++) {
            final int currentHour = hour;
            
            var hourPayments = filterPaymentsByHour(payments, currentHour, startDate, endDate);
            var confirmedPayments = filterPaymentsByStatus(hourPayments, CONFIRMED_STATUS);
            
            var transactions = (long) hourPayments.size();
            var sales = calculateTotalSales(confirmedPayments);
            
            hourlySales.add(new HourlySalesData(
                String.format("%02d:00", hour),
                sales,
                transactions
            ));
        }
        
        return hourlySales;
    }
    
    public List<WeeklySalesData> calculateWeeklySalesData(List<PaymentNotification> payments, 
                                                                                 LocalDate startDate, 
                                                                                 LocalDate endDate) {
        List<WeeklySalesData> weeklySales = new ArrayList<>();
        
        // Calcular semanas en el período
        var currentWeek = startDate.with(java.time.DayOfWeek.MONDAY);
        
        while (!currentWeek.isAfter(endDate)) {
            var weekEnd = currentWeek.plusDays(6);
            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }
            
            var weekPayments = filterPaymentsByDateRange(payments, currentWeek, weekEnd);
            var confirmedPayments = filterPaymentsByStatus(weekPayments, CONFIRMED_STATUS);
            
            var transactions = (long) weekPayments.size();
            var sales = calculateTotalSales(confirmedPayments);
            var weekNumber = getWeekNumber(currentWeek);
            
            weeklySales.add(new WeeklySalesData(
                String.format("Semana %d", weekNumber),
                sales,
                transactions
            ));
            
            currentWeek = currentWeek.plusWeeks(1);
        }
        
        return weeklySales;
    }
    
    public List<MonthlySalesData> calculateMonthlySalesData(List<PaymentNotification> payments, 
                                                                                   LocalDate startDate, 
                                                                                   LocalDate endDate) {
        List<MonthlySalesData> monthlySales = new ArrayList<>();
        
        var currentMonth = startDate.withDayOfMonth(1);
        
        while (!currentMonth.isAfter(endDate)) {
            var monthEnd = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth());
            if (monthEnd.isAfter(endDate)) {
                monthEnd = endDate;
            }
            
            var monthPayments = filterPaymentsByDateRange(payments, currentMonth, monthEnd);
            var confirmedPayments = filterPaymentsByStatus(monthPayments, CONFIRMED_STATUS);
            
            var transactions = (long) monthPayments.size();
            var sales = calculateTotalSales(confirmedPayments);
            var monthName = getMonthName(currentMonth);
            
            monthlySales.add(new MonthlySalesData(
                monthName,
                sales,
                transactions
            ));
            
            currentMonth = currentMonth.plusMonths(1);
        }
        
        return monthlySales;
    }
    
    private List<PaymentNotification> filterPaymentsByDate(List<PaymentNotification> payments, LocalDate date) {
        return payments.stream()
                .filter(payment -> payment.createdAt.toLocalDate().equals(date))
                .toList();
    }
    
    private List<PaymentNotification> filterPaymentsByHour(List<PaymentNotification> payments, int hour, 
                                                          LocalDate startDate, LocalDate endDate) {
        return payments.stream()
                .filter(payment -> payment.createdAt.getHour() == hour)
                .filter(payment -> !payment.createdAt.toLocalDate().isBefore(startDate))
                .filter(payment -> !payment.createdAt.toLocalDate().isAfter(endDate))
                .toList();
    }
    
    private List<PaymentNotification> filterPaymentsByDateRange(List<PaymentNotification> payments, 
                                                               LocalDate startDate, LocalDate endDate) {
        return payments.stream()
                .filter(payment -> !payment.createdAt.toLocalDate().isBefore(startDate))
                .filter(payment -> !payment.createdAt.toLocalDate().isAfter(endDate))
                .toList();
    }
    
    private List<PaymentNotification> filterPaymentsByStatus(List<PaymentNotification> payments, String status) {
        return payments.stream()
                .filter(payment -> status.equals(payment.status))
                .toList();
    }
    
    private double calculateTotalSales(List<PaymentNotification> confirmedPayments) {
        return confirmedPayments.stream()
                .mapToDouble(payment -> payment.amount)
                .sum();
    }
    
    private String getDayName(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es"));
    }
    
    private int getWeekNumber(LocalDate date) {
        return date.get(java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfYear());
    }
    
    private String getMonthName(LocalDate date) {
        return date.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
    }
}

