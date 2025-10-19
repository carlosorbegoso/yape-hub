package org.sky.dto.response.common;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PaginationInfo(
    int currentPage,
    int totalPages,
    long totalElements,
    int pageSize,
    boolean hasNext,
    boolean hasPrevious
) {
    // Constructor compacto - validaciones y normalizaciones
    public PaginationInfo {
        // Validaciones
        if (currentPage < 0) {
            throw new IllegalArgumentException("Current page cannot be negative");
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("Total pages cannot be negative");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("Total elements cannot be negative");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        
        // Validaciones de consistencia
        if (currentPage > 0 && currentPage > totalPages) {
            throw new IllegalArgumentException("Current page cannot be greater than total pages");
        }
        
        // Recalcular flags si es necesario
        hasNext = currentPage < totalPages;
        hasPrevious = currentPage > 1;
    }
    
    // Constructor de conveniencia
    public static PaginationInfo create(int currentPage, long totalElements, int pageSize) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean hasNext = currentPage < totalPages;
        boolean hasPrevious = currentPage > 1;
        
        return new PaginationInfo(currentPage, totalPages, totalElements, pageSize, hasNext, hasPrevious);
    }
    
    // Métodos de conveniencia
    public long totalItems() {
        return totalElements;
    }
    
    public int itemsPerPage() {
        return pageSize;
    }
    
    public boolean isEmpty() {
        return totalElements == 0;
    }
    
    public boolean isFirstPage() {
        return currentPage == 1;
    }
    
    public boolean isLastPage() {
        return currentPage == totalPages;
    }
}
