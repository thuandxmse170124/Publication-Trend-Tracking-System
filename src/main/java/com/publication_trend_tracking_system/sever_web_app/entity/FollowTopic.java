package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "follow_topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long followId;

    @Column(name = "topic_id", nullable = false)
    private String topicId;

    @Column(name = "topic_name", nullable = false)
    private String topicName;

    @Column(name = "followed_at")
    private LocalDateTime followedAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    public void prePersist() {
        followedAt = LocalDateTime.now();
    }
}