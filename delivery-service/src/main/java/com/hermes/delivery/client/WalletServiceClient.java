package com.hermes.delivery.client;

import com.hermes.common.wallet.CreditWalletRequest;

public interface WalletServiceClient {
    void credit(CreditWalletRequest request);
}