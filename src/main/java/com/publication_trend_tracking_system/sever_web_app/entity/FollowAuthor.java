package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "follow_authors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowAuthor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long followId;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private Author author;

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