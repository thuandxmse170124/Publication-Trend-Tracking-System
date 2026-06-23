package com.publication_trend_tracking_system.sever_web_app.service;

public interface EmailService {

    void sendEmail(
            String to,
            String subject,
            String content
    );
}