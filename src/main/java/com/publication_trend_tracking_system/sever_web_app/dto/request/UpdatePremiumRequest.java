package com.publication_trend_tracking_system.sever_web_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePremiumRequest {

    @NotBlank
    private String packageName;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    @Positive
    private Integer durationDays;

    private String description;

}