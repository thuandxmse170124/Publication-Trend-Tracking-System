package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "premiums")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Premium {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "premium_id")
    private Long premiumId;

    @Column(name = "package_name")
    private String packageName;

    private Double amount;

    @Column(name = "duration_days")
    private Integer durationDays;

    private String description;

    @Column(name = "is_active")
    private Boolean isActive;
}