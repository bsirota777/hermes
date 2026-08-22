package com.hermes.delivery.parcel.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ParcelDto(
        @NotBlank
        String description,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal lengthCm,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal widthCm,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal heightCm,

        @NotNull
        @DecimalMin(value = "0.01")
        @DecimalMax(value = "10.00")
        BigDecimal weightKg,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal declaredValue,

        boolean insured,

        @DecimalMin(value = "0.00")
        BigDecimal insuredValue
) {}