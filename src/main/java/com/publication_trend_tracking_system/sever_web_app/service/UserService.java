package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.request.ChangePasswordRequest;

public interface UserService {

    void changePassword(
            ChangePasswordRequest request);
}