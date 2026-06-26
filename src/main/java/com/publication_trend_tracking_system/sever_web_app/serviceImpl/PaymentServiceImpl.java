package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.request.PaymentRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PaymentResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Invoice;
import com.publication_trend_tracking_system.sever_web_app.entity.PaymentTransaction;
import com.publication_trend_tracking_system.sever_web_app.entity.Premium;
import com.publication_trend_tracking_system.sever_web_app.entity.UserSubscription;
import com.publication_trend_tracking_system.sever_web_app.repository.InvoiceRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.PaymentTransactionRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserSubscriptionRepository;
import com.publication_trend_tracking_system.sever_web_app.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class PaymentServiceImpl
        implements PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PaymentTransactionRepository
            paymentTransactionRepository;

    @Override
    public PaymentResponse processPayment(
            PaymentRequest request
    ) {

        Invoice invoice =
                invoiceRepository
                        .findById(
                                request.getInvoiceId())
                        .orElseThrow();

        if ("PAID".equals(
                invoice.getStatus())) {

            throw new RuntimeException(
                    "Invoice already paid");
        }

        invoice.setStatus("PAID");

        invoiceRepository.save(invoice);

        PaymentTransaction transaction =
                PaymentTransaction.builder()
                        .invoice(invoice)
                        .paymentMethod(
                                request.getPaymentMethod())
                        .amountPaid(
                                invoice.getFinalAmount())
                        .transactionStatus(
                                "SUCCESS")
                        .transactionDate(
                                LocalDateTime.now())
                        .build();

        transaction =
                paymentTransactionRepository
                        .save(transaction);

        Premium premium =
                invoice.getPremium();

        LocalDateTime startDate =
                LocalDateTime.now();

        LocalDateTime endDate =
                startDate.plusDays(
                        premium.getDurationDays()
                );

        UserSubscription subscription =
                UserSubscription.builder()
                        .user(
                                invoice.getUser())
                        .premium(
                                premium)
                        .startDate(
                                startDate)
                        .endDate(
                                endDate)
                        .status(
                                "ACTIVE")
                        .createdAt(
                                LocalDateTime.now())
                        .build();

        userSubscriptionRepository
                .save(subscription);

        return PaymentResponse.builder()
                .transactionId(
                        transaction.getTransactionId())
                .invoiceId(
                        invoice.getInvoiceId())
                .amountPaid(
                        transaction.getAmountPaid())
                .paymentMethod(
                        transaction.getPaymentMethod())
                .transactionStatus(
                        transaction.getTransactionStatus())
                .build();
    }
}