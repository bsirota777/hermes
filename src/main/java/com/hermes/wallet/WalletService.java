package com.hermes.wallet;

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

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        return getWalletByUserId(userId).getBalance();
    }

    @Transactional
    public Wallet credit(Long userId, BigDecimal amount) {
        validatePositive(amount);
        Wallet wallet = getWalletByUserId(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        // TODO: once WalletTransaction exists, insert a ledger row here (type=EARNING)
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet debit(Long userId, BigDecimal amount) {
        validatePositive(amount);
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(wallet.getId(), amount, wallet.getBalance());
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        // TODO: once WalletTransaction exists, insert a ledger row here (type=PAYMENT)
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet cashOut(Long userId, BigDecimal amount) {
        // Debits the wallet now; actual money movement to the user's bank/card
        // is a separate concern (payment provider payout call) — wire that in
        // once a provider is chosen, before or after this debit depending on
        // whether you want to reserve funds first.
        Wallet wallet = debit(userId, amount);
        // TODO: once WalletTransaction exists, insert a ledger row here (type=CASHOUT)
        return wallet;
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