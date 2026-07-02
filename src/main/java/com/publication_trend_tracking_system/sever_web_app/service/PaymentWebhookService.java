package com.publication_trend_tracking_system.sever_web_app.service;

import java.util.Map;

public interface PaymentWebhookService {

    void handleWebhook(
            Map<String, Object> body
    );

}