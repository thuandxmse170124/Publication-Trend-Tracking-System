package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "related_id")
    private Long relatedId;

    // Null on rows written before typed notifications existed; the frontend falls back to its old
    // behaviour for those rather than guessing.
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 32)
    private com.publication_trend_tracking_system.sever_web_app.enums.NotificationType type;

    // How many papers this one row stands for. New papers are aggregated per follow target instead
    // of written one row per paper, so a sync that adds hundreds of papers to a followed topic
    // produces a single "N new papers" entry rather than hundreds of near-identical ones.
    @Column(name = "related_count")
    private Integer relatedCount;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        isRead = false;
    }
}