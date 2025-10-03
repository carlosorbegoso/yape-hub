package org.sky.dto.response.common;

public record PaginationInfo(
    int currentPage,
    int totalPages,
    long totalElements,
    int pageSize,
    boolean hasNext,
    boolean hasPrevious
) {
    public long totalItems() {
        return totalElements;
    }
    
    public int itemsPerPage() {
        return pageSize;
    }
}