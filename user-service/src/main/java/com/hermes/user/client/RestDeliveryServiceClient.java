package com.hermes.user.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestDeliveryServiceClient implements DeliveryServiceClient {
    private final RestClient restClient;

    public RestDeliveryServiceClient(@Value("${delivery-service.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    @Override
    public DeliveryCounts getCountsForUser(Long userId) {
        DeliveryCounts counts = restClient.get()
                .uri("/internal/deliveries/counts/{userId}", userId)
                .retrieve()
                .body(DeliveryCounts.class);
        return counts != null ? counts : new DeliveryCounts(0, 0);
    }
}
