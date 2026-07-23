package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

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

    // Not @Nationalized: the actual DB column is varchar, not nvarchar (same issue as
    // Journal.name — see comment there).
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "affiliation")
    private String affiliation;

    @Column(name = "orcid")
    private String orcid;
}
