package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.CurrentSubscriptionResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.UserSubscription;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.UserSubscriptionRepository;
import com.publication_trend_tracking_system.sever_web_app.service.UserSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserSubscriptionServiceImpl
        implements UserSubscriptionService {

    private final UserSubscriptionRepository repository;

    @Override
    public boolean isPremium(Long userId) {

        return repository
                .findFirstByUser_UserIdAndStatusOrderByEndDateDesc(
                        userId,
                        "ACTIVE"
                )
                .filter(subscription ->
                        subscription.getEndDate()
                                .isAfter(LocalDateTime.now()))
                .isPresent();

    }

    @Override
    public CurrentSubscriptionResponse getCurrentSubscription(
            Long userId
    ) {

        UserSubscription subscription =
                repository
                        .findFirstByUser_UserIdAndStatusOrderByEndDateDesc(
                                userId,
                                "ACTIVE"
                        )
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.SUBSCRIPTION_NOT_FOUND
                                ));

        return CurrentSubscriptionResponse.builder()
                .premium(true)
                .packageName(
                        subscription
                                .getPremium()
                                .getPackageName()
                )
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .status(subscription.getStatus())
                .build();

    }
}
