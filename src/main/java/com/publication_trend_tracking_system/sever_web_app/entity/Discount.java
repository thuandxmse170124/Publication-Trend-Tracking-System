package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "discounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_id")
    private Long discountId;

    @Column(name = "discount_name")
    private String discountName;

    @Column(name = "discount_percent")
    private Double discountPercent;

    @Column(name = "from_date")
    private LocalDateTime fromDate;

    @Column(name = "to_date")
    private LocalDateTime toDate;

    @Column(name = "is_active")
    private Boolean isActive;
    @ManyToMany(mappedBy = "discounts")
    @Builder.Default
    private Set<Premium> premiums = new HashSet<>();
}