package com.hermes.wallet;

import com.hermes.wallet.exception.OnboardingFailedException;
import com.stripe.StripeClient;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.service.AccountLinkService;
import com.stripe.service.AccountService;
import com.stripe.service.V1Services;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Rewritten: the old version took a userId and looked the User up itself (UserRepository,
// throwing UserNotFoundException). Now WalletService already resolves/creates the Wallet
// before calling in, so startOnboarding takes the Wallet directly - there's no more
// "user doesn't exist" case here at all, that's simply not a concern this class has anymore.
@ExtendWith(MockitoExtension.class)
class StripeOnboardingServiceTest {

    @Mock private StripeClient stripeClient;
    @Mock private V1Services v1Services;
    @Mock private AccountService accountService;
    @Mock private AccountLinkService accountLinkService;
    @Mock private WalletRepository walletRepository;

    private StripeOnboardingService onboardingService;

    @BeforeEach
    void setUp() {
        onboardingService = new StripeOnboardingService(stripeClient, walletRepository);
    }

    @Test
    void startOnboarding_createsAccountAndReturnsLink_whenWalletHasNoStripeAccountYet() throws StripeException {
        when(stripeClient.v1()).thenReturn(v1Services);

        Wallet wallet = new Wallet(1L);
        wallet.setId(10L);

        Account mockAccount = mock(Account.class);
        when(mockAccount.getId()).thenReturn("acct_new123");

        AccountLink mockLink = mock(AccountLink.class);
        when(mockLink.getUrl()).thenReturn("https://connect.stripe.com/setup/e/acct_new123");

        when(v1Services.accounts()).thenReturn(accountService);
        when(accountService.create(any(AccountCreateParams.class))).thenReturn(mockAccount);
        when(v1Services.accountLinks()).thenReturn(accountLinkService);
        when(accountLinkService.create(any(AccountLinkCreateParams.class))).thenReturn(mockLink);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = onboardingService.startOnboarding(wallet);

        assertThat(result).isEqualTo("https://connect.stripe.com/setup/e/acct_new123");
        assertThat(wallet.getStripeAccountId()).isEqualTo("acct_new123");
        verify(walletRepository).save(wallet);
        verify(accountService).create(any(AccountCreateParams.class));
    }

    @Test
    void startOnboarding_skipsAccountCreation_whenWalletAlreadyHasStripeAccount() throws StripeException {
        when(stripeClient.v1()).thenReturn(v1Services);

        Wallet wallet = new Wallet(1L);
        wallet.setId(10L);
        wallet.setStripeAccountId("acct_existing123");

        AccountLink mockLink = mock(AccountLink.class);
        when(mockLink.getUrl()).thenReturn("https://connect.stripe.com/setup/e/acct_existing123");

        when(v1Services.accountLinks()).thenReturn(accountLinkService);
        when(accountLinkService.create(any(AccountLinkCreateParams.class))).thenReturn(mockLink);

        String result = onboardingService.startOnboarding(wallet);

        assertThat(result).isEqualTo("https://connect.stripe.com/setup/e/acct_existing123");
        verify(v1Services, never()).accounts();
        verify(walletRepository, never()).save(any());
    }

    @Test
    void startOnboarding_throwsOnboardingFailedException_whenStripeAccountCreationFails() throws StripeException {
        when(stripeClient.v1()).thenReturn(v1Services);

        Wallet wallet = new Wallet(1L);
        wallet.setId(10L);

        when(v1Services.accounts()).thenReturn(accountService);
        when(accountService.create(any(AccountCreateParams.class))).thenThrow(mock(CardException.class));

        assertThatThrownBy(() -> onboardingService.startOnboarding(wallet))
                .isInstanceOf(OnboardingFailedException.class)
                .hasCauseInstanceOf(StripeException.class);

        verify(walletRepository, never()).save(any());
    }

    @Test
    void startOnboarding_throwsOnboardingFailedException_whenAccountLinkCreationFails() throws StripeException {
        when(stripeClient.v1()).thenReturn(v1Services);

        Wallet wallet = new Wallet(1L);
        wallet.setId(10L);
        wallet.setStripeAccountId("acct_existing123");

        when(v1Services.accountLinks()).thenReturn(accountLinkService);
        when(accountLinkService.create(any(AccountLinkCreateParams.class))).thenThrow(mock(CardException.class));

        assertThatThrownBy(() -> onboardingService.startOnboarding(wallet))
                .isInstanceOf(OnboardingFailedException.class);
    }
}
