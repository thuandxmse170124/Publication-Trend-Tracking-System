package com.publication_trend_tracking_system.sever_web_app.serviceImpl;


import com.publication_trend_tracking_system.sever_web_app.dto.response.PaymentResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Invoice;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.InvoiceRepository;

import com.publication_trend_tracking_system.sever_web_app.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class PaymentServiceImpl
        implements PaymentService {

    private final PayOS payOS;

    private final InvoiceRepository invoiceRepository;

    @Override
    public PaymentResponse createPayment(
            Long invoiceId
    ) {

        Invoice invoice =
                invoiceRepository
                        .findById(invoiceId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.INVOICE_NOT_FOUND
                                ));

        if (!"PENDING".equals(invoice.getStatus())) {

            throw new AppException(
                    ErrorCode.INVALID_INVOICE_STATUS
            );
        }

        try {

            PaymentLinkItem item =
                    PaymentLinkItem.builder()
                            .name(
                                    invoice.getPremium()
                                            .getPackageName()
                            )
                            .price(
                                    invoice.getFinalAmount()
                                            .longValue()
                            )
                            .quantity(1)
                            .build();

            CreatePaymentLinkRequest request =
                    CreatePaymentLinkRequest.builder()
                            .orderCode(
                                    invoice.getInvoiceId()
                            )
                            .amount(
                                    invoice.getFinalAmount()
                                            .longValue()
                            )
                            .description(
                                    "Premium"
                            )
                            .returnUrl(
                                    "http://localhost:3000/payment/success"
                            )
                            .cancelUrl(
                                    "http://localhost:3000/payment/cancel"
                            )
                            .item(item)
                            .build();

            CreatePaymentLinkResponse response =
                    payOS.paymentRequests()
                            .create(request);

            return PaymentResponse.builder()
                    .checkoutUrl(
                            response.getCheckoutUrl()
                    )
                    .paymentLinkId(
                            response.getPaymentLinkId()
                    )
                    .qrCode(
                            response.getQrCode()
                    )
                    .build();

        } catch (Exception e) {

            e.printStackTrace();

            throw new AppException(
                    ErrorCode.PAYMENT_CREATE_FAILED
            );

        }

    }
}