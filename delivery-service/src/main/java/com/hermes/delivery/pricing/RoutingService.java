package com.hermes.delivery.pricing;

import com.hermes.delivery.geocoding.Coordinates;
import java.math.BigDecimal;

public interface RoutingService {
    BigDecimal getDistanceKm(Coordinates origin, Coordinates destination);
}