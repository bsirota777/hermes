package com.hermes.wallet;

import com.hermes.TestcontainersConfig;
import com.hermes.user.User;
import com.hermes.user.UserRepository;
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

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String email) {
        User user = new User();
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword("encoded-password");
        return userRepository.save(user);
    }

    @Test
    void findByUserId_returnsWallet_whenWalletExists() {
        User user = persistUser("wallet-user1@example.com");
        Wallet wallet = new Wallet(user);
        wallet.setBalance(new BigDecimal("50.00"));
        walletRepository.save(wallet);

        Optional<Wallet> result = walletRepository.findByUserId(user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void findByUserId_returnsEmpty_whenNoWalletExists() {
        User user = persistUser("wallet-user2@example.com");

        Optional<Wallet> result = walletRepository.findByUserId(user.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void save_persistsDefaultZeroBalance_whenNotExplicitlySet() {
        User user = persistUser("wallet-user3@example.com");
        Wallet wallet = new Wallet(user);

        Wallet saved = walletRepository.save(wallet);

        assertThat(saved.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void save_setsCreatedAtAndUpdatedAt() {
        User user = persistUser("wallet-user4@example.com");
        Wallet wallet = new Wallet(user);

        Wallet saved = walletRepository.save(wallet);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}