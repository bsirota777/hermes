package com.hermes.pricing;

import com.hermes.geocoding.Coordinates;
import java.math.BigDecimal;

public interface RoutingService {
    BigDecimal getDistanceKm(Coordinates origin, Coordinates destination);
}