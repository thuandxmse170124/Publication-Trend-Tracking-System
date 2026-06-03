package com.publication_trend_tracking_system.sever_web_app.dto.request;

import lombok.Data;

@Data
public class ChangePasswordRequest {

    private String oldPassword;

    private String newPassword;
}
