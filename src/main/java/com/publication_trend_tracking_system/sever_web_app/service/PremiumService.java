package com.publication_trend_tracking_system.sever_web_app.service;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PremiumResponse;

import java.util.List;

public interface PremiumService {

    List<PremiumResponse> getAllPremiums();
}