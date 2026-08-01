package com.hermes.pricing;

import java.math.BigDecimal;

public class EarthDistanceCalculator {
    // Average radius of the Earth in kilometers
    private static final double EARTH_RADIUS = 6371.0;

    public static BigDecimal calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Differences in coordinates
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        // Haversine formula
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Calculate the final distance
        return BigDecimal.valueOf(EARTH_RADIUS * c);
    }

    public static void main(String[] args) {
        // Example: Sydney (Lat: -33.8688, Lon: 151.2093) to Melbourne (Lat: -37.8136, Lon: 144.9631)
        double lat1 = -33.8688;
        double lon1 = 151.2093;
        double lat2 = -37.8136;
        double lon2 = 144.9631;

        BigDecimal distance = calculateHaversineDistance(lat1, lon1, lat2, lon2);
        System.out.printf("Straight-line distance: %.2f km%n", distance);
    }
}
