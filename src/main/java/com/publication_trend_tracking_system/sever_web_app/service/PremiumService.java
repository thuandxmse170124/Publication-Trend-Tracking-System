package com.publication_trend_tracking_system.sever_web_app.service;
import com.publication_trend_tracking_system.sever_web_app.dto.request.CreatePremiumRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.UpdatePremiumRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.UpdatePremiumStatusRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PremiumResponse;

import java.util.List;

public interface PremiumService {

    PremiumResponse createPremium(CreatePremiumRequest request);

    List<PremiumResponse> getAllPremiums();

    PremiumResponse getPremium(Long premiumId);

    PremiumResponse updatePremium(
            Long premiumId,
            UpdatePremiumRequest request
    );

    PremiumResponse updateStatus(
            Long premiumId,
            UpdatePremiumStatusRequest request
    );

    void deletePremium(Long premiumId);

}