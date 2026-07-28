package com.hermes.wallet;

import com.hermes.user.User;
import com.hermes.wallet.exception.InsufficientFundsException;
import com.hermes.wallet.exception.WalletNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    private Wallet wallet;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(userId);
        wallet = new Wallet(user);
        wallet.setId(10L);
        wallet.setBalance(new BigDecimal("100.00"));
    }

    @Test
    void getBalance_returnsBalance_whenWalletExists() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        BigDecimal balance = walletService.getBalance(userId);

        assertThat(balance).isEqualByComparingTo("100.00");
    }

    @Test
    void getBalance_throwsWalletNotFoundException_whenWalletDoesNotExist() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getBalance(userId))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void credit_increasesBalance() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.credit(userId, new BigDecimal("25.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("125.00");
        verify(walletRepository).save(wallet);
    }

    @Test
    void credit_throwsIllegalArgumentException_whenAmountIsZeroOrNegative() {
        assertThatThrownBy(() -> walletService.credit(userId, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> walletService.credit(userId, new BigDecimal("-5.00")))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(walletRepository);
    }

    @Test
    void debit_decreasesBalance_whenSufficientFunds() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.debit(userId, new BigDecimal("40.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("60.00");
    }

    @Test
    void debit_throwsInsufficientFundsException_whenBalanceTooLow() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.debit(userId, new BigDecimal("500.00")))
                .isInstanceOf(InsufficientFundsException.class);

        verify(walletRepository, never()).save(any());
    }

    @Test
    void debit_throwsIllegalArgumentException_whenAmountIsZeroOrNegative() {
        assertThatThrownBy(() -> walletService.debit(userId, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(walletRepository);
    }

    @Test
    void cashOut_debitsWallet_whenSufficientFunds() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.cashOut(userId, new BigDecimal("30.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    void cashOut_throwsInsufficientFundsException_whenBalanceTooLow() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.cashOut(userId, new BigDecimal("1000.00")))
                .isInstanceOf(InsufficientFundsException.class);
    }
}