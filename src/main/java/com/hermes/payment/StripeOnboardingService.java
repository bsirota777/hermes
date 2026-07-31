package com.hermes.payment;

import com.hermes.payment.exception.OnboardingFailedException;
import com.hermes.user.User;
import com.hermes.user.UserRepository;
import com.hermes.user.exception.UserNotFoundException;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeOnboardingService {

    private final StripeClient stripeClient;
    private final UserRepository userRepository;

    public StripeOnboardingService(StripeClient stripeClient,
                                   UserRepository userRepository) {
        this.stripeClient = stripeClient;
        this.userRepository = userRepository;
    }

    public String startOnboarding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String accountId = user.getStripeAccountId();
        try {
            if (accountId == null) {
                AccountCreateParams createParams = AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .setCountry("AU")
                        .setEmail(user.getEmail())
                        .setCapabilities(
                                AccountCreateParams.Capabilities.builder()
                                        .setTransfers(AccountCreateParams.Capabilities.Transfers.builder()
                                                .setRequested(true).build())
                                        .build())
                        .build();
                Account account = stripeClient.v1().accounts().create(createParams);
                accountId = account.getId();
                user.setStripeAccountId(accountId);
                userRepository.save(user);
            }

            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setRefreshUrl("https://yourapp.com/stripe/refresh")
                    .setReturnUrl("https://yourapp.com/stripe/return")
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();
            AccountLink link = stripeClient.v1().accountLinks().create(linkParams);
            return link.getUrl(); // frontend redirects the user here
        } catch (StripeException e) {
            throw new OnboardingFailedException(userId, e);
        }
    }
}