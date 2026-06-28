package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ResearchFieldResponse;
import java.util.List;

public interface ResearchFieldService {
    List<ResearchFieldResponse> getAllFields();
}
