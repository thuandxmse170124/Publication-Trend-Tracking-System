package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionResponse {

    private Long transactionId;

    private String paymentMethod;

    private BigDecimal amountPaid;

    private String transactionStatus;

    private LocalDateTime transactionDate;

}