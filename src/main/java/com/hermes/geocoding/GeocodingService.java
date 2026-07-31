package com.hermes.geocoding;

public interface GeocodingService {
    Coordinates geocode(String address);
}