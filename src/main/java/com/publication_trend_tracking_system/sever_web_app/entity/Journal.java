package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

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

    // Not @Nationalized: the actual DB column is varchar, not nvarchar. Hibernate binding this
    // as NCHAR against a varchar column made the SQL Server driver reject every read with
    // "The conversion from varchar to NCHAR is unsupported", failing every journal lookup.
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "issn")
    private String issn;

    @Column(name = "publisher")
    private String publisher;

    @Column(name = "status", nullable = false)
    private String status;
}
