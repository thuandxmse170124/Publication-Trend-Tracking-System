package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.UserResponse;

import java.util.List;

public interface AdminUserService {

    org.springframework.data.domain.Page<UserResponse> getAllUsers(org.springframework.data.domain.Pageable pageable);

    void activateUser(Long userId);

    void deactivateUser(Long userId);

    void banUser(Long userId);
}