package com.hermes.profile.geocoding;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// No old-monolith equivalent - the old test suite only covered GoogleGeocodingService, which was
// never the @Primary bean even there. Nominatim is what's actually used in production, so it's
// the one worth testing. Real network calls aren't an option in a unit test, and the RestClient
// is built internally in the constructor (no seam to inject a mock), so this spins up a tiny local
// HTTP server (JDK built-in, no extra dependency) as a stand-in for nominatim.openstreetmap.org.
class NominatimGeocodingServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    private String startServer(int statusCode, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/search", exchange -> {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return "http://localhost:" + server.getAddress().getPort();
    }

    @Test
    void geocode_returnsCoordinates_onSuccessfulResponse() throws IOException {
        String baseUrl = startServer(200, """
                [{"lat":"-37.8136","lon":"144.9631","display_name":"Melbourne"}]
                """);
        NominatimGeocodingService service = new NominatimGeocodingService(baseUrl, "HermesTest/1.0");

        Coordinates result = service.geocode("1 New St, Springfield VIC 3000, Australia");

        assertThat(result.latitude()).isEqualTo(-37.8136);
        assertThat(result.longitude()).isEqualTo(144.9631);
    }

    @Test
    void geocode_throws_whenNoResultsReturned() throws IOException {
        String baseUrl = startServer(200, "[]");
        NominatimGeocodingService service = new NominatimGeocodingService(baseUrl, "HermesTest/1.0");

        assertThatThrownBy(() -> service.geocode("nonexistent address"))
                .isInstanceOf(GeocodingFailedException.class)
                .hasMessageContaining("No results found");
    }

    @Test
    void geocode_sendsConfiguredUserAgentHeader() throws IOException {
        AtomicReference<String> capturedUserAgent = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/search", exchange -> {
            capturedUserAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            byte[] bytes = """
                    [{"lat":"-37.8","lon":"144.9"}]
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        NominatimGeocodingService service = new NominatimGeocodingService(baseUrl, "HermesProfileServiceTests/1.0");

        service.geocode("some address");

        assertThat(capturedUserAgent.get()).isEqualTo("HermesProfileServiceTests/1.0");
    }
}
