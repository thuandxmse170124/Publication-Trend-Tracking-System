package com.publication_trend_tracking_system.sever_web_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    @NotBlank(message = "OTP_REQUIRED")
    @Pattern(
            regexp = "^\\d{6}$",
            message = "OTP_INVALID")
    private String otp;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, message = "INVALID_PASSWORD")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$",
            message = "PASSWORD_INVALID_FORMAT")
    private String newPassword;

    @NotBlank(message = "CONFIRM_PASSWORD_REQUIRED")
    private String confirmPassword;
}