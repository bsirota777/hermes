package com.hermes.delivery;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parcels")
@Getter
@Setter
@NoArgsConstructor
public class Parcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @NotNull
    @DecimalMin(value = "0.01", message = "Length must be greater than 0")
    @Column(nullable = false)
    private BigDecimal lengthCm;

    @NotNull
    @DecimalMin(value = "0.01", message = "Width must be greater than 0")
    @Column(nullable = false)
    private BigDecimal widthCm;

    @NotNull
    @DecimalMin(value = "0.01", message = "Height must be greater than 0")
    @Column(nullable = false)
    private BigDecimal heightCm;

    @NotNull
    @DecimalMin(value = "0.01", message = "Weight must be greater than 0")
    @DecimalMax(value = "10.00", message = "Weight cannot exceed 10kg")
    @Column(nullable = false)
    private BigDecimal weightKg;

    @NotNull
    @DecimalMin(value = "0.00", message = "Declared value cannot be negative")
    @Column(nullable = false)
    private BigDecimal declaredValue;

    @Column(nullable = false)
    private boolean insured;

    @DecimalMin(value = "0.00", message = "Insured value cannot be negative")
    private BigDecimal insuredValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}