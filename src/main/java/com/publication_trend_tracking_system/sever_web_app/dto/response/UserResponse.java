package com.publication_trend_tracking_system.sever_web_app.dto.response;

import com.publication_trend_tracking_system.sever_web_app.enums.UserStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long userId;

    private String fullName;

    private String email;

    private String role;

    private UserStatus status;
}
