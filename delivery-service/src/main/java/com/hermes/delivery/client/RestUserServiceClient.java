package com.hermes.delivery.client;

import com.hermes.common.user.UserSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class RestUserServiceClient implements UserServiceClient {
    private final RestClient restClient;

    public RestUserServiceClient(@Value("${user-service.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    @Override
    public Optional<UserSummary> findUserByEmail(String email) {
        try {
            UserSummary user = restClient.get()
                    .uri("/internal/users/by-email?email={email}", email)
                    .retrieve()
                    .body(UserSummary.class);
            return Optional.ofNullable(user);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
