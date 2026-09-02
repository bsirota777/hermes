package com.hermes.delivery.client;

import com.hermes.common.wallet.CreditWalletRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestWalletServiceClient implements WalletServiceClient {

    private final RestClient restClient;
    private final PendingPayoutService pendingPayoutService;

    public RestWalletServiceClient(@Value("${wallet-service.base-url}") String baseUrl,
                                    PendingPayoutService pendingPayoutService) {
        this.restClient = RestClient.create(baseUrl);
        this.pendingPayoutService = pendingPayoutService;
    }

    @Override
    @CircuitBreaker(name = "walletService", fallbackMethod = "creditFallback")
    public void credit(CreditWalletRequest request) {
        restClient.post()
                .uri("/internal/wallet/credit")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private void creditFallback(CreditWalletRequest request, Throwable t) {
        pendingPayoutService.recordFailedPayout(request);
    }
}