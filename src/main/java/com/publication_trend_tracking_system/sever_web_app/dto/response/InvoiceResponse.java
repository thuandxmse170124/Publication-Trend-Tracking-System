package com.publication_trend_tracking_system.sever_web_app.dto.response;

import com.publication_trend_tracking_system.sever_web_app.enums.InvoiceStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {

    private Long invoiceId;

    private String packageName;

    private Integer durationDays;

    private BigDecimal originalAmount;

    private Double discountPercent;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private InvoiceStatus status;
    private Long orderCode;
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;
}