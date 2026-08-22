package com.hermes.profile.client;
import com.hermes.common.user.UserSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class RestUserServiceClient implements UserServiceClient {
    private final RestClient restClient;
    public RestUserServiceClient(@Value("${user-service.base-url}") String baseUrl) { this.restClient = RestClient.create(baseUrl); }
    public UserSummary getUser(Long userId) {
        return restClient.get().uri("/internal/users/{id}", userId).retrieve().body(UserSummary.class);
    }
}
