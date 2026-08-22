package com.hermes.delivery.client;

import com.hermes.common.profile.DriverProfileSummary;
import com.hermes.common.profile.ProfileSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.hermes.common.profile.FindOrCreateProfileRequest;

import java.util.Optional;

@Component
public class RestProfileServiceClient implements ProfileServiceClient {
    private final RestClient restClient;

    public RestProfileServiceClient(@Value("${profile-service.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    @Override
    public ProfileSummary getSenderProfile(Long id) {
        return restClient.get()
                .uri("/internal/sender-profiles/{id}", id)
                .retrieve()
                .body(ProfileSummary.class);
    }

    @Override
    public ProfileSummary getRecipientProfile(Long id) {
        return restClient.get()
                .uri("/internal/recipient-profiles/{id}", id)
                .retrieve()
                .body(ProfileSummary.class);
    }

    @Override
    public DriverProfileSummary getDriverProfile(Long id) {
        return restClient.get()
                .uri("/internal/driver-profiles/{id}", id)
                .retrieve()
                .body(DriverProfileSummary.class);
    }

    @Override
    public ProfileSummary findOrCreateSenderProfile(FindOrCreateProfileRequest request) {
        return restClient.post()
                .uri("/internal/sender-profiles/find-or-create")
                .body(request)
                .retrieve()
                .body(ProfileSummary.class);
    }

    @Override
    public ProfileSummary findOrCreateRecipientProfile(FindOrCreateProfileRequest request) {
        return restClient.post()
                .uri("/internal/recipient-profiles/find-or-create")
                .body(request)
                .retrieve()
                .body(ProfileSummary.class);
    }

    @Override
    public Optional<ProfileSummary> findSenderProfileByUserId(Long userId) {
        try {
            ProfileSummary profile = restClient.get()
                    .uri("/internal/sender-profiles/by-user/{userId}", userId)
                    .retrieve()
                    .body(ProfileSummary.class);
            return Optional.ofNullable(profile);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ProfileSummary> findRecipientProfileByUserId(Long userId) {
        try {
            ProfileSummary profile = restClient.get()
                    .uri("/internal/recipient-profiles/by-user/{userId}", userId)
                    .retrieve()
                    .body(ProfileSummary.class);
            return Optional.ofNullable(profile);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<DriverProfileSummary> findDriverProfileByUserId(Long userId) {
        try {
            DriverProfileSummary profile = restClient.get()
                    .uri("/internal/driver-profiles/by-user/{userId}", userId)
                    .retrieve()
                    .body(DriverProfileSummary.class);
            return Optional.ofNullable(profile);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}