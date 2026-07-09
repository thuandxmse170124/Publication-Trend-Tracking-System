package com.publication_trend_tracking_system.sever_web_app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceRequest {
    @NotNull
    private Long premiumId;
}