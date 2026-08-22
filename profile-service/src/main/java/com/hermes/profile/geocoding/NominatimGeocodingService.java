package com.hermes.profile.geocoding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

@Service @Primary
public class NominatimGeocodingService implements GeocodingService {
    private final RestClient client;
    public NominatimGeocodingService(@Value("${nominatim.base-url:https://nominatim.openstreetmap.org}") String baseUrl,
                                     @Value("${nominatim.user-agent}") String userAgent) {
        this.client = RestClient.builder().baseUrl(baseUrl).defaultHeader("User-Agent", userAgent).build();
    }
    @Override
    public Coordinates geocode(String address) {
        List<Map> results = client.get().uri(uri -> uri.path("/search").queryParam("q", address).queryParam("format", "jsonv2").queryParam("limit", 1).build())
                .retrieve().body(List.class);
        if (results == null || results.isEmpty()) throw new GeocodingFailedException(address, "No results found");
        Map result = results.get(0);
        return new Coordinates(Double.parseDouble(result.get("lat").toString()), Double.parseDouble(result.get("lon").toString()));
    }
}
