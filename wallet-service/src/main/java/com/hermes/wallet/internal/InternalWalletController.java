package com.hermes.wallet.internal;

import com.hermes.common.wallet.CreditWalletRequest;
import com.hermes.wallet.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/wallet")
public class InternalWalletController {

    private final WalletService walletService;

    public InternalWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/credit")
    public ResponseEntity<Void> credit(@RequestBody CreditWalletRequest request) {
        walletService.credit(request.userId(), request.amount(), request.type(), request.deliveryId());
        return ResponseEntity.ok().build();
    }
}
