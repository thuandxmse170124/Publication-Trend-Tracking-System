package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.PaymentTransactionResponse;

import java.util.List;

public interface PaymentTransactionService {

    List<PaymentTransactionResponse>
    getMyTransactions(Long userId);

}