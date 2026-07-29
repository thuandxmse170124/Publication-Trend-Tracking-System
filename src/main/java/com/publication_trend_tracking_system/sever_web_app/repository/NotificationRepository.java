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

    // Paged feed. The unpaged variant above loads a user's entire history into memory on every
    // dropdown open, which grows without bound as syncs accumulate.
    org.springframework.data.domain.Page<Notification>
    findByUserUserIdOrderByCreatedAtDesc(
            Long userId,
            org.springframework.data.domain.Pageable pageable);

    // The badge only needs a number. Counting in the database keeps it correct no matter how many
    // rows exist, whereas counting the fetched page would only ever see that page.
    long countByUserUserIdAndIsReadFalse(Long userId);

    List<Notification>
    findByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(
            Long userId);

    @Modifying
    @Transactional
    void deleteByUserUserId(
            Long userId);
}