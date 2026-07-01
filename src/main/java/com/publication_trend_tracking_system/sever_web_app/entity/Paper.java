package com.publication_trend_tracking_system.sever_web_app.entity;

import com.publication_trend_tracking_system.sever_web_app.enums.PaperPublicationType;
import com.publication_trend_tracking_system.sever_web_app.enums.PaperVisibilityStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "papers", indexes = {
    @Index(name = "idx_papers_title", columnList = "title"),
    @Index(name = "idx_papers_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "paper_id")
    private Long paperId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private Journal journal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id")
    private ResearchField field;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_source_id")
    private ApiSource apiSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_type", nullable = false)
    private PaperPublicationType publicationType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "abstract", columnDefinition = "VARCHAR(MAX)")
    private String paperAbstract;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "doi", unique = true)
    private String doi;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "citation_count", nullable = false)
    private Integer citationCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_status", nullable = false)
    private PaperVisibilityStatus visibilityStatus;

    @Column(name = "is_open_access")
    private Boolean isOpenAccess;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "paper_authors",
        joinColumns = @JoinColumn(name = "paper_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @Builder.Default
    private Set<Author> authors = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "paper_keywords",
        joinColumns = @JoinColumn(name = "paper_id"),
        inverseJoinColumns = @JoinColumn(name = "keyword_id")
    )
    @Builder.Default
    private Set<Keyword> keywords = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "paper_topics",
        joinColumns = @JoinColumn(name = "paper_id"),
        inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    @Builder.Default
    private Set<Topic> topics = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (publicationType == null) {
            publicationType = PaperPublicationType.OTHER;
        }

        if (visibilityStatus == null) {
            visibilityStatus = PaperVisibilityStatus.VISIBLE;
        }

        if (citationCount == null) {
            citationCount = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
