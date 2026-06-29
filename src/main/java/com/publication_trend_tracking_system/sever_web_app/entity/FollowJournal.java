package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "follow_journals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowJournal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long followId;

    @ManyToOne
    @JoinColumn(name = "journal_id")
    private Journal journal;

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