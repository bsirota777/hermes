// common/src/main/java/com/hermes/common/wallet/CreditWalletRequest.java
package com.hermes.common.wallet;

import java.math.BigDecimal;

public record CreditWalletRequest(
        Long userId,
        BigDecimal amount,
        WalletTransactionType type,
        Long deliveryId
) {}