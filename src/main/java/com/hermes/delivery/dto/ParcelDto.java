package com.hermes.delivery.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ParcelDto {
    private Long id;
    private Long deliveryId;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private BigDecimal declaredValue;
    private boolean insured;
    private BigDecimal insuredValue;
    private LocalDateTime createdAt;

    public ParcelDto(Long id, Long deliveryId, BigDecimal lengthCm, BigDecimal widthCm,
                     BigDecimal heightCm, BigDecimal weightKg, BigDecimal declaredValue,
                     boolean insured, BigDecimal insuredValue, LocalDateTime createdAt) {
        this.id = id;
        this.deliveryId = deliveryId;
        this.lengthCm = lengthCm;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.declaredValue = declaredValue;
        this.insured = insured;
        this.insuredValue = insuredValue;
        this.createdAt = createdAt;
    }
}