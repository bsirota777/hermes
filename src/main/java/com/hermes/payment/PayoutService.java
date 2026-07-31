package com.hermes.payment;

import com.hermes.payment.exception.PayoutFailedException;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.param.TransferCreateParams;
import org.springframework.beans.factory.annotation.Value;
import com.stripe.model.Transfer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PayoutService {

    private final StripeClient stripeClient;

    /*public PayoutService(@Value("${stripe.secret-key}") String secretKey) {
        this.stripeClient = new StripeClient(secretKey);
    }*/

    public PayoutService(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }

    public Transfer sendPayout(String stripeAccountId, BigDecimal amount, String currency, Long walletTransactionId) {
        TransferCreateParams params = TransferCreateParams.builder()
                .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValueExact()) // cents
                .setCurrency(currency)
                .setDestination(stripeAccountId)
                .setDescription("Hermes wallet cash-out")
                .putMetadata("walletTransactionId", String.valueOf(walletTransactionId))
                .build();
        try {
            return stripeClient.v1().transfers().create(params);
        } catch (StripeException e) {
            throw new PayoutFailedException(stripeAccountId, amount, e);
        }
    }
}
