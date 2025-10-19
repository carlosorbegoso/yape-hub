package org.sky.dto.response.admin;

import org.sky.dto.response.common.PaginationInfo;
import org.sky.dto.response.payment.PaymentDetail;
import org.sky.dto.response.payment.PaymentSummary;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record AdminPaymentManagementResponse(
    List<PaymentDetail> payments,
    PaymentSummary summary,
    PaginationInfo pagination
) {}
