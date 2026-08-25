package com.hermes.wallet;

import com.hermes.common.wallet.WalletTransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
class WalletTransactionRepositoryTest {

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    private Wallet persistWallet(Long userId) {
        return walletRepository.save(new Wallet(userId));
    }

    @Test
    void save_persistsTransactionWithAllFields() {
        Wallet wallet = persistWallet(200L);

        WalletTransaction txn = new WalletTransaction(
                wallet, new BigDecimal("25.00"), WalletTransactionType.EARNING, null);

        WalletTransaction saved = walletTransactionRepository.save(txn);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getWallet().getId()).isEqualTo(wallet.getId());
        assertThat(saved.getAmount()).isEqualByComparingTo("25.00");
        assertThat(saved.getType()).isEqualTo(WalletTransactionType.EARNING);
        assertThat(saved.getRelatedDeliveryId()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void save_persistsTransactionWithRelatedDeliveryId() {
        Wallet driverWallet = persistWallet(201L);

        WalletTransaction txn = new WalletTransaction(
                driverWallet, new BigDecimal("20.00"), WalletTransactionType.EARNING, 999L);

        WalletTransaction saved = walletTransactionRepository.save(txn);

        assertThat(saved.getRelatedDeliveryId()).isEqualTo(999L);
    }

    @Test
    void findByWalletIdOrderByCreatedAtDesc_returnsTransactionsNewestFirst() throws InterruptedException {
        Wallet wallet = persistWallet(202L);

        WalletTransaction first = walletTransactionRepository.save(
                new WalletTransaction(wallet, new BigDecimal("10.00"), WalletTransactionType.EARNING, null));
        Thread.sleep(10); // ensure distinct createdAt timestamps
        WalletTransaction second = walletTransactionRepository.save(
                new WalletTransaction(wallet, new BigDecimal("-5.00"), WalletTransactionType.PAYOUT, null));

        List<WalletTransaction> result = walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(second.getId());
        assertThat(result.get(1).getId()).isEqualTo(first.getId());
    }

    @Test
    void findByWalletIdOrderByCreatedAtDesc_returnsEmptyList_whenNoTransactions() {
        Wallet wallet = persistWallet(203L);

        List<WalletTransaction> result = walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findByWalletIdOrderByCreatedAtDesc_onlyReturnsTransactionsForGivenWallet() {
        Wallet walletA = persistWallet(204L);
        Wallet walletB = persistWallet(205L);

        walletTransactionRepository.save(
                new WalletTransaction(walletA, new BigDecimal("10.00"), WalletTransactionType.EARNING, null));
        walletTransactionRepository.save(
                new WalletTransaction(walletB, new BigDecimal("15.00"), WalletTransactionType.EARNING, null));

        List<WalletTransaction> result = walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(walletA.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWallet().getId()).isEqualTo(walletA.getId());
    }
}
