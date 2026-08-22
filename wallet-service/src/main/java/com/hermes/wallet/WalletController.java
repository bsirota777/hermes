package com.hermes.wallet;

import com.hermes.wallet.dto.CashOutRequestDto;
import com.hermes.wallet.dto.OnboardingResponseDto;
import com.hermes.wallet.dto.WalletBalanceResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/{userId}/balance")
    public ResponseEntity<WalletBalanceResponseDto> getBalance(@PathVariable Long userId) {
        var balance = walletService.getBalance(userId);
        return ResponseEntity.ok(new WalletBalanceResponseDto(userId, balance));
    }

    @PostMapping("/{userId}/cashout")
    public ResponseEntity<WalletBalanceResponseDto> cashOut(
            @PathVariable Long userId,
            @Valid @RequestBody CashOutRequestDto request) {
        Wallet wallet = walletService.cashOut(userId, request.amount());
        return ResponseEntity.ok(new WalletBalanceResponseDto(userId, wallet.getBalance()));
    }

    @PostMapping("/{userId}/onboarding")
    public ResponseEntity<OnboardingResponseDto> startOnboarding(@PathVariable Long userId) {
        String url = walletService.startStripeOnboarding(userId);
        return ResponseEntity.ok(new OnboardingResponseDto(url));
    }
}
