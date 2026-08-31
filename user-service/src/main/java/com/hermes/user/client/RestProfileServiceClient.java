package com.hermes.user.client;

import com.hermes.user.dto.AddressResponse;
import com.hermes.user.dto.DriverProfileDto;
import com.hermes.user.dto.UpdateAddressRequest;
import com.hermes.user.dto.DriverRegistrationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class RestProfileServiceClient implements ProfileServiceClient {
    private final RestClient restClient;

    public RestProfileServiceClient(@Value("${profile-service.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    @Override
    public DriverProfileDto registerDriver(Long userId, DriverRegistrationRequest request) {
        return restClient.post()
                .uri("/internal/driver-profiles/{userId}", userId)
                .body(request)
                .retrieve()
                .body(DriverProfileDto.class);
    }

    @Override
    public DriverProfileDto updateDriver(Long userId, DriverRegistrationRequest request) {
        return restClient.patch()
                .uri("/internal/driver-profiles/{userId}", userId)
                .body(request)
                .retrieve()
                .body(DriverProfileDto.class);
    }

    @Override
    public DriverProfileDto getDriver(Long userId) {
        return restClient.get()
                .uri("/internal/driver-profiles/user/{userId}", userId)
                .retrieve()
                .body(DriverProfileDto.class);
    }

    @Override
    public void updateAddress(Long userId, UpdateAddressRequest request) {
        restClient.patch()
                .uri("/internal/profiles/by-user/{userId}/address", userId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public Optional<AddressResponse> getAddress(Long userId) {
        try {
            AddressResponse response = restClient.get()
                    .uri("/internal/profiles/by-user/{userId}", userId)
                    .retrieve()
                    .body(AddressResponse.class);
            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
