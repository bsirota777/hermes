package com.hermes.pricing;

import com.hermes.parcel.dto.ParcelDto;
import com.hermes.geocoding.Coordinates;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PricingService {

    private final PricingConfig pricingConfig;
    private final RoutingService routingService;

    public PricingService(PricingConfig pricingConfig, RoutingService routingService) {
        this.pricingConfig = pricingConfig;
        this.routingService = routingService;
    }

    public BigDecimal calculateDeliveryFee(Coordinates pickUp, Coordinates dropOff, List<ParcelDto> parcels) {
        BigDecimal distanceKm = routingService.getDistanceKm(pickUp, dropOff);
        BigDecimal totalWeightKg = parcels.stream()
                .map(ParcelDto::weightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return pricingConfig.getBaseFee()
                .add(distanceKm.multiply(pricingConfig.getRatePerKm()))
                .add(totalWeightKg.multiply(pricingConfig.getRatePerKg()));
    }
}