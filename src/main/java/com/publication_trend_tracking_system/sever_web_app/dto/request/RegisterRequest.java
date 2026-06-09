package com.publication_trend_tracking_system.sever_web_app.dto.request;



import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "FULLNAME_REQUIRED")
    @Size(min = 3, max = 100, message = "INVALID_FULLNAME")
    private String fullName;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@(gmail\\.com|.*\\.edu|.*\\.edu\\.vn)$",
            message = "INVALID_EMAIL"
    )
    private String email;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, message = "INVALID_PASSWORD")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$",
            message = "PASSWORD_INVALID_FORMAT"
    )
    private String password;
}