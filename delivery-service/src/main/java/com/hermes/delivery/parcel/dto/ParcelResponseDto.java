package com.hermes.delivery.parcel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ParcelResponseDto(
        Long id,
        Long deliveryId,
        String description,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal declaredValue,
        boolean insured,
        BigDecimal insuredValue,
        LocalDateTime createdAt
) {}