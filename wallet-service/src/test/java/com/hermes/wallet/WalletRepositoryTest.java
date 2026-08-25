package com.hermes.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
class WalletRepositoryTest {

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void findByUserId_returnsWallet_whenWalletExists() {
        Wallet wallet = new Wallet(100L);
        wallet.setBalance(new BigDecimal("50.00"));
        walletRepository.save(wallet);

        Optional<Wallet> result = walletRepository.findByUserId(100L);

        assertThat(result).isPresent();
        assertThat(result.get().getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void findByUserId_returnsEmpty_whenNoWalletExists() {
        Optional<Wallet> result = walletRepository.findByUserId(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_persistsDefaultZeroBalance_whenNotExplicitlySet() {
        Wallet wallet = new Wallet(101L);

        Wallet saved = walletRepository.save(wallet);

        assertThat(saved.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void save_setsCreatedAtAndUpdatedAt() {
        Wallet wallet = new Wallet(102L);

        Wallet saved = walletRepository.save(wallet);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void save_persistsStripeFields() {
        Wallet wallet = new Wallet(103L);
        wallet.setStripeAccountId("acct_test123");
        wallet.setStripePayoutsEnabled(true);

        Wallet saved = walletRepository.save(wallet);
        Wallet reloaded = walletRepository.findByUserId(103L).orElseThrow();

        assertThat(reloaded.getStripeAccountId()).isEqualTo("acct_test123");
        assertThat(reloaded.isStripePayoutsEnabled()).isTrue();
    }
}
