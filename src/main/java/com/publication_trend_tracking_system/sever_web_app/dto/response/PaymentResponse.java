package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String checkoutUrl;

    private String paymentLinkId;

    private String qrCode;

}