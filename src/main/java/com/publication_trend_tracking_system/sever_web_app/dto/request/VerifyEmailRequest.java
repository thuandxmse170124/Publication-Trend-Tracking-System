package com.publication_trend_tracking_system.sever_web_app.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailRequest {

    @NotBlank(message = "INVALID_EMAIL")
    private String email;

    @NotBlank(message = "OTP_REQUIRED")
    private String otp;
}