package com.hermes.delivery.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteDeliveryRequest(@NotBlank String qrCodeToken) {}