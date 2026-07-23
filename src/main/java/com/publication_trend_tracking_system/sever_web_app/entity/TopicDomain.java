package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "topic_domains")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "domain_id")
    private Integer domainId;

    @Column(name = "openalex_id", nullable = false, unique = true)
    private String openalexId;

    @Column(name = "display_name", nullable = false)
    private String displayName;
}
