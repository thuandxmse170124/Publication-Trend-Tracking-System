package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction,Long>{

    List<PaymentTransaction>
    findByInvoice_User_UserIdOrderByTransactionDateDesc(
            Long userId
    );

}