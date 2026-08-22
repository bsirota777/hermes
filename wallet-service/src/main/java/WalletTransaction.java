package com.hermes.wallet;

import com.hermes.delivery.Delivery;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @NotNull
    private Wallet wallet;

    // Positive for credits (EARNING, REFUND), negative for debits (CASHOUT, PAYMENT)
    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletTransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_delivery_id")
    private Delivery relatedDelivery;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public WalletTransaction(Wallet wallet, BigDecimal amount, WalletTransactionType type, Delivery relatedDelivery) {
        this.wallet = wallet;
        this.amount = amount;
        this.type = type;
        this.relatedDelivery = relatedDelivery;
    }
}