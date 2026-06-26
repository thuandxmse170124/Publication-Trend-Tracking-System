package com.publication_trend_tracking_system.sever_web_app.repository;

import com.publication_trend_tracking_system.sever_web_app.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSubscriptionRepository
        extends JpaRepository<
                UserSubscription,
                Long> {

    Optional<UserSubscription>
    findFirstByUser_UserIdAndStatusOrderByEndDateDesc(
            Long userId,
            String status
    );
}