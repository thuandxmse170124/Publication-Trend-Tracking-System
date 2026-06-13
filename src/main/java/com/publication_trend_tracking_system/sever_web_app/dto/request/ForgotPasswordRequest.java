package com.publication_trend_tracking_system.sever_web_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest {

    @NotBlank(message = "EMAIL_REQUIRED")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@(gmail\\.com|.*\\.edu|.*\\.edu\\.vn)$",
            message = "INVALID_EMAIL")
    private String email;
}
