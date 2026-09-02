package com.hermes.delivery.scheduler;

import com.hermes.delivery.client.WalletServiceClient;
import com.hermes.delivery.entity.PayoutStatus;
import com.hermes.delivery.entity.PendingPayout;
import com.hermes.delivery.repository.PendingPayoutRepository;
import com.hermes.common.wallet.CreditWalletRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;

@Component
public class PendingPayoutRetryScheduler {

    private final PendingPayoutRepository repository;
    private final WalletServiceClient walletServiceClient;

    public PendingPayoutRetryScheduler(PendingPayoutRepository repository,
                                       WalletServiceClient walletServiceClient) {
        this.repository = repository;
        this.walletServiceClient = walletServiceClient;
    }

    @Scheduled(fixedDelay = 60000) // every 60s
    public void retryPendingPayouts() {
        List<PendingPayout> pending = repository.findByStatus(PayoutStatus.PENDING);
        for (PendingPayout payout : pending) {
            try {
                walletServiceClient.credit(new CreditWalletRequest(
                        payout.getUserId(), payout.getAmount(), payout.getType(), payout.getDeliveryId()));
                payout.setStatus(PayoutStatus.COMPLETED);
            } catch (Exception e) {
                payout.setAttempts(payout.getAttempts() + 1);
                payout.setLastAttemptAt(Instant.now());
                if (payout.getAttempts() >= 5) {
                    payout.setStatus(PayoutStatus.FAILED); // needs manual/ops attention
                }
            }
            repository.save(payout);
        }
    }
}