package org.sky.controller;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sky.dto.ApiResponse;
import org.sky.dto.transaction.TransactionListResponse;
import org.sky.dto.transaction.TransactionResponse;
import org.sky.service.TransactionService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Path("/api/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Transaction Management", description = "Transaction management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {
    
    @Inject
    TransactionService transactionService;
    
    @GET
    @Operation(summary = "Get transactions", description = "Get list of transactions with filters and pagination")
    public Uni<Response> getTransactions(@QueryParam("adminId") Long adminId,
                                   @QueryParam("page") @DefaultValue("1") int page,
                                   @QueryParam("limit") @DefaultValue("50") int limit,
                                   @QueryParam("startDate") String startDateStr,
                                   @QueryParam("endDate") String endDateStr,
                                   @QueryParam("branchId") Long branchId,
                                   @QueryParam("sellerId") Long sellerId,
                                   @QueryParam("status") @DefaultValue("all") String status) {
        
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        
        if (startDateStr != null) {
            startDate = LocalDateTime.parse(startDateStr + "T00:00:00");
        }
        if (endDateStr != null) {
            endDate = LocalDateTime.parse(endDateStr + "T23:59:59");
        }
        
        return transactionService.getTransactions(adminId, page, limit, startDate, endDate, branchId, sellerId, status)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                });
    }
    
    @POST
    @Path("/{transactionId}/confirm")
    @Operation(summary = "Confirm transaction", description = "Confirm a transaction manually")
    public Uni<Response> confirmTransaction(@PathParam("transactionId") Long transactionId,
                                      @QueryParam("sellerId") Long sellerId,
                                      @QueryParam("notes") String notes) {
        return transactionService.confirmTransaction(transactionId, sellerId, notes)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                });
    }
    
    @POST
    @Path("/{transactionId}/process")
    @Operation(summary = "Process transaction", description = "Mark transaction as processed")
    public Uni<Response> processTransaction(@PathParam("transactionId") Long transactionId) {
        return transactionService.processTransaction(transactionId)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                });
    }
    
    @GET
    @Path("/sellers/{sellerId}")
    @Operation(summary = "Get transactions by seller", description = "Get transactions for a specific seller")
    public Uni<Response> getTransactionsBySeller(@PathParam("sellerId") Long sellerId,
                                           @QueryParam("page") @DefaultValue("1") int page,
                                           @QueryParam("limit") @DefaultValue("20") int limit,
                                           @QueryParam("startDate") String startDateStr,
                                           @QueryParam("endDate") String endDateStr) {
        
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        
        if (startDateStr != null) {
            startDate = LocalDateTime.parse(startDateStr + "T00:00:00");
        }
        if (endDateStr != null) {
            endDate = LocalDateTime.parse(endDateStr + "T23:59:59");
        }
        
        return transactionService.getTransactions(null, page, limit, startDate, endDate, null, sellerId, "all")
                .map(response -> {
                    if (response.isSuccess()) {
                        return Response.ok(response).build();
                    } else {
                        return Response.status(400).entity(response).build();
                    }
                });
    }
}
