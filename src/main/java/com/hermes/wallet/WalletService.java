package com.hermes.wallet;

import com.hermes.delivery.Delivery;
import com.hermes.wallet.exception.InsufficientFundsException;
import com.hermes.wallet.exception.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        return getWalletByUserId(userId).getBalance();
    }

    @Transactional
    public Wallet credit(Long userId, BigDecimal amount, WalletTransactionType type, Delivery relatedDelivery) {
        validatePositive(amount);
        Wallet wallet = getWalletByUserId(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
        walletTransactionRepository.save(new WalletTransaction(wallet, amount, type, relatedDelivery));
        return wallet;
    }

    @Transactional
    public Wallet debit(Long userId, BigDecimal amount, WalletTransactionType type, Delivery relatedDelivery) {
        validatePositive(amount);
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(wallet.getId(), amount, wallet.getBalance());
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        walletTransactionRepository.save(new WalletTransaction(wallet, amount.negate(), type, relatedDelivery));
        return wallet;
    }

    @Transactional
    public Wallet cashOut(Long userId, BigDecimal amount) {
        return debit(userId, amount, WalletTransactionType.CASHOUT, null);
    }

    private Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));
    }

    private void validatePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
    }
}