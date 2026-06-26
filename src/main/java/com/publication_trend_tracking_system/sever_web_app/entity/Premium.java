package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

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

    private BigDecimal amount;

    @Column(name = "duration_days")
    private Integer durationDays;

    private String description;

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToMany
    @JoinTable(
            name = "premium_discounts",
            joinColumns =
            @JoinColumn(name = "premium_id"),
            inverseJoinColumns =
            @JoinColumn(name = "discount_id")
    )
    @Builder.Default
    private Set<Discount> discounts = new HashSet<>();
}