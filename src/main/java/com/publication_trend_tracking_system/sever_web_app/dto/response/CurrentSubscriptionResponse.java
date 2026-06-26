package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentSubscriptionResponse {
    private boolean premium;

    private String packageName;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String status;
}
