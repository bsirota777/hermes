package com.hermes.delivery.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateParcelRequestDto {

    @NotNull
    private Long deliveryId;

    @NotBlank
    private String description;

    @NotNull
    @DecimalMin(value = "0.01", message = "Length must be greater than 0")
    private BigDecimal lengthCm;

    @NotNull
    @DecimalMin(value = "0.01", message = "Width must be greater than 0")
    private BigDecimal widthCm;

    @NotNull
    @DecimalMin(value = "0.01", message = "Height must be greater than 0")
    private BigDecimal heightCm;

    @NotNull
    @DecimalMin(value = "0.01", message = "Weight must be greater than 0")
    @DecimalMax(value = "10.00", message = "Weight cannot exceed 10kg")
    private BigDecimal weightKg;

    @NotNull
    @DecimalMin(value = "0.00", message = "Declared value cannot be negative")
    private BigDecimal declaredValue;

    private boolean insured;

    @DecimalMin(value = "0.00", message = "Insured value cannot be negative")
    private BigDecimal insuredValue;
}