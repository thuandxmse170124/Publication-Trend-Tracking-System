package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long notificationId;

    private String title;

    private String message;

    private Boolean isRead;

    private LocalDateTime createdAt;

    private Long relatedId;

    // Tells the client which page relatedId belongs to. Null for rows created before typed
    // notifications, which the client treats as legacy paper links.
    private com.publication_trend_tracking_system.sever_web_app.enums.NotificationType type;

    // Number of papers behind an aggregated entry.
    private Integer relatedCount;
}