package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.TopicSeedStatusResponse;

public interface TopicSeedService {
    void seedOfficialTaxonomy();
    TopicSeedStatusResponse getSeedStatus();
    void cancelSeed();
}
