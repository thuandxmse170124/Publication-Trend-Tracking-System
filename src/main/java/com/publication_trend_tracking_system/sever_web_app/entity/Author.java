package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "authors", indexes = {
    @Index(name = "idx_authors_fullname", columnList = "full_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "author_id")
    private Long authorId;

    // Both columns are NVARCHAR(255) as of the 2026-07-29 migration — see Paper.title.
    @Nationalized
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Nationalized
    @Column(name = "affiliation")
    private String affiliation;

    @Column(name = "orcid")
    private String orcid;
}
