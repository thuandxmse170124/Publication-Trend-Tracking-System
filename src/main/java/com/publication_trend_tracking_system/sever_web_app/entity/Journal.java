package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "journals")
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

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "issn")
    private String issn;

    @Column(name = "publisher")
    private String publisher;

    @Column(name = "status", nullable = false)
    private String status;
}
