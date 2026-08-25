package com.hermes.wallet;

import com.hermes.common.wallet.WalletTransactionType;
import com.hermes.wallet.exception.InsufficientFundsException;
import com.hermes.wallet.exception.StripeAccountNotLinkedException;
import com.hermes.wallet.exception.StripeOnboardingIncompleteException;
import com.stripe.model.Transfer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// WalletService now does find-or-create on wallet lookup (see getOrCreateWalletByUserId) instead
// of the old eager-creation-at-registration + throw-if-missing model, so unlike the old
// WalletServiceTest, WalletNotFoundException is never expected here in normal flow.
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private PayoutService payoutService;
    @Mock private StripeOnboardingService stripeOnboardingService;

    private WalletService walletService;

    private Wallet wallet;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository, walletTransactionRepository, payoutService, stripeOnboardingService);

        wallet = new Wallet(userId);
        wallet.setId(10L);
        wallet.setBalance(new BigDecimal("100.00"));
    }

    // ---------- getBalance ----------

    @Test
    void getBalance_returnsBalance_whenWalletExists() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        BigDecimal balance = walletService.getBalance(userId);

        assertThat(balance).isEqualByComparingTo("100.00");
    }

    @Test
    void getBalance_createsWalletWithZeroBalance_whenNoneExists() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        BigDecimal balance = walletService.getBalance(userId);

        assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
        verify(walletRepository).save(argThat(w -> w.getUserId().equals(userId)));
    }

    // ---------- credit ----------

    @Test
    void credit_increasesBalanceAndRecordsTransaction() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.credit(userId, new BigDecimal("25.00"), WalletTransactionType.EARNING, 42L);

        assertThat(result.getBalance()).isEqualByComparingTo("125.00");
        verify(walletTransactionRepository).save(argThat(txn ->
                txn.getRelatedDeliveryId().equals(42L)
                        && txn.getType() == WalletTransactionType.EARNING
                        && txn.getAmount().compareTo(new BigDecimal("25.00")) == 0
        ));
    }

    @Test
    void credit_createsWallet_whenNoneExistsYet() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.credit(userId, new BigDecimal("25.00"), WalletTransactionType.REFUND, null);

        assertThat(result.getBalance()).isEqualByComparingTo("25.00");
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    void credit_throwsIllegalArgumentException_whenAmountIsZeroOrNegative() {
        assertThatThrownBy(() -> walletService.credit(userId, BigDecimal.ZERO, WalletTransactionType.EARNING, 42L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> walletService.credit(userId, new BigDecimal("-5.00"), WalletTransactionType.EARNING, 42L))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(walletRepository, walletTransactionRepository);
    }

    // ---------- debit ----------

    @Test
    void debit_decreasesBalance_whenSufficientFunds() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.debit(userId, new BigDecimal("40.00"), WalletTransactionType.PAYOUT, 42L);

        assertThat(result.getBalance()).isEqualByComparingTo("60.00");
        verify(walletTransactionRepository).save(argThat(txn ->
                txn.getAmount().compareTo(new BigDecimal("-40.00")) == 0
                        && txn.getType() == WalletTransactionType.PAYOUT
                        && txn.getRelatedDeliveryId().equals(42L)
        ));
    }

    @Test
    void debit_throwsInsufficientFundsException_whenBalanceTooLow() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.debit(userId, new BigDecimal("500.00"), WalletTransactionType.PAYOUT, 42L))
                .isInstanceOf(InsufficientFundsException.class);

        verify(walletRepository, never()).save(any());
        verify(walletTransactionRepository, never()).save(any());
    }

    @Test
    void debit_throwsIllegalArgumentException_whenAmountIsZeroOrNegative() {
        assertThatThrownBy(() -> walletService.debit(userId, BigDecimal.ZERO, WalletTransactionType.PAYOUT, 42L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> walletService.debit(userId, new BigDecimal("-5.00"), WalletTransactionType.PAYOUT, 42L))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(walletRepository, walletTransactionRepository);
    }

    // ---------- cashOut ----------

    @Test
    void cashOut_debitsWalletAndSendsPayout_whenStripeLinkedAndOnboarded() {
        wallet.setStripeAccountId("acct_test123");
        wallet.setStripePayoutsEnabled(true);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payoutService.sendPayout(eq("acct_test123"), any(BigDecimal.class), anyString(), any()))
                .thenReturn(mock(Transfer.class));

        Wallet result = walletService.cashOut(userId, new BigDecimal("40.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("60.00");
        verify(walletTransactionRepository).save(argThat(txn ->
                txn.getAmount().compareTo(new BigDecimal("-40.00")) == 0
                        && txn.getType() == WalletTransactionType.PAYOUT
                        && txn.getRelatedDeliveryId() == null
        ));
        verify(payoutService).sendPayout(eq("acct_test123"), eq(new BigDecimal("40.00")), anyString(), any());
    }

    @Test
    void cashOut_throwsStripeAccountNotLinkedException_whenNoStripeAccount() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.cashOut(userId, new BigDecimal("40.00")))
                .isInstanceOf(StripeAccountNotLinkedException.class);

        verifyNoInteractions(payoutService);
    }

    @Test
    void cashOut_throwsStripeOnboardingIncompleteException_whenPayoutsNotYetEnabled() {
        wallet.setStripeAccountId("acct_test123");
        wallet.setStripePayoutsEnabled(false);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.cashOut(userId, new BigDecimal("40.00")))
                .isInstanceOf(StripeOnboardingIncompleteException.class);

        verifyNoInteractions(payoutService);
    }

    @Test
    void cashOut_throwsInsufficientFundsException_whenBalanceTooLow() {
        wallet.setStripeAccountId("acct_test123");
        wallet.setStripePayoutsEnabled(true);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.cashOut(userId, new BigDecimal("1000.00")))
                .isInstanceOf(InsufficientFundsException.class);

        verifyNoInteractions(payoutService);
    }

    // ---------- startStripeOnboarding ----------

    @Test
    void startStripeOnboarding_delegatesToStripeOnboardingServiceWithTheWallet() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(stripeOnboardingService.startOnboarding(wallet)).thenReturn("https://connect.stripe.com/setup/e/acct_x");

        String result = walletService.startStripeOnboarding(userId);

        assertThat(result).isEqualTo("https://connect.stripe.com/setup/e/acct_x");
        verify(stripeOnboardingService).startOnboarding(wallet);
    }

    @Test
    void startStripeOnboarding_createsWalletFirst_whenNoneExistsYet() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stripeOnboardingService.startOnboarding(any(Wallet.class)))
                .thenReturn("https://connect.stripe.com/setup/e/acct_new");

        String result = walletService.startStripeOnboarding(userId);

        assertThat(result).isEqualTo("https://connect.stripe.com/setup/e/acct_new");
        verify(walletRepository).save(argThat(w -> w.getUserId().equals(userId)));
    }
}
