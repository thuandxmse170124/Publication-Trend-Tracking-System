package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PaymentResponse;
import com.publication_trend_tracking_system.sever_web_app.service.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "api")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{invoiceId}")
    public ApiResponse<PaymentResponse> createPayment(
            @PathVariable Long invoiceId
    ) {

        return ApiResponse.<PaymentResponse>builder()
                .code(1000)
                .message("Create Payment Success")
                .result(
                        paymentService.createPayment(invoiceId)
                )
                .build();
    }
}