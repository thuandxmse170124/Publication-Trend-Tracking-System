package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserSubscriptionService {

    private final UserSubscriptionRepository
            userSubscriptionRepository;

    public boolean isPremium(
            Long userId
    ) {

        return userSubscriptionRepository
                .findFirstByUser_UserIdAndStatusOrderByEndDateDesc(
                        userId,
                        "ACTIVE"
                )
                .filter(subscription ->
                        subscription.getEndDate()
                                .isAfter(
                                        LocalDateTime.now()))
                .isPresent();
    }
}