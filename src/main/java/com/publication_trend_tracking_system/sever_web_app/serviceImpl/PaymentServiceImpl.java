package com.publication_trend_tracking_system.sever_web_app.serviceImpl;


import com.publication_trend_tracking_system.sever_web_app.dto.response.PaymentResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Invoice;
import com.publication_trend_tracking_system.sever_web_app.enums.InvoiceStatus;
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

        if (invoice.getStatus() != InvoiceStatus.PENDING) {

            throw new AppException(
                    ErrorCode.INVALID_INVOICE_STATUS
            );
        }

        // A payment link was already created for this invoice earlier — PayOS rejects a second
        // create() call for the same orderCode ("Payment order already exists"), and its
        // get-by-orderCode API doesn't return the checkoutUrl/qrCode back, so the only way to
        // let the user pay is to hand back what we saved from the first successful call.
        if (invoice.getOrderCode() != null && invoice.getCheckoutUrl() != null) {
            return PaymentResponse.builder()
                    .checkoutUrl(invoice.getCheckoutUrl())
                    .paymentLinkId(invoice.getPaymentLinkId())
                    .qrCode(invoice.getQrCode())
                    .build();
        }

        Long orderCode;

        if (invoice.getOrderCode() == null) {

            orderCode = System.currentTimeMillis();

            invoice.setOrderCode(orderCode);

            invoiceRepository.save(invoice);

        } else {

            orderCode = invoice.getOrderCode();
        }

        try {

            PaymentLinkItem item =
                    PaymentLinkItem.builder()
                            .name(
                                    invoice.getPackageName()
                            )
                            .price(
                                    invoice.getFinalAmount()
                                            .longValue()
                            )
                            .quantity(1)
                            .build();

            CreatePaymentLinkRequest request =
                    CreatePaymentLinkRequest.builder()
                            .orderCode(orderCode)
                            .amount(
                                    invoice.getFinalAmount().longValue()
                            )
                            .description(
                                    invoice.getPackageName()
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

            invoice.setCheckoutUrl(response.getCheckoutUrl());
            invoice.setPaymentLinkId(response.getPaymentLinkId());
            invoice.setQrCode(response.getQrCode());
            invoiceRepository.save(invoice);

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