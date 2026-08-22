package com.hermes.wallet;

import com.hermes.common.wallet.WalletTransactionType;
import com.hermes.wallet.exception.InsufficientFundsException;
import com.hermes.wallet.exception.StripeAccountNotLinkedException;
import com.hermes.wallet.exception.StripeOnboardingIncompleteException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PayoutService payoutService;
    private final StripeOnboardingService stripeOnboardingService;

    @Transactional
    public BigDecimal getBalance(Long userId) {
        return getOrCreateWalletByUserId(userId).getBalance();
    }

    @Transactional
    public Wallet credit(Long userId, BigDecimal amount, WalletTransactionType type, Long relatedDeliveryId) {
        validatePositive(amount);
        Wallet wallet = getOrCreateWalletByUserId(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
        walletTransactionRepository.save(new WalletTransaction(wallet, amount, type, relatedDeliveryId));
        return wallet;
    }

    @Transactional
    public Wallet debit(Long userId, BigDecimal amount, WalletTransactionType type, Long relatedDeliveryId) {
        validatePositive(amount);
        Wallet wallet = getOrCreateWalletByUserId(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(wallet.getId(), amount, wallet.getBalance());
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        walletTransactionRepository.save(new WalletTransaction(wallet, amount.negate(), type, relatedDeliveryId));
        return wallet;
    }

    @Transactional
    public Wallet cashOut(Long userId, BigDecimal amount) {
        Wallet wallet = getOrCreateWalletByUserId(userId);
        if (wallet.getStripeAccountId() == null) {
            throw new StripeAccountNotLinkedException(userId);
        }
        if (!wallet.isStripePayoutsEnabled()) {
            throw new StripeOnboardingIncompleteException(userId);
        }

        Wallet debited = debit(userId, amount, WalletTransactionType.PAYOUT, null);
        payoutService.sendPayout(wallet.getStripeAccountId(), amount, "aud", debited.getId());
        return debited;
    }

    @Transactional
    public String startStripeOnboarding(Long userId) {
        Wallet wallet = getOrCreateWalletByUserId(userId);
        return stripeOnboardingService.startOnboarding(wallet);
    }

    private Wallet getOrCreateWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(new Wallet(userId)));
    }

    private void validatePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
    }
}
