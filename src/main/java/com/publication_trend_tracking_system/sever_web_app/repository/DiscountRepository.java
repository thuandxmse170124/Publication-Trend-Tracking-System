package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountRepository
        extends JpaRepository<Discount, Long> {
    java.util.Optional<Discount> findFirstByIsActiveTrue();
}