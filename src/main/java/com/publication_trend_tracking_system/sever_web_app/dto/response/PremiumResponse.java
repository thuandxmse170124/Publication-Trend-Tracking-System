package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumResponse {

    private Long premiumId;

    private String packageName;

    private Double amount;

    private Integer durationDays;

    private String description;
}