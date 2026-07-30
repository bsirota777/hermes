package com.hermes.wallet;

import com.hermes.TestcontainersConfig;
import com.hermes.delivery.Delivery;
import com.hermes.delivery.DeliveryRepository;
import com.hermes.delivery.DeliveryStatus;
import com.hermes.user.*;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SenderProfileRepository senderProfileRepository;

    @Autowired
    private RecipientProfileRepository recipientProfileRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    private User persistUser(String email) {
        User user = new User("Test User", email, "encoded-password");
        return userRepository.save(user);
    }

    private Wallet persistWallet(User user) {
        Wallet wallet = new Wallet(user);
        return walletRepository.save(wallet);
    }

    private Delivery persistDelivery(User senderUser, User recipientUser) {
        SenderProfile sender = new SenderProfile();
        sender.setAddress("123 ABC street");
        sender.setPhoneNumber("0412788899");
        sender.setUser(senderUser);
        SenderProfile savedSender = senderProfileRepository.save(sender);

        RecipientProfile recipient = new RecipientProfile();
        recipient.setAddress("345 XYZ street");
        recipient.setPhoneNumber("041234567");
        recipient.setUser(recipientUser);
        RecipientProfile savedRecipient = recipientProfileRepository.save(recipient);

        Delivery delivery = new Delivery();
        delivery.setSender(savedSender);
        delivery.setRecipient(savedRecipient);
        delivery.setPickUpAddress("123 Main St");
        delivery.setDropOffAddress("456 Oak Ave");
        delivery.setDeliveryFee(new BigDecimal("25.00"));
        delivery.setDriverCommissionRate(new BigDecimal("0.80"));
        delivery.setStatus(DeliveryStatus.CREATED);

        return deliveryRepository.save(delivery);
    }

    @Test
    void save_persistsTransactionWithAllFields() {
        User user = persistUser("wallet-txn1@example.com");
        Wallet wallet = persistWallet(user);

        WalletTransaction txn = new WalletTransaction(
                wallet, new BigDecimal("25.00"), WalletTransactionType.EARNING, null);

        WalletTransaction saved = walletTransactionRepository.save(txn);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getWallet().getId()).isEqualTo(wallet.getId());
        assertThat(saved.getAmount()).isEqualByComparingTo("25.00");
        assertThat(saved.getType()).isEqualTo(WalletTransactionType.EARNING);
        assertThat(saved.getRelatedDelivery()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void save_persistsTransactionWithRelatedDelivery() {
        User senderUser = persistUser("sender-txn@example.com");
        User recipientUser = persistUser("recipient-txn@example.com");
        User driverUser = persistUser("driver-txn@example.com");
        Wallet driverWallet = persistWallet(driverUser);
        Delivery delivery = persistDelivery(senderUser, recipientUser);

        WalletTransaction txn = new WalletTransaction(
                driverWallet, new BigDecimal("20.00"), WalletTransactionType.EARNING, delivery);

        WalletTransaction saved = walletTransactionRepository.save(txn);

        assertThat(saved.getRelatedDelivery()).isNotNull();
        assertThat(saved.getRelatedDelivery().getId()).isEqualTo(delivery.getId());
    }

    @Test
    void findByWalletIdOrderByCreatedAtDesc_returnsTransactionsNewestFirst() throws InterruptedException {
        User user = persistUser("wallet-txn2@example.com");
        Wallet wallet = persistWallet(user);

        WalletTransaction first = walletTransactionRepository.save(
                new WalletTransaction(wallet, new BigDecimal("10.00"), WalletTransactionType.EARNING, null));
        Thread.sleep(10); // ensure distinct createdAt timestamps
        WalletTransaction second = walletTransactionRepository.save(
                new WalletTransaction(wallet, new BigDecimal("-5.00"), WalletTransactionType.PAYMENT, null));

        List<WalletTransaction> result = walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(second.getId());
        assertThat(result.get(1).getId()).isEqualTo(first.getId());
    }

    @Test
    void findByWalletIdOrderByCreatedAtDesc_returnsEmptyList_whenNoTransactions() {
        User user = persistUser("wallet-txn3@example.com");
        Wallet wallet = persistWallet(user);

        List<WalletTransaction> result = walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findByWalletIdOrderByCreatedAtDesc_onlyReturnsTransactionsForGivenWallet() {
        User userA = persistUser("wallet-txn4a@example.com");
        User userB = persistUser("wallet-txn4b@example.com");
        Wallet walletA = persistWallet(userA);
        Wallet walletB = persistWallet(userB);

        walletTransactionRepository.save(
                new WalletTransaction(walletA, new BigDecimal("10.00"), WalletTransactionType.EARNING, null));
        walletTransactionRepository.save(
                new WalletTransaction(walletB, new BigDecimal("15.00"), WalletTransactionType.EARNING, null));

        List<WalletTransaction> result = walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(walletA.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWallet().getId()).isEqualTo(walletA.getId());
    }
}