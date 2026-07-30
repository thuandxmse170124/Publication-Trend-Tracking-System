package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_id")
    private Integer topicId;

    @Column(name = "topic_name", nullable = false, unique = true)
    private String topicName;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Builder.Default
    @Column(name = "trend_score")
    private Float trendScore = 0.0f;

    // Null for legacy topics created ad-hoc from OpenAlex concepts before the official
    // 4,516-topic taxonomy was seeded. Not DB-unique: SQL Server unique constraints only
    // allow a single NULL row, so uniqueness for non-null values is enforced in application code.
    @Column(name = "openalex_id")
    private String openalexId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subfield_id")
    private TopicSubfield subfield;

    @Column(name = "last_synced_at")
    private java.time.LocalDateTime lastSyncedAt;

    @PrePersist
    public void prePersist() {
        if (trendScore == null) {
            trendScore = 0.0f;
        }
    }
}
