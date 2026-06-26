package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.request.PaymentRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PaymentResponse;
import com.publication_trend_tracking_system.sever_web_app.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ApiResponse<PaymentResponse>
    processPayment(
            @RequestBody
            PaymentRequest request
    ) {

        return ApiResponse
                .<PaymentResponse>builder()
                .code(1000)
                .message("Payment Success")
                .result(
                        paymentService
                                .processPayment(
                                        request))
                .build();
    }
}