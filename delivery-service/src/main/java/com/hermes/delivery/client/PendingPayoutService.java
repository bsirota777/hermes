package com.hermes.delivery.client;

import com.hermes.common.wallet.CreditWalletRequest;
import com.hermes.delivery.entity.PendingPayout;
import com.hermes.delivery.repository.PendingPayoutRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PendingPayoutService {

    private static final Logger log = LoggerFactory.getLogger(PendingPayoutService.class);

    private final PendingPayoutRepository pendingPayoutRepository;

    public PendingPayoutService(PendingPayoutRepository pendingPayoutRepository) {
        this.pendingPayoutRepository = pendingPayoutRepository;
    }

    public void recordFailedPayout(CreditWalletRequest request) {
        log.warn("wallet-service unavailable - recording pending payout for user {} (delivery {})",
                request.userId(), request.deliveryId());

        PendingPayout payout = new PendingPayout(
                request.userId(), request.amount(), request.type(), request.deliveryId());

        pendingPayoutRepository.save(payout);
    }
}
