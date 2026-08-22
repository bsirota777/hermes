package com.hermes.wallet.dto;

import java.math.BigDecimal;

public record WalletBalanceResponseDto(Long userId, BigDecimal balance) {}