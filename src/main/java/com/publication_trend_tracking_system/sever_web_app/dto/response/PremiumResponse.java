package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumResponse {

    private Long premiumId;

    private String packageName;

    private BigDecimal amount;

    private Integer durationDays;

    private String description;
}