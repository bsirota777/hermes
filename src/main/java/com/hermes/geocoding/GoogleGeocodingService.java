package com.hermes.geocoding;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.hermes.geocoding.exception.GeocodingFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleGeocodingService implements GeocodingService {

    private final GeoApiContext context;

    public GoogleGeocodingService(@Value("${google.maps.api-key}") String apiKey) {
        this.context = new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public Coordinates geocode(String address) {
        try {
            GeocodingResult[] results = GeocodingApi.geocode(context, address).await();
            if (results.length == 0) {
                throw new GeocodingFailedException(address, "No results found");
            }
            LatLng location = results[0].geometry.location;
            return new Coordinates(location.lat, location.lng);
        } catch (Exception e) {
            throw new GeocodingFailedException(address, e);
        }
    }
}
