package com.hermes.payment;

import com.hermes.payment.exception.OnboardingFailedException;
import com.hermes.user.User;
import com.hermes.user.UserRepository;
import com.hermes.user.exception.UserNotFoundException;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeOnboardingServiceTest {

    @Mock
    private StripeClient stripeClient;
    @Mock
    private V1Services v1Services;
    @Mock
    private AccountService accountService;
    @Mock
    private AccountLinkService accountLinkService;
    @Mock
    private UserRepository userRepository;

    private StripeOnboardingService onboardingService;

    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        onboardingService = new StripeOnboardingService(stripeClient, userRepository);
    }

    @Test
    void startOnboarding_createsAccountAndReturnsLink_whenUserHasNoStripeAccountYet() throws StripeException {
        when(stripeClient.v1()).thenReturn(v1Services);

        User user = new User("Test User", "test@example.com", "password");
        user.setId(userId);

        Account mockAccount = mock(Account.class);
        when(mockAccount.getId()).thenReturn("acct_new123");

        AccountLink mockLink = mock(AccountLink.class);
        when(mockLink.getUrl()).thenReturn("https://connect.stripe.com/setup/e/acct_new123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(v1Services.accounts()).thenReturn(accountService);
        when(accountService.create(any(AccountCreateParams.class))).thenReturn(mockAccount);
        when(v1Services.accountLinks()).thenReturn(accountLinkService);
        when(accountLinkService.create(any(AccountLinkCreateParams.class))).thenReturn(mockLink);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = onboardingService.startOnboarding(userId);

        assertThat(result).isEqualTo("https://connect.stripe.com/setup/e/acct_new123");
        assertThat(user.getStripeAccountId()).isEqualTo("acct_new123");
        verify(userRepository).save(user);
        verify(accountService).create(any(AccountCreateParams.class));
    }

    @Test
    void startOnboarding_skipsAccountCreation_whenUserAlreadyHasStripeAccount() throws StripeException {
        when(stripeClient.v1()).thenReturn(v1Services);

        User user = new User("Test User", "test@example.com", "password");
        user.setId(userId);
        user.setStripeAccountId("acct_existing123");

        AccountLink mockLink = mock(AccountLink.class);
        when(mockLink.getUrl()).thenReturn("https://connect.stripe.com/setup/e/acct_existing123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(v1Services.accountLinks()).thenReturn(accountLinkService);
        when(accountLinkService.create(any(AccountLinkCreateParams.class))).thenReturn(mockLink);

        String result = onboardingService.startOnboarding(userId);

        assertThat(result).isEqualTo("https://connect.stripe.com/setup/e/acct_existing123");
        verify(v1Services, never()).accounts();
        verify(userRepository, never()).save(any());
    }

    @Test
    void startOnboarding_throwsUserNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.startOnboarding(userId))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(v1Services);
    }

    @Test
    void startOnboarding_throwsOnboardingFailedException_whenStripeAccountCreationFails() throws StripeException {
        when(stripeClient.v1()).thenReturn(v1Services);

        User user = new User("Test User", "test@example.com", "password");
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(v1Services.accounts()).thenReturn(accountService);
        when(accountService.create(any(AccountCreateParams.class))).thenThrow(mock(CardException.class));

        assertThatThrownBy(() -> onboardingService.startOnboarding(userId))
                .isInstanceOf(OnboardingFailedException.class)
                .hasCauseInstanceOf(StripeException.class);
    }

    @Test
    void startOnboarding_throwsOnboardingFailedException_whenAccountLinkCreationFails() throws StripeException {
        when(stripeClient.v1()).thenReturn(v1Services);

        User user = new User("Test User", "test@example.com", "password");
        user.setId(userId);
        user.setStripeAccountId("acct_existing123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(v1Services.accountLinks()).thenReturn(accountLinkService);
        when(accountLinkService.create(any(AccountLinkCreateParams.class))).thenThrow(mock(CardException.class));

        assertThatThrownBy(() -> onboardingService.startOnboarding(userId))
                .isInstanceOf(OnboardingFailedException.class);
    }
}