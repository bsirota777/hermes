package com.hermes.delivery.pricing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "pricing")
@Getter
@Setter
public class PricingConfig {
    private BigDecimal baseFee;
    private BigDecimal ratePerKm;
    private BigDecimal ratePerKg;
}