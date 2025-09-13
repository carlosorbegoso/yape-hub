package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import org.sky.dto.ApiResponse;
import org.sky.dto.transaction.TransactionListResponse;
import org.sky.dto.transaction.TransactionResponse;
import org.sky.model.Transaction;
import org.sky.repository.TransactionRepository;
import org.sky.repository.SellerRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class TransactionService {
    
    @Inject
    TransactionRepository transactionRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    public Uni<ApiResponse<TransactionListResponse>> getTransactions(Long adminId, int page, int limit, 
                                                              LocalDateTime startDate, LocalDateTime endDate,
                                                              Long branchId, Long sellerId, String status) {
        Uni<List<Transaction>> transactionsUni;
        
        if (adminId != null) {
            transactionsUni = transactionRepository.findByAdminId(adminId);
        } else if (branchId != null) {
            transactionsUni = transactionRepository.findByBranchId(branchId);
        } else if (sellerId != null) {
            transactionsUni = transactionRepository.findBySellerId(sellerId);
        } else {
            transactionsUni = transactionRepository.listAll();
        }
        
        return transactionsUni.map(transactions -> {
            // Filter by date range
            if (startDate != null && endDate != null) {
                transactions = transactions.stream()
                        .filter(txn -> txn.timestamp.isAfter(startDate) && txn.timestamp.isBefore(endDate))
                        .collect(Collectors.toList());
            }
            
            // Filter by status
            if (status != null && !status.equals("all")) {
                if ("pending".equals(status)) {
                    transactions = transactions.stream()
                            .filter(txn -> !txn.isProcessed)
                            .collect(Collectors.toList());
                } else if ("confirmed".equals(status)) {
                    transactions = transactions.stream()
                            .filter(txn -> txn.isProcessed)
                            .collect(Collectors.toList());
                }
            }
            
            // Pagination
            int totalItems = transactions.size();
            int totalPages = (int) Math.ceil((double) totalItems / limit);
            int startIndex = (page - 1) * limit;
            int endIndex = Math.min(startIndex + limit, totalItems);
            
            List<Transaction> paginatedTransactions = transactions.subList(startIndex, endIndex);
            
            List<TransactionResponse> transactionResponses = paginatedTransactions.stream()
                    .map(txn -> new TransactionResponse(
                            txn.id, txn.securityCode, txn.amount, txn.timestamp,
                            txn.description, txn.type, txn.branch.admin.businessName,
                            txn.branch.id, txn.branch.name, 
                            txn.seller != null ? txn.seller.id : null,
                            txn.seller != null ? txn.seller.sellerName : null,
                            txn.isProcessed, txn.paymentMethod, txn.customerPhone
                    ))
                    .collect(Collectors.toList());
            
            TransactionListResponse.PaginationInfo pagination = new TransactionListResponse.PaginationInfo(
                    page, totalPages, totalItems, limit
            );
            
            TransactionListResponse response = new TransactionListResponse(transactionResponses, pagination);
            
            return ApiResponse.success("Transacciones obtenidas exitosamente", response);
        });
    }
    
    @WithTransaction
    public Uni<ApiResponse<TransactionResponse>> confirmTransaction(Long transactionId, Long sellerId, String notes) {
        return transactionRepository.findById(transactionId)
                .chain(transaction -> {
                    if (transaction == null) {
                        return Uni.createFrom().item(ApiResponse.<TransactionResponse>error("Transacción no encontrada"));
                    }
                    
                    transaction.isConfirmed = true;
                    transaction.confirmedAt = LocalDateTime.now();
                    transaction.notes = notes;
                    
                    if (sellerId != null) {
                        return sellerRepository.findById(sellerId)
                                .chain(seller -> {
                                    transaction.seller = seller;
                                    return transactionRepository.persist(transaction);
                                })
                                .map(persistedTransaction -> {
                                    TransactionResponse response = new TransactionResponse(
                                            persistedTransaction.id, persistedTransaction.securityCode, persistedTransaction.amount, persistedTransaction.timestamp,
                                            persistedTransaction.description, persistedTransaction.type, persistedTransaction.branch.admin.businessName,
                                            persistedTransaction.branch.id, persistedTransaction.branch.name,
                                            persistedTransaction.seller != null ? persistedTransaction.seller.id : null,
                                            persistedTransaction.seller != null ? persistedTransaction.seller.sellerName : null,
                                            persistedTransaction.isProcessed, persistedTransaction.paymentMethod, persistedTransaction.customerPhone
                                    );
                                    return ApiResponse.success("Transacción confirmada exitosamente", response);
                                });
                    } else {
                        return transactionRepository.persist(transaction)
                                .map(persistedTransaction -> {
                                    TransactionResponse response = new TransactionResponse(
                                            persistedTransaction.id, persistedTransaction.securityCode, persistedTransaction.amount, persistedTransaction.timestamp,
                                            persistedTransaction.description, persistedTransaction.type, persistedTransaction.branch.admin.businessName,
                                            persistedTransaction.branch.id, persistedTransaction.branch.name,
                                            persistedTransaction.seller != null ? persistedTransaction.seller.id : null,
                                            persistedTransaction.seller != null ? persistedTransaction.seller.sellerName : null,
                                            persistedTransaction.isProcessed, persistedTransaction.paymentMethod, persistedTransaction.customerPhone
                                    );
                                    return ApiResponse.success("Transacción confirmada exitosamente", response);
                                });
                    }
                });
    }
    
    @WithTransaction
    public Uni<ApiResponse<TransactionResponse>> processTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .chain(transaction -> {
                    if (transaction == null) {
                        return Uni.createFrom().item(ApiResponse.<TransactionResponse>error("Transacción no encontrada"));
                    }
                    
                    transaction.isProcessed = true;
                    transaction.processedAt = LocalDateTime.now();
                    
                    return transactionRepository.persist(transaction)
                            .map(persistedTransaction -> {
                                TransactionResponse response = new TransactionResponse(
                                        persistedTransaction.id, persistedTransaction.securityCode, persistedTransaction.amount, persistedTransaction.timestamp,
                                        persistedTransaction.description, persistedTransaction.type, persistedTransaction.branch.admin.businessName,
                                        persistedTransaction.branch.id, persistedTransaction.branch.name,
                                        persistedTransaction.seller != null ? persistedTransaction.seller.id : null,
                                        persistedTransaction.seller != null ? persistedTransaction.seller.sellerName : null,
                                        persistedTransaction.isProcessed, persistedTransaction.paymentMethod, persistedTransaction.customerPhone
                                );
                                return ApiResponse.success("Transacción procesada exitosamente", response);
                            });
                });
    }
}
