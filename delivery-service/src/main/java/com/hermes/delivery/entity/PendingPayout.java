package com.hermes.delivery.entity;

import com.hermes.common.wallet.WalletTransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pending_payouts")
@Getter
@Setter
@NoArgsConstructor
public class PendingPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletTransactionType type;

    @Column(name = "delivery_id", nullable = false)
    private Long deliveryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    public PendingPayout(Long userId, BigDecimal amount, WalletTransactionType type, Long deliveryId) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.deliveryId = deliveryId;
        this.status = PayoutStatus.PENDING;
    }
}