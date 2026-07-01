package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.request.PaymentRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse createPayment(
            Long invoiceId
    );

}