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

    @Column(name = "journal_id", nullable = false)
    private String journalId;

    @Column(name = "journal_name", nullable = false)
    private String journalName;

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