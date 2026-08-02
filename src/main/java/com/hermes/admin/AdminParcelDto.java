package com.hermes.admin;

import java.math.BigDecimal;

public record AdminParcelDto(
        Long id,
        String description,
        BigDecimal weightKg,
        BigDecimal declaredValue,
        boolean insured,
        BigDecimal insuredValue
) {}