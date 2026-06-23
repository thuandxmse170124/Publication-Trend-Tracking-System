package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "keywords")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_id")
    private Integer keywordId;

    @Column(name = "keyword_name", nullable = false, unique = true)
    private String keywordName;
}
