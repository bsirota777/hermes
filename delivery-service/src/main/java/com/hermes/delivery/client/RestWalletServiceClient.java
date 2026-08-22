package com.hermes.delivery.client;

import com.hermes.common.wallet.CreditWalletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestWalletServiceClient implements WalletServiceClient {
    private final RestClient restClient;

    public RestWalletServiceClient(@Value("${wallet-service.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    @Override
    public void credit(CreditWalletRequest request) {
        restClient.post()
                .uri("/internal/wallet/credit")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}