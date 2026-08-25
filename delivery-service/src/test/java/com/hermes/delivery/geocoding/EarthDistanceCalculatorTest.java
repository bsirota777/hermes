package com.hermes.delivery.geocoding;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

// Logic is unchanged from the pre-split monolith - ported as-is, just repackaged.
class EarthDistanceCalculatorTest {

    private static final BigDecimal TOLERANCE_KM = new BigDecimal("0.01");

    @Test
    void calculateHaversineDistance_forSamePoint_returnsZero() {
        BigDecimal distance = EarthDistanceCalculator.calculateHaversineDistance(
                -33.8688, 151.2093, -33.8688, 151.2093);
        assertThat(distance).isCloseTo(BigDecimal.ZERO, within(TOLERANCE_KM));
    }

    @Test
    void calculateHaversineDistance_forOneDegreeLatitudeAtEquator_isApproxOneHundredElevenKm() {
        BigDecimal distance = EarthDistanceCalculator.calculateHaversineDistance(0, 0, 1, 0);
        assertThat(distance).isCloseTo(new BigDecimal("111.19492664455873"), within(TOLERANCE_KM));
    }

    @Test
    void calculateHaversineDistance_forOneDegreeLongitudeAtEquator_isApproxOneHundredElevenKm() {
        BigDecimal distance = EarthDistanceCalculator.calculateHaversineDistance(0, 0, 0, 1);
        assertThat(distance).isCloseTo(new BigDecimal("111.19492664455873"), within(TOLERANCE_KM));
    }

    @Test
    void calculateHaversineDistance_betweenSydneyAndMelbourne_matchesKnownGreatCircleDistance() {
        BigDecimal distance = EarthDistanceCalculator.calculateHaversineDistance(
                -33.8688, 151.2093, -37.8136, 144.9631);
        assertThat(distance).isCloseTo(new BigDecimal("713.4274807201239"), within(TOLERANCE_KM));
    }

    @Test
    void calculateHaversineDistance_isSymmetric() {
        BigDecimal sydneyToMelbourne = EarthDistanceCalculator.calculateHaversineDistance(
                -33.8688, 151.2093, -37.8136, 144.9631);
        BigDecimal melbourneToSydney = EarthDistanceCalculator.calculateHaversineDistance(
                -37.8136, 144.9631, -33.8688, 151.2093);
        assertThat(sydneyToMelbourne).isCloseTo(melbourneToSydney, within(TOLERANCE_KM));
    }

    @Test
    void calculateHaversineDistance_forAntipodalPointsOnEquator_isHalfEarthCircumference() {
        BigDecimal distance = EarthDistanceCalculator.calculateHaversineDistance(0, 0, 0, 180);
        assertThat(distance).isCloseTo(new BigDecimal("20015.086796020572"), within(TOLERANCE_KM));
    }
}
