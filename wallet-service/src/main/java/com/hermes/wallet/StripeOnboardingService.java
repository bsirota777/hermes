package com.hermes.wallet;

import com.hermes.wallet.exception.OnboardingFailedException;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import org.springframework.stereotype.Service;

@Service
public class StripeOnboardingService {

    private final StripeClient stripeClient;
    private final WalletRepository walletRepository;

    public StripeOnboardingService(StripeClient stripeClient, WalletRepository walletRepository) {
        this.stripeClient = stripeClient;
        this.walletRepository = walletRepository;
    }

    // Called with a wallet that already exists (WalletService creates it first if needed).
    public String startOnboarding(Wallet wallet) {
        String accountId = wallet.getStripeAccountId();
        try {
            if (accountId == null) {
                AccountCreateParams createParams = AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .setCountry("AU")
                        .setCapabilities(
                                AccountCreateParams.Capabilities.builder()
                                        .setTransfers(AccountCreateParams.Capabilities.Transfers.builder()
                                                .setRequested(true).build())
                                        .build())
                        .build();
                Account account = stripeClient.v1().accounts().create(createParams);
                accountId = account.getId();
                wallet.setStripeAccountId(accountId);
                walletRepository.save(wallet);
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
            throw new OnboardingFailedException(wallet.getUserId(), e);
        }
    }
}
