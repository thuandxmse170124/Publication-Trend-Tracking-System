package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "topic_subfields")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicSubfield {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subfield_id")
    private Integer subfieldId;

    @Column(name = "openalex_id", nullable = false, unique = true)
    private String openalexId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private TopicField field;
}
