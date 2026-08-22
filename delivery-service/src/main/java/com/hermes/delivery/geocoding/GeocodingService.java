package com.hermes.delivery.geocoding;

public interface GeocodingService {
    Coordinates geocode(String address);
}