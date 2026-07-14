package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUserUserIdOrderByCreatedAtDesc(
            Long userId);

    List<Notification>
    findByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(
            Long userId);

    @Modifying
    @Transactional
    void deleteByUserUserId(
            Long userId);
}