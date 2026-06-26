package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.CurrentSubscriptionResponse;

public interface UserSubscriptionService {

    boolean isPremium(Long userId);

    CurrentSubscriptionResponse getCurrentSubscription(Long userId);

}