package org.sky.service.hubnotifications;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.request.payment.PaymentNotificationRequest;
import org.sky.model.PaymentNotificationEntity;
import org.sky.model.PaymentRejectionEntity;
import org.sky.model.SellerEntity;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.PaymentRejectionRepository;
import org.sky.repository.SellerRepository;
import org.sky.repository.UserRepository;
import org.sky.model.UserEntityEntity;

import java.util.List;

@ApplicationScoped
public class PaymentNotificationDataService {

    @Inject
    PaymentNotificationRepository paymentRepository;

    @Inject
    PaymentRejectionRepository rejectionRepository;

    @Inject
    SellerRepository sellerRepository;

    @Inject
    UserRepository userRepository;

    public Uni<PaymentNotificationEntity> savePaymentNotification(PaymentNotificationEntity payment) {
        return paymentRepository.persist(payment);
    }

    @WithTransaction
    public Uni<List<SellerEntity>> findSellersByAdminId(Long adminId) {
        return sellerRepository.findByAdminId(adminId);
    }

    public Uni<List<PaymentNotificationEntity>> findPendingPaymentsForSeller(Long sellerId, int page, int size, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return paymentRepository.findPendingPaymentsForSeller(sellerId, page, size, startDate, endDate);
    }

    public Uni<List<PaymentNotificationEntity>> findAllPendingPayments(int page, int size, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return paymentRepository.findAllPendingPayments(page, size, startDate, endDate);
    }

    public Uni<Long> countPendingPaymentsForSeller(Long sellerId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return paymentRepository.countPendingPaymentsForSeller(sellerId, startDate, endDate);
    }

    public Uni<Long> countAllPendingPayments(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return paymentRepository.countAllPendingPayments(startDate, endDate);
    }

    public Uni<PaymentNotificationEntity> findPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }

    public Uni<PaymentNotificationEntity> updatePaymentStatus(Long paymentId, String status) {
        return paymentRepository.updatePaymentStatus(paymentId, status);
    }

    public Uni<PaymentRejectionEntity> savePaymentRejection(PaymentRejectionEntity rejection) {
        return rejectionRepository.persist(rejection);
    }

    public Uni<PaymentNotificationEntity> createPaymentForSeller(PaymentNotificationRequest request, SellerEntity seller) {
        PaymentNotificationEntity payment = PaymentNotificationMapper.REQUEST_WITH_SELLER_TO_ENTITY.apply(request).apply(seller);
        return savePaymentNotification(payment);
    }
    
    /**
     * Busca un usuario por su ID
     */
    public Uni<UserEntityEntity> findUserById(Long userId) {
        return userRepository.findById(userId);
    }
    
    /**
     * Busca pagos pendientes para un admin (todos los pagos de sus vendedores)
     */
    public Uni<List<PaymentNotificationEntity>> findPendingPaymentsForAdmin(Long adminId, int page, int size, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return paymentRepository.findPendingPaymentsForAdmin(adminId, page, size, startDate, endDate);
    }
    
    /**
     * Cuenta pagos pendientes para un admin
     */
    public Uni<Long> countPendingPaymentsForAdmin(Long adminId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return paymentRepository.countPendingPaymentsForAdmin(adminId, startDate, endDate);
    }
    
    /**
     * Busca pagos para un admin por estado específico
     */
    public Uni<List<PaymentNotificationEntity>> findPaymentsForAdminByStatus(Long adminId, int page, int size, String status, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return paymentRepository.findPaymentsForAdminByStatus(adminId, page, size, status, startDate, endDate);
    }
    
    /**
     * Cuenta pagos para un admin por estado específico
     */
    public Uni<Long> countPaymentsForAdminByStatus(Long adminId, String status, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return paymentRepository.countPaymentsForAdminByStatus(adminId, status, startDate, endDate);
    }
    
    /**
     * Suma montos totales para un admin por estado específico
     */
    public Uni<Double> sumAmountForAdminByStatus(Long adminId, String status, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return paymentRepository.sumAmountForAdminByStatus(adminId, status, startDate, endDate);
    }
    
}
