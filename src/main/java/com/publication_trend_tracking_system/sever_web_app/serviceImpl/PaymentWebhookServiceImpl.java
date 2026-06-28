package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.entity.Invoice;
import com.publication_trend_tracking_system.sever_web_app.entity.PaymentTransaction;
import com.publication_trend_tracking_system.sever_web_app.entity.Premium;
import com.publication_trend_tracking_system.sever_web_app.entity.UserSubscription;
import com.publication_trend_tracking_system.sever_web_app.repository.InvoiceRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.PaymentTransactionRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserSubscriptionRepository;
import com.publication_trend_tracking_system.sever_web_app.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl
        implements PaymentWebhookService {

    private final PayOS payOS;

    private final InvoiceRepository invoiceRepository;

    private final PaymentTransactionRepository paymentTransactionRepository;

    private final UserSubscriptionRepository userSubscriptionRepository;

    @Override
    public void handleWebhook(
            Map<String, Object> body
    ) {

        WebhookData data =
                payOS
                        .webhooks()
                        .verify(body);

        Long orderCode =
                data.getOrderCode();

        Invoice invoice =
                invoiceRepository
                        .findById(orderCode)
                        .orElseThrow();

        if ("PAID".equals(invoice.getStatus())) {
            return;
        }

        invoice.setStatus("PAID");

        invoiceRepository.save(invoice);

        PaymentTransaction transaction =
                PaymentTransaction.builder()
                        .invoice(invoice)
                        .amountPaid(invoice.getFinalAmount())
                        .paymentMethod("PAYOS")
                        .transactionStatus("SUCCESS")
                        .transactionDate(LocalDateTime.now())
                        .build();

        paymentTransactionRepository.save(transaction);

        Premium premium =
                invoice.getPremium();

        UserSubscription subscription =
                UserSubscription.builder()
                        .user(invoice.getUser())
                        .premium(premium)
                        .startDate(LocalDateTime.now())
                        .endDate(
                                LocalDateTime.now()
                                        .plusDays(
                                                premium.getDurationDays()
                                        )
                        )
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now())
                        .build();

        userSubscriptionRepository.save(subscription);

    }

}