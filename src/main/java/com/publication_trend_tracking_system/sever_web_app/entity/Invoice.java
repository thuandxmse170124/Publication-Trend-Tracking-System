package com.publication_trend_tracking_system.sever_web_app.entity;

import com.publication_trend_tracking_system.sever_web_app.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;


    @Column(name = "order_code", unique = true)
    private Long orderCode;

    // Persisted from PayOS's create-payment-link response so a later "Pay" click can reuse the
    // same link instead of re-calling PayOS with the same orderCode, which it rejects
    // ("Payment order already exists") — and PayOS's get-by-orderCode API doesn't return these
    // back, so there's no way to recover them if they aren't saved here.
    @Column(name = "checkout_url")
    private String checkoutUrl;

    @Column(name = "payment_link_id")
    private String paymentLinkId;

    @Column(name = "qr_code", columnDefinition = "VARCHAR(MAX)")
    private String qrCode;

    // Người mua
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Gói Premium (tham chiếu để truy vết)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "premium_id", nullable = false)
    private Premium premium;

    // Discount áp dụng (có thể null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id")
    private Discount discount;

    /*
     * ===== SNAPSHOT =====
     * Lưu lại thông tin tại thời điểm mua
     */

    @Column(name = "package_name", nullable = false)
    private String packageName;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "original_amount", nullable = false)
    private BigDecimal originalAmount;

    @Column(name = "discount_percent")
    private Double discountPercent;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "final_amount", nullable = false)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    public void prePersist() {

        createdAt = LocalDateTime.now();

        status = InvoiceStatus.PENDING;
    }
}