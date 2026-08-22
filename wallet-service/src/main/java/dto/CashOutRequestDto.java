package com.hermes.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CashOutRequestDto(
        @NotNull
        @DecimalMin(value = "10.00")
        BigDecimal amount
) {}