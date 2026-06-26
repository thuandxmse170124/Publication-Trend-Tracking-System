package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.PersonalStatsResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SystemStatsResponse;


import java.util.List;

public interface DashboardService {
    SystemStatsResponse getSystemStats();
    PersonalStatsResponse getPersonalStats();

}
