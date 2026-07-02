package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountResponse {

    private Long discountId;

    private String discountName;

    private Double discountPercent;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    private Boolean isActive;
}