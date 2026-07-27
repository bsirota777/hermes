package com.hermes.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeliveryRequestDto(
        @NotNull
        Long senderProfileId,

        @NotNull
        Long recipientProfileId,

        @NotBlank
        String pickUpAddress,

        @NotBlank
        String dropOffAddress
) {}