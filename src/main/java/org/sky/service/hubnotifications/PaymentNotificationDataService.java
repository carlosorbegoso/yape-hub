package org.sky.service.hubnotifications;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.payment.PaymentNotificationRequest;
import org.sky.model.PaymentNotification;
import org.sky.model.SellerEntity;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.PaymentRejectionRepository;
import org.sky.repository.SellerRepository;

import java.util.List;

@ApplicationScoped
public class PaymentNotificationDataService {

    @Inject
    PaymentNotificationRepository paymentRepository;

    @Inject
    PaymentRejectionRepository rejectionRepository;

    @Inject
    SellerRepository sellerRepository;

    public Uni<PaymentNotification> savePaymentNotification(PaymentNotification payment) {
        return paymentRepository.persist(payment);
    }

    @WithTransaction
    public Uni<List<SellerEntity>> findSellersByAdminId(Long adminId) {
        return sellerRepository.findByAdminId(adminId);
    }

    public Uni<List<PaymentNotification>> findPendingPaymentsForSeller(Long sellerId, int page, int size, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return paymentRepository.findPendingPaymentsForSeller(sellerId, page, size, startDate, endDate);
    }

    public Uni<List<PaymentNotification>> findAllPendingPayments(int page, int size, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return paymentRepository.findAllPendingPayments(page, size, startDate, endDate);
    }

    public Uni<Long> countPendingPaymentsForSeller(Long sellerId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return paymentRepository.countPendingPaymentsForSeller(sellerId, startDate, endDate);
    }

    public Uni<Long> countAllPendingPayments(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return paymentRepository.countAllPendingPayments(startDate, endDate);
    }

    public Uni<PaymentNotification> findPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }

    public Uni<PaymentNotification> updatePaymentStatus(Long paymentId, String status) {
        return paymentRepository.updatePaymentStatus(paymentId, status);
    }

    public Uni<org.sky.model.PaymentRejection> savePaymentRejection(org.sky.model.PaymentRejection rejection) {
        return rejectionRepository.persist(rejection);
    }

    public Uni<PaymentNotification> createPaymentForSeller(PaymentNotificationRequest request, SellerEntity seller) {
        PaymentNotification payment = PaymentNotificationMapper.REQUEST_WITH_SELLER_TO_ENTITY.apply(request).apply(seller);
        return savePaymentNotification(payment);
    }
}
