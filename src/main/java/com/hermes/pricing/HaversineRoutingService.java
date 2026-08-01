package com.hermes.pricing;

import com.hermes.geocoding.Coordinates;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Service
public class HaversineRoutingService implements RoutingService {

    private final RestClient restClient;
    private final String apiKey;

    public HaversineRoutingService(RestClient.Builder builder,
                                   @Value("${google.maps.api-key}") String apiKey) {
        this.restClient = builder.baseUrl("https://maps.googleapis.com").build();
        this.apiKey = apiKey;
    }

    @Override
    public BigDecimal getDistanceKm(Coordinates pickUp, Coordinates dropOff) {
        // call Distance Matrix API, parse response, return km
        // TODO:
        return EarthDistanceCalculator.calculateHaversineDistance(pickUp.latitude(), pickUp.longitude(),
                dropOff.latitude(), dropOff.longitude());
    }
}