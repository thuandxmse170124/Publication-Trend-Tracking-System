package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.Invoice;
import com.publication_trend_tracking_system.sever_web_app.entity.Premium;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository
        extends JpaRepository<Invoice, Long> {

    boolean existsByPremium(Premium premium);

    List<Invoice> findByUser(User user);
    Optional<Invoice> findByOrderCode(
            Long orderCode
    );
    Optional<Invoice>
    findFirstByUserAndStatusOrderByCreatedAtDesc(
            User user,
            InvoiceStatus status
    );
}