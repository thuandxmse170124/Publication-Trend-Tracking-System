package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.entity.Invoice;
import com.publication_trend_tracking_system.sever_web_app.entity.PaymentTransaction;
import com.publication_trend_tracking_system.sever_web_app.entity.Premium;
import com.publication_trend_tracking_system.sever_web_app.entity.UserSubscription;
import com.publication_trend_tracking_system.sever_web_app.enums.InvoiceStatus;
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
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookServiceImpl
        implements PaymentWebhookService {

    private final PayOS payOS;

    private final InvoiceRepository invoiceRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    @Value("${payos.bypass-signature-check:false}")
    private Boolean bypassSignatureCheck;

    @Override
    @Transactional
    public void handleWebhook(
            Map<String, Object> body
    ) {
        log.info("===== WEBHOOK RECEIVED =====");
        log.info("Processing PayOS Webhook payload: {}", body);

        // A payload with no signature field at all is PayOS confirming the URL, which they do when
        // the webhook is registered in their dashboard and periodically afterwards. It carries no
        // order to process, and it has to be answered with 200 — a failure here is what stops the
        // webhook from being accepted, and without an accepted webhook a completed payment never
        // reaches us and the subscription is never activated.
        //
        // This is only for a *missing* signature. One that is present and wrong still fails below:
        // that is a forged callback, not a health check.
        if (body == null || body.get("signature") == null) {
            log.info("No signature in payload — treating as a PayOS webhook confirmation ping.");
            return;
        }

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
                        .findByOrderCode(finalOrderCode)
                        .orElseThrow(() -> {

                            log.error(
                                    "Invoice not found with orderCode {}",
                                    finalOrderCode
                            );

                            return new NoSuchElementException(
                                    "Invoice not found"
                            );

                        });

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            log.info("Invoice {} is already PAID. Returning early.", orderCode);
            return;
        }

        invoice.setStatus(InvoiceStatus.PAID);

        invoice.setPaidAt(
                LocalDateTime.now()
        );

        invoiceRepository.save(invoice);
        log.info("Updated Invoice status to PAID for invoice ID: {}", invoice.getInvoiceId());


        Premium premium = invoice.getPremium();

        UserSubscription subscription =
                userSubscriptionRepository
                        .findFirstByUser_UserIdAndStatusOrderByEndDateDesc(
                                invoice.getUser().getUserId(),
                                "ACTIVE"
                        )
                        .orElse(null);

        if (subscription == null) {

            subscription =
                    UserSubscription.builder()
                            .user(invoice.getUser())
                            .premium(premium)
                            .startDate(LocalDateTime.now())
                            .endDate(
                                    LocalDateTime.now()
                                            .plusDays(
                                                    invoice.getDurationDays()
                                            )
                            )
                            .status("ACTIVE")
                            .createdAt(LocalDateTime.now())
                            .build();

            log.info("Create new Premium Subscription");

        } else {
            subscription.setPremium(premium);

            if (subscription.getEndDate().isAfter(LocalDateTime.now())) {

                subscription.setEndDate(
                        subscription.getEndDate()
                                .plusDays(
                                        invoice.getDurationDays()
                                )
                );


                log.info("Extend Premium Subscription");

            } else {


                subscription.setStartDate(
                        LocalDateTime.now()
                );

                subscription.setEndDate(
                        LocalDateTime.now()
                                .plusDays(
                                        invoice.getDurationDays()
                                )
                );

                subscription.setPremium(premium);
                subscription.setStatus("ACTIVE");

                log.info("Restart Premium Subscription");
            }
        }

        userSubscriptionRepository.save(subscription);

        log.info(
                "Saved UserSubscription for user: {}, package: {}",
                invoice.getUser().getEmail(),
                premium.getPackageName()
        );

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