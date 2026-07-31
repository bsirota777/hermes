package com.hermes.geocoding;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.Geometry;
import com.google.maps.model.LatLng;
import com.hermes.geocoding.exception.GeocodingFailedException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class GoogleGeocodingServiceTest {

    private final GoogleGeocodingService geocodingService = new GoogleGeocodingService("fake-api-key");

    @Test
    void geocode_returnsCoordinates_whenAddressIsValid() throws Exception {
        GeocodingResult result = new GeocodingResult();
        result.geometry = new Geometry();
        result.geometry.location = new LatLng(-37.8136, 144.9631); // Melbourne

        try (MockedStatic<GeocodingApi> mockedApi = mockStatic(GeocodingApi.class)) {
            var request = mock(com.google.maps.GeocodingApiRequest.class);
            when(GeocodingApi.geocode(any(GeoApiContext.class), anyString())).thenReturn(request);
            when(request.await()).thenReturn(new GeocodingResult[]{result});

            Coordinates coords = geocodingService.geocode("123 Collins St, Melbourne VIC");

            assertThat(coords.latitude()).isEqualTo(-37.8136);
            assertThat(coords.longitude()).isEqualTo(144.9631);
        }
    }

    @Test
    void geocode_throwsGeocodingFailedException_whenNoResultsFound() throws Exception {
        try (MockedStatic<GeocodingApi> mockedApi = mockStatic(GeocodingApi.class)) {
            var request = mock(com.google.maps.GeocodingApiRequest.class);
            when(GeocodingApi.geocode(any(GeoApiContext.class), anyString())).thenReturn(request);
            when(request.await()).thenReturn(new GeocodingResult[0]);

            assertThatThrownBy(() -> geocodingService.geocode("nonexistent address xyz"))
                    .isInstanceOf(GeocodingFailedException.class)
                    .hasMessageContaining("No results found");
        }
    }

    @Test
    void geocode_throwsGeocodingFailedException_whenApiCallFails() throws Exception {
        try (MockedStatic<GeocodingApi> mockedApi = mockStatic(GeocodingApi.class)) {
            var request = mock(com.google.maps.GeocodingApiRequest.class);
            when(GeocodingApi.geocode(any(GeoApiContext.class), anyString())).thenReturn(request);
            when(request.await()).thenThrow(new RuntimeException("network error"));

            assertThatThrownBy(() -> geocodingService.geocode("123 Collins St"))
                    .isInstanceOf(GeocodingFailedException.class);
        }
    }
}
