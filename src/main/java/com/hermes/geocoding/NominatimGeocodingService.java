package com.hermes.geocoding;

import com.hermes.geocoding.exception.GeocodingFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Primary
public class NominatimGeocodingService implements GeocodingService {

    private static final String BASE_URL = "https://nominatim.openstreetmap.org/search";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String userAgent;

    public NominatimGeocodingService(@Value("${nominatim.user-agent}") String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public Coordinates geocode(String address) {
        Coordinates result = tryGeocode(address);
        if (result != null) {
            return result;
        }

        /*String streetLevelFallback = dropLeadingHouseNumber(address);
        if (streetLevelFallback != null) {
            try {
                Thread.sleep(1100); // respect Nominatim's 1 req/sec usage policy before retrying
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Coordinates fallbackResult = tryGeocode(streetLevelFallback);
            if (fallbackResult != null) {
                return fallbackResult;
            }
        }*/

        throw new GeocodingFailedException(address, "No results found");
    }

    /**
     * Attempts to geocode the given query. Returns null (rather than throwing) if no
     * results are found, so callers can fall back to a less specific query.
     */
    private Coordinates tryGeocode(String query) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(BASE_URL)
                    .queryParam("q", query)
                    .queryParam("format", "jsonv2")
                    .queryParam("addressdetails", 1)
                    .queryParam("limit", 5)
                    .build()
                    .encode()
                    .toUri();

            System.out.println("Nominatim URI: " + uri);
            System.out.println("Query: [" + query + "]");

            HttpHeaders headers = new HttpHeaders();
            headers.set(
                    HttpHeaders.USER_AGENT,
                    "HermesDeliveryApp/1.0 (oceanis_au@outlook.com)"
            );
            headers.set(
                    HttpHeaders.ACCEPT,
                    MediaType.APPLICATION_JSON_VALUE
            );

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            System.out.println("Nominatim status: " + response.getStatusCode());
            System.out.println("Nominatim body: " + response.getBody());

            JsonNode results = objectMapper.readTree(response.getBody());

            if (!results.isArray() || results.isEmpty()) {
                return null;
            }

            JsonNode result = results.get(0);

            double lat = result.get("lat").asDouble();
            double lon = result.get("lon").asDouble();

            return new Coordinates(lat, lon);

        } catch (Exception e) {
            System.out.println("Nominatim call threw: " + e);
            throw new GeocodingFailedException(query, e);
        }
    }

    /**
     * Given "395 Bourke Street, Melbourne VIC 3000, Australia", returns
     * "Bourke Street, Melbourne VIC 3000, Australia" — i.e. drops a leading house
     * number so the street itself can still be geocoded when the exact address
     * point isn't in OSM's data (common for Australian addresses).
     * Returns null if the input doesn't start with a house number.
     */
    private String dropLeadingHouseNumber(String address) {
        String[] parts = address.split(",", 2);
        if (parts.length < 2) {
            return null;
        }

        String firstSegment = parts[0].trim();
        int firstSpace = firstSegment.indexOf(' ');
        if (firstSpace <= 0) {
            return null;
        }

        String possibleHouseNumber = firstSegment.substring(0, firstSpace);
        if (!possibleHouseNumber.chars().anyMatch(Character::isDigit)) {
            return null;
        }

        String streetOnly = firstSegment.substring(firstSpace + 1).trim();
        return streetOnly + "," + parts[1];
    }
}