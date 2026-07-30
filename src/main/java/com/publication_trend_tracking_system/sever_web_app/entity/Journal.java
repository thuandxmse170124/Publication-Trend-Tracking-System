package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "journals", indexes = {
    @Index(name = "idx_journals_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Journal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "journal_id")
    private Integer journalId;

    // Name and publisher are NVARCHAR(255) as of the 2026-07-29 migration — see Paper.title.
    // issn stays plain varchar: it is an ASCII identifier, not free text.
    @Nationalized
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "issn")
    private String issn;

    @Nationalized
    @Column(name = "publisher")
    private String publisher;

    @Column(name = "status", nullable = false)
    private String status;
}
