package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.PaymentTransactionResponse;
import com.publication_trend_tracking_system.sever_web_app.repository.PaymentTransactionRepository;
import com.publication_trend_tracking_system.sever_web_app.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentTransactionServiceImpl
        implements PaymentTransactionService {

    private final PaymentTransactionRepository repository;

    @Override
    public List<PaymentTransactionResponse>
    getMyTransactions(Long userId) {

        return repository
                .findByInvoice_User_UserIdOrderByTransactionDateDesc(
                        userId
                )
                .stream()
                .map(transaction ->

                        PaymentTransactionResponse
                                .builder()
                                .transactionId(
                                        transaction.getTransactionId()
                                )
                                .paymentMethod(
                                        transaction.getPaymentMethod()
                                )
                                .amountPaid(
                                        transaction.getAmountPaid()
                                )
                                .transactionStatus(
                                        transaction.getTransactionStatus()
                                )
                                .transactionDate(
                                        transaction.getTransactionDate()
                                )
                                .build()

                )
                .toList();

    }
}