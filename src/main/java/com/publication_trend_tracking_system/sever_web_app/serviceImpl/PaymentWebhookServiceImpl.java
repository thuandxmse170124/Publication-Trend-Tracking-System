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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookServiceImpl
        implements PaymentWebhookService {

    private final PayOS payOS;

    private final InvoiceRepository invoiceRepository;

    private final PaymentTransactionRepository paymentTransactionRepository;

    private final UserSubscriptionRepository userSubscriptionRepository;

    @Value("${payos.bypass-signature-check:false}")
    private Boolean bypassSignatureCheck;

    @Override
    @Transactional
    public void handleWebhook(
            Map<String, Object> body
    ) {
        log.info("Processing PayOS Webhook payload: {}", body);

        Long orderCode = null;
        try {
            WebhookData data =
                    payOS
                            .webhooks()
                            .verify(body);
            orderCode = data.getOrderCode();
            log.info("Webhook signature verified successfully. OrderCode: {}", orderCode);
        } catch (Exception e) {
            log.error("PayOS Webhook Signature Verification Failed: {}", e.getMessage());
            if (Boolean.TRUE.equals(bypassSignatureCheck)) {
                log.warn("Bypassing signature verification (payos.bypass-signature-check is enabled).");
                orderCode = extractOrderCodeFromRawBody(body);
                if (orderCode == null) {
                    log.error("Bypass failed: Could not extract orderCode from body.");
                    throw new RuntimeException("Failed to extract orderCode from webhook body", e);
                }
                log.info("Extracted orderCode from raw body: {}", orderCode);
            } else {
                throw new RuntimeException("PayOS Webhook Signature Verification Failed", e);
            }
        }

        final Long finalOrderCode = orderCode;
        Invoice invoice =
                invoiceRepository
                        .findById(finalOrderCode)
                        .orElseThrow(() -> {
                            log.error("Invoice not found for orderCode (invoiceId): {}", finalOrderCode);
                            return new java.util.NoSuchElementException("Invoice not found for ID: " + finalOrderCode);
                        });

        if ("PAID".equals(invoice.getStatus())) {
            log.info("Invoice {} is already PAID. Returning early.", orderCode);
            return;
        }

        invoice.setStatus("PAID");

        invoiceRepository.save(invoice);
        log.info("Updated Invoice status to PAID for invoice ID: {}", orderCode);

        PaymentTransaction transaction =
                PaymentTransaction.builder()
                        .invoice(invoice)
                        .amountPaid(invoice.getFinalAmount())
                        .paymentMethod("PAYOS")
                        .transactionStatus("SUCCESS")
                        .transactionDate(LocalDateTime.now())
                        .build();

        paymentTransactionRepository.save(transaction);
        log.info("Saved PaymentTransaction for invoice ID: {}", orderCode);

        Premium premium =
                invoice.getPremium();
        if (premium != null) {
            premium.setIsActive(true);
            log.info("Set Premium package {} status to active", premium.getPackageName());
        }

        UserSubscription subscription =
                UserSubscription.builder()
                        .user(invoice.getUser())
                        .premium(premium)
                        .startDate(LocalDateTime.now())
                        .endDate(
                                LocalDateTime.now()
                                        .plusDays(
                                                premium != null ? premium.getDurationDays() : 30
                                        )
                        )
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now())
                        .build();

        userSubscriptionRepository.save(subscription);
        log.info("Created and saved UserSubscription for user: {}, plan: {}",
                invoice.getUser() != null ? invoice.getUser().getEmail() : "unknown",
                premium != null ? premium.getPackageName() : "unknown");

    }

    private Long extractOrderCodeFromRawBody(Map<String, Object> body) {
        try {
            if (body != null && body.containsKey("data")) {
                Object dataObj = body.get("data");
                if (dataObj instanceof Map) {
                    Map<?, ?> dataMap = (Map<?, ?>) dataObj;
                    Object orderCodeObj = dataMap.get("orderCode");
                    if (orderCodeObj != null) {
                        return Long.valueOf(orderCodeObj.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting orderCode from raw webhook body", e);
        }
        return null;
    }

}