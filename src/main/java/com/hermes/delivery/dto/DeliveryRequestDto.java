package com.hermes.delivery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DeliveryRequestDto(
        @NotNull
        Long senderProfileId,

        @NotNull
        Long recipientProfileId,

        @NotBlank
        String pickUpAddress,

        @NotBlank
        String dropOffAddress,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal deliveryFee
) {}