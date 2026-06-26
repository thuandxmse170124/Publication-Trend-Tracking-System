package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long transactionId;

    private Long invoiceId;

    private BigDecimal amountPaid;

    private String paymentMethod;

    private String transactionStatus;
}