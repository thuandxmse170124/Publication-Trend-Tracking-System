package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "api_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "source_id")
    private Integer sourceId;

    @Column(name = "source_name", nullable = false, unique = true)
    private String sourceName;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "api_key_ref")
    private String apiKeyRef;

    @Column(name = "status", nullable = false)
    private String status;
}
